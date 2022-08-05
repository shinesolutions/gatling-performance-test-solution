package cdk.gatling.gatlingstack;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.Arrays;

class GatlingContainerOptions extends Construct {
    private final ContainerDefinitionOptions containerDefinitionOptions;

    public GatlingContainerOptions(Construct scope, String id, String clusterNamespace, String taskDefinitionName, String bucket) {
        super(scope, id);

        DockerImageAsset gatlingDockerImageAsset = DockerImageAsset.Builder.create(this, "GatlingDockerImageAsset")
                .directory("../gatling-tests")
                .build();

        this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromDockerImageAsset(gatlingDockerImageAsset))
                .command(Arrays.asList("-r", bucket))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "GatlingFargateLogGroup")
                                .logGroupName(String.format("/ecs/%s/%s", clusterNamespace, taskDefinitionName))
                                .retention(RetentionDays.TWO_WEEKS)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build())
                        .streamPrefix(taskDefinitionName)
                        .build()))
                .build();
    }

    public ContainerDefinitionOptions getContainerDefinitionOptions() {
        return this.containerDefinitionOptions;
    }
}
