package simulations.PostCode

import CommonFunctions.SimulationDetails
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import scenarios.PostCodeScenario

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


class PostCodeSimulation extends Simulation {

  //Define test environment
  def environment: String = SimulationDetails.getEnvVarOrDefault("ENVIRONMENT", "test")

  val simulationName = "PostCode"

  //Define simulation type to be used for the test
  val simulationType = SimulationDetails.getEnvVarOrDefault("SIMULATION_TYPE", "PostCode_1")

  //Get simulation values
  val (noOfUsers, rampupTime, peakLoadDuration) = SimulationDetails.getSimulationValues(simulationName, simulationType, environment)
  var RandomPostcodeScenario: ScenarioBuilder =_

  RandomPostcodeScenario = PostCodeScenario.PostCodeScn_RandomPostcodesOnly(simulationName, simulationType, environment)

  setUp(
    RandomPostcodeScenario.inject(rampUsers(noOfUsers) during (rampupTime seconds))
  ).maxDuration(rampupTime + peakLoadDuration seconds)
}
