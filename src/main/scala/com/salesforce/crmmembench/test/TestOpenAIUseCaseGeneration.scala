package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.LLM_endpoints.OpenAI
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator
import com.salesforce.crmmembench.{Personas, Utils}

/**
 * Test OpenAI model for use case generation to identify the issue
 */
object TestOpenAIUseCaseGeneration {
  def main(args: Array[String]): Unit = {
    println("Testing OpenAI for use case generation...")
  
  // Create a custom generator that uses only OpenAI
  val generator = new UserFactsEvidenceGenerator(evidenceCount = 1) {
    override val llmModel = OpenAI.gpt4oJson
    override val runShort = true // Only process a few people
  }
  
  // Load a person
  val person = Personas.loadPersonas(stage = "enriched_backgrounds").roles.head
  println(s"Testing with person: ${person.getPrimitiveRoleName}")
  
  try {
    // Try to generate use cases
    println("\nAttempting to generate use cases with OpenAI.gpt4oJson...")
    val useCases = generator.generateUseCases(person, targetCount = 2)
    
    println(s"✅ Successfully generated ${useCases.length} use cases with OpenAI!")
    useCases.foreach { useCase =>
      println(s"  - Category: ${useCase.category}")
      println(s"    Scenario: ${useCase.scenario_description.take(100)}...")
    }
  } catch {
    case e: Exception =>
      println(s"❌ Failed to generate use cases with OpenAI!")
      println(s"Exception type: ${e.getClass.getName}")
      println(s"Message: ${e.getMessage}")
      println("\nStack trace:")
      e.printStackTrace()
      
      // Check if it's an environment issue
      if (e.getMessage != null && e.getMessage.contains("OPENAI_API_KEY")) {
        println("\n⚠️  Issue: OpenAI API key not set in environment")
      }
      
      // Try to get more detailed error info
      e.getCause match {
        case null => // No cause
        case cause =>
          println(s"\nCause: ${cause.getClass.getName}")
          println(s"Cause message: ${cause.getMessage}")
      }
  }
  }
}