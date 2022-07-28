package scenarios

import CommonFunctions.SimulationDetails
import config.ConfigDetails
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import requests.PostCodeRequests

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object PostCodeScenario {

  def PostCodeScn_RandomPostcodesOnly(simulationName: String, simulationType: String, environment: String): ScenarioBuilder = {

    def pcHost = ConfigDetails.getHostBaseUrl(environment, "PostCode")

    //Get scenario values
    val (pacingMin, pacingMax, userDistribution) = SimulationDetails.getScenarioValues(simulationName, simulationType)

    val scn: ScenarioBuilder = scenario("Postcode_RandomPostCodesOnly")
      .forever {
        pace(pacingMin milliseconds, pacingMax milliseconds)
          .randomSwitch(
            userDistribution("Get_Postcode_Random") ->
              exec(PostCodeRequests.GET_Postcode_Random(pcHost))
          )
      }

    scn
  }

}
