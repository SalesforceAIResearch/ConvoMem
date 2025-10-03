package com.salesforce.crmmembench

import com.salesforce.crmmembench.personas.GeneratePersonas.RolesResponse
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.Source

object Personas {

  case class Person(
                     category: String,
                     role_name: String,
                     description: String,
                     background: Option[String],
                     id: String = java.util.UUID.randomUUID().toString
                   ) {
    def getPrimitiveRoleName: String = role_name.replaceAll(" ", "_").filter(c => c.isLetterOrDigit || c == '_')
  }

  // Default version for development to avoid creating too many files
  val DefaultVersion = "default"

  def savePersonasToFile(rolesResponse: RolesResponse, stage: String = "initial_rough_roles", version: Option[String] = Some(DefaultVersion)): Unit = {
    // Use provided version if available, otherwise generate a new one based on current time
    val actualVersion = version.getOrElse {
      val versionFormatter = DateTimeFormatter.ofPattern("MMdd_HHmm")
      LocalDateTime.now().format(versionFormatter)
    }

    // Define the directory and filename
    val outputDir = s"src/main/resources/personas/$stage"
    val fileName = s"personas_${actualVersion}.json"
    val outputPath = Paths.get(outputDir, fileName)

    // Create the directory if it doesn't exist
    Files.createDirectories(Paths.get(outputDir))

    // Convert the case class list back to a pretty-printed JSON string
    val jsonString = rolesResponse.asJson.spaces2 // Using spaces2 for readability

    // Write the JSON string to the file
    val writer = new PrintWriter(new File(outputPath.toString))
    writer.write(jsonString)
    writer.close()

    println(s"\nSuccessfully converted back to JSON and saved to: $outputPath")
  }

  def loadPersonas(stage: String = "initial_rough_roles", version: Option[String] = None): RolesResponse = {
    // Use provided version if available, otherwise use the default version
    val actualVersion = version.getOrElse(DefaultVersion)

    // Define the directory and filename
    val inputDir = s"src/main/resources/personas/$stage"
    val fileName = s"personas_${actualVersion}.json"
    val inputPath = Paths.get(inputDir, fileName).toString

    // Read the JSON file and ensure the source is closed after use
    val jsonString = try {
      val source = Source.fromFile(inputPath)
      try {
        source.mkString
      } finally {
        source.close()
      }
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Failed to read file $inputPath: ${e.getMessage}", e)
    }

    // Parse the JSON into a RolesResponse object
    val response = decode[RolesResponse](jsonString) match {
      case Right(rolesResponse) =>
        println(s"Successfully loaded ${rolesResponse.roles.length} personas from: $inputPath")
        rolesResponse
      case Left(error) =>
        throw new RuntimeException(s"Failed to decode JSON from $inputPath: $error")
    }
    response.copy(roles = response.roles.sortBy(_.id).take(50))
  }
}
