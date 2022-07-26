package config

import CommonFunctions.SimulationDetails.getClass

import java.io.File
import java.nio.file.Paths
import scala.xml.{Elem, XML}

object ConfigDetails {

  /**
   * Opens the XML file containing the environment configuration parameters such as hosts(API endpoints)
   *  @return An Elem object that contains the elements from the XML file
   */
  def getEnvironmentConfigXML(): Elem = {

    XML.load(getClass.getResourceAsStream("/config/EnvironmentConfig.XML"))
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

    scala.util.Properties.envOrElse(hostName.toUpperCase, valueFromXml)

  }

}
