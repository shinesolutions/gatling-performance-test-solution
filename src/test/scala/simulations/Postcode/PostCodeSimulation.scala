package simulations.Postcode


import CommonFunctions.SimulationDetails
import config.ConfigDetails
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.http
import requests.PostCode.PostCodeRequests
import scenarios.PostCode.scenarios.PostCodeScenario

import scala.concurrent.duration.DurationInt


class PostCodeSimulation extends Simulation {

  //Open the XML which contains scenario details
  val simulationParams = SimulationDetails.getSimulationConfigXML("PostCode")

  //Define test environment
  def testEnv = System.getProperty("testEnv", "test")

  val simulationName = "Postcode"
  //Define simulation type to be used for the test
  val simulationType = "Postcode_1"

  //Get simulation values
  val (noOfUsers, rampupTime, peakLoadDuration) = SimulationDetails.getSimulationValues(simulationName, simulationType, testEnv)
  var RandomPostcodeScenario: ScenarioBuilder =_

  RandomPostcodeScenario = PostCodeScenario.PostCodeScn_RandomPostcodesOnly(simulationName, simulationType, testEnv)

  setUp(
    RandomPostcodeScenario.inject(rampUsers(noOfUsers) during (rampupTime seconds))
  ).maxDuration(rampupTime + peakLoadDuration seconds);
}
