package config

import io.gatling.core.Predef._
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.structure.ChainBuilder

import scala.xml.{Elem, XML}
//import io.gatling.jsonpath.GatlingElParser.Elem

import java.io.File


import scala.util.Random
import scala.util.matching.Regex
import scala.xml


object ConfigDetails {

  /**
   * Opens the XML file containing the environment configuration parameters such as hosts(API endpoints)
   *  @return An Elem object that contains the elements from the XML file
   */
  def getEnvironmentConfigXML(): Elem = {
    val fs = File.separator

    return XML.loadFile("src" + fs + "test" + fs + "scala" + fs + "config" + fs + "EnvironmentConfig.XML")
  }

  /**
   * Get the base URL for the host in specified environment
   * @param environmentName The name of the environment
   * @param hostName The name of the host
   * @return The base URL for the host
   */
  def getHostBaseUrl(environmentName: String, hostName: String): String = {
    val envConfig = ConfigDetails.getEnvironmentConfigXML()
    val valueFromXml = (envConfig \\ "environments" \ environmentName \ hostName).text

    if (valueFromXml.isEmpty) {
      throw new Exception("ERROR - Endpoint is not defined for host '" + hostName + "' in EnvironmentConfig.xml for environment '" + environmentName + "'")
    }

    return scala.util.Properties.envOrElse(hostName.toUpperCase, valueFromXml)

  }

}
