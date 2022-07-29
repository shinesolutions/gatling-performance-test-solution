package runner;

import org.slf4j.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;
import static org.slf4j.LoggerFactory.getLogger;

public class GatlingAwsTestRunner {

    private static final Logger LOG = getLogger(GatlingAwsTestRunner.class);
    private final Config config;
    private final EcsClient ecsClient;
    private final Ec2Client ec2Client;
    private int lgIterator = 0;
    List<KeyValuePair> environmentVariables = new ArrayList<>();

    public static void main(String[] args) {
        GatlingAwsTestRunner gatlingAwsTestRunner = new GatlingAwsTestRunner();

        gatlingAwsTestRunner.runLoadTest();
    }

    public GatlingAwsTestRunner() {
        this.config = new Config();
        this.ecsClient = EcsClient.builder().build();
        this.ec2Client = Ec2Client.builder().build();
    }

    private void runLoadTest() {

        if (anyTasksActive()) {
            throw new IllegalStateException("There are already tasks active on the cluster!");
        }

        LOG.info("Starting load test on AWS with {} containers", config.numOfLoadGenerators);
        //LOG.info("Feeder starting from {}", config.feederStart);

        StringBuilder summary = new StringBuilder("\n******************** SIMULATION VALUES ********************");
        summary.append("\nEnvironment: ").append(config.environment);
        summary.append("\nSimulation: ").append(config.simulation);
        summary.append("\nUsers: ").append(config.usersPerContainer * config.numOfLoadGenerators);

        if(config.targetRpm > 0)
            summary.append("\nTarget RPM: ").append(config.targetRpm * config.numOfLoadGenerators).append(" requests per minute");

        if(config.rampUpDuration > 0)
            summary.append("\nRamp up duration: ").append(config.rampUpDuration).append(" minute(s)");

        if(config.peakLoadDuration > 0)
            summary.append("\nPeak load duration: ").append(config.peakLoadDuration).append(" minute(s)");

        if(config.rampUpDuration > 0 && config.peakLoadDuration > 0)
            summary.append("\nTotal duration: ").append(config.rampUpDuration + config.peakLoadDuration).append(" minute(s)");

        summary.append("\n**********************************************************");

        LOG.info(String.valueOf(summary));

        int currentFeeder = config.feederStart;

        setEnvironmentVariables();
        for (lgIterator = 0; lgIterator < config.numOfLoadGenerators; lgIterator++) {
            final RunTaskRequest runTaskRequest = createRunTaskRequest(currentFeeder);

            LOG.info("Starting container {}/{}", lgIterator + 1, config.numOfLoadGenerators);

            ecsClient.runTask(runTaskRequest);

            currentFeeder += config.usersPerContainer;
        }

        if (config.waitForTestCompletion) {
            waitForTestCompletion();
        }

        // This is recommended to be used when using Jenkins for triggering tests.
        if (!config.waitForTestCompletion)
            LOG.info("The test has been started in ECS cluster of your AWS account. ");

    }

    private void setEnvironmentVariables() {
        environmentVariables.add(KeyValuePair.builder().name("REPORT_BUCKET").value(config.gatlingReportBucket).build());
        environmentVariables.add(KeyValuePair.builder().name("USERS").value(String.valueOf(config.usersPerContainer)).build());
        environmentVariables.add(KeyValuePair.builder().name("SIMULATION").value(config.simulation).build());

        if(config.simulationType != null)
            environmentVariables.add(KeyValuePair.builder().name("SIMULATION_TYPE").value(config.simulationType).build());

        environmentVariables.add(KeyValuePair.builder().name("ENVIRONMENT").value(config.environment).build());
        // optional, don't set if null
        if(config.peakLoadDuration > 0)
            environmentVariables.add(KeyValuePair.builder().name("PEAK_LOAD_DURATION").value(String.valueOf(config.peakLoadDuration)).build());

        if(config.rampUpDuration > 0)
            environmentVariables.add(KeyValuePair.builder().name("RAMP_UP_DURATION").value(String.valueOf(config.rampUpDuration)).build());

        if(config.targetRpm > 0)
            environmentVariables.add(KeyValuePair.builder().name("TARGET_RPM").value(String.valueOf(config.targetRpm)).build());

    }

    private RunTaskRequest createRunTaskRequest(int currentFeeder) {
        final NetworkConfiguration networkConfiguration = getNetworkConfiguration();

        environmentVariables.add(KeyValuePair.builder().name("FEEDER_START").value(String.valueOf(currentFeeder)).build());

        final TaskOverride taskOverride = TaskOverride.builder()
                .containerOverrides(ContainerOverride.builder()
                        .name("gatlingRunnerContainer")
                        .environment(environmentVariables)
                        .build())
                .build();

        return RunTaskRequest.builder()
                .launchType(LaunchType.FARGATE)
                .cluster(config.clusterName)
                .taskDefinition(config.taskDefinitionName)
                .startedBy("Gatling Load Test: " + config.simulation)
                .count(1)
                .networkConfiguration(networkConfiguration)
                .overrides(taskOverride)
                .build();
    }

    private void waitForTestCompletion() {
        if (config.waitForTestCompletion) {
            LOG.info("Waiting until all tasks on cluster are completed...");
            int sleepTime = getInitialSleepTime();

            while (true) {
                try {
                    LOG.info("Waiting for " + sleepTime/1000 + " seconds before checking cluster state");
                    Thread.sleep(sleepTime);
                    Cluster cluster = getClusterState();

                    if (cluster.runningTasksCount() == 0 && cluster.pendingTasksCount() == 0)
                        break;

                    LOG.info("Status: {} pending tasks and {} running tasks", cluster.pendingTasksCount(), cluster.runningTasksCount());
                    sleepTime = 60000;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private int getInitialSleepTime(){
        int sleepTime = 0;

        if (config.peakLoadDuration > 0)
            sleepTime = sleepTime + config.peakLoadDuration;

        if (config.rampUpDuration > 0)
            sleepTime = sleepTime + config.rampUpDuration;

        if (sleepTime == 0)
            sleepTime = 60000;

        return sleepTime;
    }

    private AwsVpcConfiguration getAwsVpcConfiguration() {
        return AwsVpcConfiguration.builder()
                .assignPublicIp(AssignPublicIp.ENABLED)
                .subnets(getSubnets(config.vpcId))
                .build();
    }

    private NetworkConfiguration getNetworkConfiguration() {
        return NetworkConfiguration.builder()
                .awsvpcConfiguration(getAwsVpcConfiguration())
                .build();
    }

    private List<String> getSubnets(String vpcId) {
        final DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder()
                .filters(Filter.builder().name("vpc-id").values(vpcId).build())
                .build();

        final DescribeSubnetsResponse describeSubnetsResponse = ec2Client.describeSubnets(describeSubnetsRequest);

        return describeSubnetsResponse.subnets().stream().map(Subnet::subnetId).collect(Collectors.toList());
    }

    private Cluster getClusterState() {
        DescribeClustersRequest request = DescribeClustersRequest.builder().clusters(config.clusterName).build();
        return ecsClient.describeClusters(request).clusters().get(0);
    }

    private boolean anyTasksActive() {
        Cluster cluster = getClusterState();

        return cluster.runningTasksCount() != 0 && cluster.pendingTasksCount() != 0;
    }

    static class Config {
        // Required params
        //TODO::::
        final String vpcId = Objects.requireNonNull(getenv("VPC_ID"), "VPC_ID is required."); //"vpc-b3ad53d6";
        final String clusterName = Objects.requireNonNull(getenv("CLUSTER_NAME"), "CLUSTER_NAME is required.");//"gatling-cluster";
        final String taskDefinitionName = Objects.requireNonNull(getenv("TASK_DEFINITION"), "TASK_DEFINITION_NAME is required.");//"gatling-tests";
        final String gatlingReportBucket = Objects.requireNonNull(System.getenv("REPORT_BUCKET"), "REPORT_BUCKET is required."); //"gatling-results-prashant";

        //Optional with defaults
        final int numOfLoadGenerators = parseInt(getEnvVarOrDefault("NUM_OF_LOAD_GENERATORS", "1"));

        // The below feederStart and usersPerContainer values would be used if there is a feeder being used by simulation, and we need to use different set of feeder records per container.
        // For example: Using different set of users per container.
        final int feederStart = parseInt(getEnvVarOrDefault("FEEDER_START", "0"));
        final int usersPerContainer = parseInt(getEnvVarOrDefault("USERS", "10"));

        final String simulation = Objects.requireNonNull(System.getenv("SIMULATION"), "SIMULATION is required."); //"simulations.PostCode.PostCodeSimulation";
        final String simulationType = System.getenv("SIMULATION_TYPE");
        final String environment = Objects.requireNonNull(System.getenv("ENVIRONMENT"), "ENVIRONMENT is required."); //"test";
        final int peakLoadDuration = parseInt(getenv("PEAK_LOAD_DURATION")); // minutes
        final int rampUpDuration = parseInt(getenv("RAMP_UP_DURATION")); // minutes
        final int targetRpm = parseInt(getenv("TARGET_RPM")); // request per minute
        final boolean waitForTestCompletion = true;

        String getEnvVarOrDefault(String var, String defaultValue) {
            if (getenv(var) == null) {
                return defaultValue;
            } else {
                return getenv(var);
            }
        }
    }
}
