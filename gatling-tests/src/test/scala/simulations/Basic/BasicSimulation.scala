package simulations.Basic

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.{http, status}
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.language.postfixOps

class BasicSimulation extends Simulation {

  // Define headers
  val headers: Map[String, String] = Map("Content-Type" -> """application/json""")

  // Http protocol
  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://api.postcodes.io")
    .headers(headers)

  // Scenario Definition
  val scn: ScenarioBuilder = scenario("PostcodeScenario")
    .exec(http("Get_Postcode_Random").get("/random/postcodes"))
    .pause(5)
    .exec(http("GET_Postcode").get("/postcodes/" + "OX495NU"))
    .pause(5)

  // Simulation
  setUp(
    scn.inject(atOnceUsers(5)))
    .protocols(httpProtocol)

}
