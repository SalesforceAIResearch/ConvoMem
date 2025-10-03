package com.salesforce.crmmembench.personas

import com.salesforce.crmmembench.GeneralPrompts.PROJECT_BACKGROUND
import com.salesforce.crmmembench.LLM_endpoints.Gemini
import com.salesforce.crmmembench.Personas.{DefaultVersion, Person, savePersonasToFile}
import com.salesforce.crmmembench.personas.Prompts.INITIAL_ROLES_GENERATION
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.{Decoder, HCursor}

object GeneratePersonas {
  
  /**
   * Number of personas to generate.
   * Default is 50 to match the prompt, but can be overridden for testing.
   */
  val numberOfPersonas: Int = 100

  case class RolesResponse(roles: List[Person])

  // Custom decoder for Person to handle missing id field
  implicit val personDecoder: Decoder[Person] = (c: HCursor) => for {
    category <- c.get[String]("category")
    role_name <- c.get[String]("role_name")
    description <- c.get[String]("description")
    background <- c.getOrElse[Option[String]]("background")(None)
  } yield Person(
    category = category,
    role_name = role_name,
    description = description,
    background = background
  )

  def main(args: Array[String]): Unit = {
    // Replace "50" in the prompt with the configurable number
    val customPrompt = INITIAL_ROLES_GENERATION.replaceAll("\\b50\\b", numberOfPersonas.toString)
    val jsonResponse = Gemini.proJson.generateContent(PROJECT_BACKGROUND + customPrompt).get.content
    val decodedPersons = decode[RolesResponse](jsonResponse)
    decodedPersons match {
      case Right(rolesResponse) =>
        val personList: List[Person] = rolesResponse.roles
        println(s"Successfully decoded ${personList.length} persons.")
        personList.take(3).foreach(println)
        savePersonasToFile(rolesResponse, version = Some(DefaultVersion))

      case Left(error) =>
        throw new RuntimeException(s"Failed to decode JSON: $error")
    }
  }
}
