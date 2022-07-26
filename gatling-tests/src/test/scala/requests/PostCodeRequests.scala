package requests

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

object PostCodeRequests {

  val headers_1 = Map("Content-Type" -> """application/json""")

  def GET_Postcode_Random(host: String): ChainBuilder = {
    exec(http("GET_Postcode_Random")
      .get(host + "/random/postcodes")
      .headers(headers_1))
  }

}
