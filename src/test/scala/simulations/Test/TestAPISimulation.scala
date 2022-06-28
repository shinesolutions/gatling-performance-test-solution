package simulations.Test

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.http

class TestAPISimulation extends Simulation {

  var scn: ScenarioBuilder =_
  val headers_1 = Map("Content-Type" -> """application/json""",
    "x-rapidapi-host" -> "v1.basketball.api-sports.io",
    "x-rapidapi-key" -> "XxXxXxXxXxXxXxXxXxXxXxXx"
  )
  val host = "https://v1.basketball.api-sports.io/status"

val test = "ABC"
  println(test)

  scn = scenario("scn")
    .exec({session =>
      println("Test scenario started")
      session.set("test", "2")})
    .exec(http("request_1")
      .get(host)
      .headers(headers_1))

  setUp(
    scn.inject(atOnceUsers(1))
  );

}

