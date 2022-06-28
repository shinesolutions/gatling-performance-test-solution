package scenarios.PostCode.scenarios

import CommonFunctions.SimulationDetails
import config.ConfigDetails
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import requests.PostCode.PostCodeRequests

import scala.concurrent.duration.DurationInt
import scala.xml.Elem

object PostCodeScenario {



  //Open the XML which contains scenario details
  //val simulationParams = SimulationDetails.getSimulationConfigXML("PostCode")

  //Get simulation values
  //val (noOfUsers, rampupTime, pacingMin, pacingMax, peakLoadDuration, userDistribution) = SimulationDetails.getSimulationValues(simulationParams, "Postcode", "Postcode_1", testEnv)


  def PostCodeScn_RandomPostcodesOnly(simulationName: String, simulationType: String, environment: String) : ScenarioBuilder = {

    def pcHost = ConfigDetails.getHostBaseUrl(environment, "PostCode")

    //Get scenario values
    val (pacingMin, pacingMax, userDistribution) = SimulationDetails.getScenarioValues(simulationName, simulationType)

    val scn: ScenarioBuilder = scenario("scn")
      .exec({session =>
        println("Test scenario started")
        session.set("test", "2")})
      .forever {
        pace(pacingMin milliseconds, pacingMax milliseconds)
          .randomSwitch(
            userDistribution("Get_Postcode_Random") ->
              exec(PostCodeRequests.GET_Postcode_Random(pcHost))
          )
      }

    return scn
  }


}
