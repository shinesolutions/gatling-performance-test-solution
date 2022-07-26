package simulations.Basic

import config.Config._
import io.gatling.core.Predef._
import scenarios.BasicScenario.basicScenario

import scala.language.postfixOps

class BasicSimulation extends Simulation {
  println("current dir**** " +System.getProperty("user.dir"))
  println("Configuration:")
  println("BaseURL: " + baseUrl)
  println("Nr concurrent users: " + users)
  println("Max duration: " + maxDuration)
  println("RampUp time: " + rampUpTime)

  setUp(
    basicScenario.inject(rampUsers(users) during rampUpTime)).maxDuration(maxDuration).protocols(httpProtocol)
    .assertions(
      global.failedRequests.count.is(0)
    )
}
