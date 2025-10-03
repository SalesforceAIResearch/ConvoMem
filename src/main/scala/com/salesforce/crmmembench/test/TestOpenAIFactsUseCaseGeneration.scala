package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.LLM_endpoints.OpenAI
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator
import com.salesforce.crmmembench.{Personas, Utils}
import io.circe.syntax._
import io.circe.generic.auto._

/**
 * Test object to generate use cases using OpenAI gpt-4o with UserFactsEvidenceGenerator
 */
object TestOpenAIFactsUseCaseGeneration {
  def main(args: Array[String]): Unit = {
    println("Testing OpenAI gpt-4o for use case generation with UserFactsEvidenceGenerator...")
    println("="*80)
    
    // Create a custom generator that uses only OpenAI gpt-4o
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 1) {
      override val llmModel = OpenAI.gpt4oJson
      override val runShort = true // Only process a few people
      override lazy val useCasesPerPersonConfig: Int = 5 // Generate 5 use cases per person
    }
    
    // Load personas
    val personas = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = personas.head // Take the first person
    
    println(s"\nGenerating use cases for: ${person.getPrimitiveRoleName}")
    println(s"Person ID: ${person.id}")
    println("-"*60)
    
    try {
      // Generate use cases
      val useCases = generator.generateUseCases(person, targetCount = 2)
      
      println(s"\n✅ Successfully generated ${useCases.length} use cases with OpenAI gpt-4o!")
      println("\nGenerated use cases:")
      println("="*80)
      
      useCases.zipWithIndex.foreach { case (useCase, index) =>
        println(s"\nUse Case ${index + 1}:")
        println(s"ID: ${useCase.id}")
        println(s"Category: ${useCase.category}")
        println(s"Scenario Description: ${useCase.scenario_description}")
        println("-"*60)
      }
      
      // Also print as JSON for easier inspection
      println("\n\nJSON representation of first use case:")
      println("="*80)
      println(useCases.head.asJson.spaces2)
      
    } catch {
      case e: Exception =>
        println(s"\n❌ Failed to generate use cases!")
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}