package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators._

object TestPromptGeneration extends App {
  println("Testing Prompt Generation with Questions in Scenarios")
  println("="*80)
  
  // Load a test persona
  val personas = Personas.loadPersonas(stage = "enriched_backgrounds").roles
  val testPerson = personas.head
  
  println(s"\nTest Person: ${testPerson.role_name}")
  println(s"Description: ${testPerson.description}")
  
  // Test UserFactsEvidenceGenerator
  println("\n" + "="*80)
  println("1. USER FACTS EVIDENCE GENERATOR")
  println("="*80)
  
  val userFactsGen = new UserFactsEvidenceGenerator(1)
  val userFactsParts = userFactsGen.getUseCaseSummaryPromptParts(testPerson)
  
  println("\nExample Use Case:")
  println(s"Category: ${userFactsParts.exampleUseCase.category}")
  println(s"Scenario: ${userFactsParts.exampleUseCase.scenario_description}")
  
  // Generate a few use cases
  println("\nGenerating 3 use cases...")
  val userFactsUseCases = userFactsGen.generateUseCases(testPerson).take(3)
  userFactsUseCases.zipWithIndex.foreach { case (uc, i) =>
    println(s"\nUse Case ${i+1}:")
    println(s"Category: ${uc.category}")
    println(s"Scenario: ${uc.scenario_description}")
  }
  
  // Test AssistantFactsEvidenceGenerator
  println("\n" + "="*80)
  println("2. ASSISTANT FACTS EVIDENCE GENERATOR")
  println("="*80)
  
  val assistantFactsGen = new AssistantFactsEvidenceGenerator(1)
  val assistantFactsParts = assistantFactsGen.getUseCaseSummaryPromptParts(testPerson)
  
  println("\nExample Use Case:")
  println(s"Category: ${assistantFactsParts.exampleUseCase.category}")
  println(s"Scenario: ${assistantFactsParts.exampleUseCase.scenario_description}")
  
  // Test ChangingEvidenceGenerator
  println("\n" + "="*80)
  println("3. CHANGING EVIDENCE GENERATOR")
  println("="*80)
  
  val changingGen = new ChangingEvidenceGenerator(2)
  val changingParts = changingGen.getUseCaseSummaryPromptParts(testPerson)
  
  println("\nExample Use Case:")
  println(s"Category: ${changingParts.exampleUseCase.category}")
  println(s"Scenario: ${changingParts.exampleUseCase.scenario_description}")
  
  // Test generation of evidence core from use case
  println("\n" + "="*80)
  println("4. TESTING CORE GENERATION WITH QUESTION FROM USE CASE")
  println("="*80)
  
  if (userFactsUseCases.nonEmpty) {
    val testUseCase = userFactsUseCases.head
    println(s"\nUse Case: ${testUseCase.scenario_description}")
    
    val core = userFactsGen.generateEvidenceCore(testPerson, testUseCase)
    println(s"\nGenerated Core:")
    println(s"Question: ${core.question}")
    println(s"Answer: ${core.answer}")
    println(s"Evidence:")
    core.message_evidences.foreach { msg =>
      println(s"  [${msg.speaker}]: ${msg.text}")
    }
  }
  
  println("\n" + "="*80)
  println("PROMPT GENERATION TEST COMPLETE")
  println("="*80)
}