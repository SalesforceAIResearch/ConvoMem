package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.Personas

/**
 * Simple runner to test conversation use case generation.
 * This generates use cases for a random person and prints them out.
 */
object TestConversationUseCaseGeneration {
  
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("CONVERSATION USE CASE GENERATION TEST")
    println("="*80)
    
    // Load personas and pick a random one
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val randomPerson = allPersons(scala.util.Random.nextInt(allPersons.length))
    
    println(s"\nSelected Person: ${randomPerson.getPrimitiveRoleName}")
    println(s"Category: ${randomPerson.category}")
    println(s"Description: ${randomPerson.description}")
    randomPerson.background.foreach(bg => println(s"Background: ${bg.take(200)}..."))
    
    // Create the generator
    val generator = new ConversationEvidenceGenerator()
    
    // First, let's look at the prompt that will be generated
    println("\n" + "="*80)
    println("USE CASE SUMMARY PROMPT")
    println("="*80)
    val prompt = generator.getUseCaseSummaryPrompt(randomPerson)
    println(prompt)
    
    // Now generate the use cases
    println("\n" + "="*80)
    println("GENERATING USE CASES...")
    println("="*80)
    
    try {
      val useCases = generator.generateUseCases(randomPerson)
      
      println(s"\nGenerated ${useCases.length} use cases:")
      println("-"*80)
      
      useCases.zipWithIndex.foreach { case (useCase, index) =>
        println(s"\nUse Case ${index + 1}:")
        println(s"  ID: ${useCase.id}")
        println(s"  Category: ${useCase.category}")
        println(s"  Scenario: ${useCase.scenario_description}")
        println("-"*40)
      }
      
      // Print category distribution
      println("\nCategory Distribution:")
      val categoryGroups = useCases.groupBy(_.category)
      categoryGroups.foreach { case (category, cases) =>
        println(s"  $category: ${cases.length} use cases")
      }
      
    } catch {
      case e: Exception =>
        println(s"\nError generating use cases: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}