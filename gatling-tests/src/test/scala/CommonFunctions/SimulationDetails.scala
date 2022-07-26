package CommonFunctions

import java.io.{File, InputStream}
import java.nio.file.Paths
import scala.collection.mutable
import scala.xml.{Elem, XML}

object SimulationDetails {



  /**
   * Opens the XML file containing the details of the simulation
   *
   * @param simulationName Name of the running simulation
   * @param configFileName Optional - Name of the config file. If left blank it assumes the config file name is same as simulation name + "config"
   * @return An Elem object containing all the elements from the XML file
   *
   */
  def getSimulationConfigXML(simulationName: String, configFileName: String = ""): Elem = {

    var xmlFilePath = "/config/"

    if (configFileName.isEmpty)
      xmlFilePath = xmlFilePath + simulationName + "Config.XML"
    else
      xmlFilePath = xmlFilePath + configFileName

    //TODO::: https://www.baeldung.com/java-classpath-resource-cannot-be-opened

    XML.load(getClass.getResourceAsStream(xmlFilePath))

  }

  /**
   * Get the simulation values from the config xml
   * @param simulationName Name of the running simulation
   * @param simulationType Type of the simulation
   * @param environment Environment where test needs to run
   * @return noOfUsers, rampUpDuration, peakLoadDuration
   */
  def getSimulationValues(simulationName: String, simulationType: String, environment: String) : (Int, Int, Int) = {

    //Open the XML which contains simulation details
    val simulationParams = SimulationDetails.getSimulationConfigXML(simulationName)

    var noOfUsers = 0
    var rampUpDuration = 0
    var peakLoadDuration = 0

    val summary = new mutable.StringBuilder("\n******************** SIMULATION VALUES ********************")

    summary.append("\nSimulation: " + simulationName)
    summary.append("\nEnvironment: " + environment)
    summary.append("\nSimulationType: " + simulationType)
    summary.append("\n**********************************************************")

    if ((simulationParams \\ "simulations" \ "simulation" \ simulationType).isEmpty)
      throw new Exception("ERROR - Invalid Simulation Name provided - " + simulationType + "This simulation name does not exist in Simulation")

    // Get the number of users
    if (System.getProperty("USERS") == null || (System.getProperty("USERS") == ""))
      noOfUsers = (simulationParams \\ "simulations" \ "simulation" \ simulationType \ "NumberOfUsers").text.toInt
    else
      noOfUsers = System.getProperty("USERS").toInt

    validateSimulationParameter("USERS", noOfUsers)
    summary.append("\nThreads: " + noOfUsers)

    // Get the Ramp-up Time
    if (System.getProperty("RAMP_UP_DURATION") == null || (System.getProperty("RAMP_UP_DURATION") == ""))
      rampUpDuration = (simulationParams \\ "simulations" \ "simulation" \ simulationType \ "RampUpDuration").text.toInt
    else
      rampUpDuration = System.getProperty("RAMP_UP_DURATION").toInt

    validateSimulationParameter("RAMP_UP_DURATION", rampUpDuration)

    // Get the PeakLoadDuration
    if (System.getProperty("PEAK_LOAD_DURATION") == null || (System.getProperty("PEAK_LOAD_DURATION") == ""))
      peakLoadDuration = (simulationParams \\ "simulations" \ "simulation" \ simulationType \ "PeakLoadDuration").text.toInt
    else
      peakLoadDuration = System.getProperty("PEAK_LOAD_DURATION").toInt

    validateSimulationParameter("PEAK_LOAD_DURATION", peakLoadDuration)

    summary.append("\nRamp Up Duration: " + rampUpDuration + " minute(s)")
    rampUpDuration = rampUpDuration * 60
    summary.append(" (" + rampUpDuration + " seconds)")

    summary.append("\nPeak Load Duration: " + peakLoadDuration + " minute(s)")
    peakLoadDuration = peakLoadDuration * 60
    summary.append(" (" + peakLoadDuration + " seconds)")


    summary.append("\nTotal Duration: " + (rampUpDuration + peakLoadDuration)/60 + " minute(s) (" + (rampUpDuration + peakLoadDuration) + " seconds)")

    print(summary)

    (noOfUsers, rampUpDuration, peakLoadDuration)

  }

  /**
   * Get the scenario values from the config xml
   * @param simulationName Name of the running simulation
   * @param simulationType Type of the simulation
   * @return noOfUsers, rampUpDuration, peakLoadDuration
   */
  def getScenarioValues(simulationName: String, simulationType: String) : (Int, Int, Map[String, Double]) = {

    //Open the XML which contains simulation details
    val simulationParams = SimulationDetails.getSimulationConfigXML(simulationName)

    var noOfUsers = 0
    var rampUpDuration = 0
    var targetRPM = 0

    val summary = new mutable.StringBuilder("\n***********************************************************")

    // Get the number of users
    if (System.getProperty("USERS") == null || (System.getProperty("USERS") == ""))
      noOfUsers = (simulationParams \\ "simulations" \ "simulation" \ simulationType \ "NumberOfUsers").text.toInt
    else
      noOfUsers = System.getProperty("USERS").toInt

    validateSimulationParameter("USERS", noOfUsers)

    // Get the Ramp-up Time
    if (System.getProperty("RAMP_UP_DURATION") == null || (System.getProperty("RAMP_UP_DURATION") == ""))
      rampUpDuration = (simulationParams \\ "simulations" \ "simulation" \ simulationType \ "RampUpDuration").text.toInt
    else
      rampUpDuration = System.getProperty("RAMP_UP_DURATION").toInt

    validateSimulationParameter("RAMP_UP_DURATION", rampUpDuration)

    // Get the Target RPS
    if (System.getProperty("TARGET_RPM") == null || (System.getProperty("TARGET_RPM") == ""))
      targetRPM = (simulationParams \\ "simulations" \ "simulation" \ simulationType \ "TargetRPM").text.toInt
    else
      targetRPM = System.getProperty("TARGET_RPM").toInt

    validateSimulationParameter("TARGET_RPM", targetRPM)

    summary.append("\nTarget RPM: " + targetRPM)
    val targetRPS = targetRPM / 60
    summary.append("\nTarget RPS: " + targetRPS)

    // If simulation specific weighting exist then use it. Otherwise, use the default weightings.
    val distributionParams = if ((simulationParams \\ "simulations" \ "simulation" \ simulationType \ "ActionWeighting").isEmpty) {
      simulationParams \\ "simulations" \ "DefaultWeighting"
    } else {
      simulationParams \\ "simulations" \ "simulation" \ simulationType \ "ActionWeighting"
    }

    val userDist = (distributionParams \ "_" \ "UserDistribution").map(w => w.text.toDouble)

    //Get map of all ActionWeighting names and get all action weightings. Combine (zip) the action names and weightings into a map, eg ["GET_Postcode", 1.1]
    val userDistribution = ((distributionParams \ "_").map(w => w.label) zip userDist).toMap

    // Check sum of all weightings from the simulation type config equals to 100
    if (userDist.sum.toFloat != 100)
      throw new Exception ("ERROR - Sum of User Distribution must be 100. Current sum is - " + userDist.sum.toFloat )


    //Store requests per iteration of each action weighting in a sequence var
    val requestsPerIteration = (distributionParams \ "_" \ "RequestsPerIteration").map(w=> w.text.toDouble)

    //Calculate weighted requests per iteration
    val weightedRequestsPerIteration = userDist.zip(requestsPerIteration).map{case (a,b) => a * 0.01 * b}.sum

    summary.append("\nRequests Per Iteration: " + weightedRequestsPerIteration)
    summary.append("\n***********************************************************")

    //Calculate minimum and maximum pacing
    val (pacingMin, pacingMax) = calculateIterationPacing(targetRPS, noOfUsers, weightedRequestsPerIteration)

    summary.append("\nPacing Average: " + (pacingMin + ((pacingMax - pacingMin) / 2)) + " ms")
    summary.append("\nPacing Minimum: " + pacingMin + " ms")
    summary.append("\nPacing Maximum: " + pacingMax + " ms")

    val pacingMaxSeconds = pacingMax/1000

    //Throw an error if pacing greater than the ramp up duration
    if (pacingMaxSeconds/1000 > rampUpDuration)
      throw new Exception("ERROR - Calculated pacing is greater than ramp up duration. Pacing of " + pacingMaxSeconds +
        " seconds is greater than the ramp up duration of " + rampUpDuration + " seconds. Decrease the pacing by decreasing the number of users or increasing the ramp up duration"

      )

    summary.append("\n***********************************************************\n\n")
    print(summary)

    (pacingMin, pacingMax, userDistribution)

  }

  /**
   * Validate the simulation parameter
   * @param name Parameter for validation
   * @param value Value of parameter for validation
   * @return noOfUsers, rampUpDuration, peakLoadDuration
   */
  def validateSimulationParameter(name: String, value: Int): Unit = {
    if(name == "USERS" || name == "RAMP_UP_DURATION" || name == "PEAK_LOAD_DURATION") {
      if(value < 1)
        throw new Exception("ERROR - Invalid value provided. The minimum value accepted for " + name + " is 1 i.e. it cannot be less than 1 minute. Current value is " + value + ".")
    }

    else if(name == "TARGET_RPM") {
      if(value < 60)
        throw new Exception("ERROR - Invalid value provided. The minimum value accepted for " + name + " is 60 i.e. it cannot be less than 60 requests per minute. Current value is " + value + ".")
    }

    else {
      throw new Exception("ERROR - Invalid name - " + name)
    }
  }


  /***
   * Calculates the iteration pacing required to meet the input RPS (target RPS) based on number of users and requests
   * @param targetRPS the Target Requests per second
   * @param numberOfUsers the number of users/threads
   * @param requestsPerIteration requests per iteration
   * @return pacingMin, pacingMax
   */
  def calculateIterationPacing(targetRPS: Int, numberOfUsers: Int, requestsPerIteration: Double) : (Int, Int) ={

    var pacingMin = 0
    var pacingMax = 0

    if (targetRPS > 0) {
      val requiredPacing: Int = (((numberOfUsers.toDouble / targetRPS.toDouble) * requestsPerIteration) * 1000).ceil.toInt
      pacingMin = requiredPacing - (requiredPacing * .1).ceil.toInt
      pacingMax = requiredPacing + (requiredPacing * .1).ceil.toInt

      //Check pacing is set to at least 1 seconds per request to ensure it will take effect
      if((requiredPacing/1000)  < (requestsPerIteration * 2)) {
        throw new Exception("ERROR - Calculated pacing too low - Pacing value of " + requiredPacing +
          " ms calculated to reach target of " + targetRPS + " RPS with " + numberOfUsers +
        " user(s). This is less than the minimum 2 seconds for each request in the scenario (total = " + requestsPerIteration
          + "). Increase the number of users or reduce the Target RPM. ")
      }
    }

    (pacingMin, pacingMax)
  }

}
