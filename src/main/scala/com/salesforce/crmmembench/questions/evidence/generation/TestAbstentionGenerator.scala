package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.generators.AbstentionEvidenceGenerator
import com.salesforce.crmmembench.Personas

object TestAbstentionGenerator {
  def main(args: Array[String]): Unit = {
    println("=== Testing Abstention Evidence Generator ===\n")
    
    // Create generator with 1 evidence piece
    val generator = new AbstentionEvidenceGenerator(1)
    
    // Load a test person
    val persons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = persons.head
    
    println(s"Testing with person: ${person.getPrimitiveRoleName}")
    println(s"Background: ${person.background}\n")
    
    // Test 1: Use Case Generation
    println("1. USE CASE GENERATION PROMPT:")
    println("-" * 80)
    val useCasePrompt = generator.getUseCaseSummaryPrompt(person)
    println(useCasePrompt)
    println("-" * 80)
    
    // Generate use cases
    println("\nGenerating use cases...")
    val useCases = generator.generateUseCases(person).take(3)
    useCases.zipWithIndex.foreach { case (uc, i) =>
      println(s"\nUse Case ${i+1}:")
      println(s"  Category: ${uc.category}")
      println(s"  Scenario: ${uc.scenario_description}")
    }
    
    // Test 2: Evidence Core Generation
    val useCase = useCases.head
    println(s"\n\n2. EVIDENCE CORE GENERATION PROMPT (for first use case):")
    println("-" * 80)
    val evidenceCorePrompt = generator.getEvidenceCorePrompt(person, useCase)
    println(evidenceCorePrompt)
    println("-" * 80)
    
    // Generate evidence core
    println("\nGenerating evidence core...")
    try {
      val evidenceCore = generator.generateEvidenceCore(person, useCase)
      println(s"\nGenerated Evidence Core:")
      println(s"  Question: ${evidenceCore.question}")
      println(s"  Answer: ${evidenceCore.answer}")
      println(s"  Evidence messages:")
      evidenceCore.message_evidences.foreach { msg =>
        println(s"    [${msg.speaker}]: ${msg.text}")
      }
      
      // Test 3: Conversation Generation
      println(s"\n\n3. CONVERSATION GENERATION:")
      val conversationPromptParts = generator.getConversationPromptParts(person, useCase, evidenceCore)
      println(s"Evidence Type: ${conversationPromptParts.evidenceType}")
      println(s"Scenario Description: ${conversationPromptParts.scenarioDescription}")
      
      // Test verification
      println("\n\n4. TESTING VERIFICATION:")
      
      // Create a complete evidence item for testing
      val conversations = generator.generateConversationsFromCore(person, useCase, evidenceCore)
      val evidenceItem = com.salesforce.crmmembench.questions.evidence.EvidenceItem(
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        message_evidences = evidenceCore.message_evidences,
        conversations = conversations,
        category = useCase.category,
        scenario_description = Some(useCase.scenario_description),
        personId = Some(person.id)
      )
      
      // Test with evidence
      println("\n4a. Testing WITH evidence:")
      val withEvidenceResult = FilteringVerification.verifyWithEvidence(
        evidenceItem, 
        requiredPasses = 1, 
        extensive = false,
        answeringEvaluation = DefaultAnsweringEvaluation
      )
      withEvidenceResult match {
        case Some(result) =>
          println(s"  Passed: ${result.passed}")
          println(s"  Model Answer: ${result.lastModelAnswer}")
          result.failureReason.foreach(reason => println(s"  Failure Reason: $reason"))
        case None =>
          println("  ERROR: Failed to get verification result")
      }
      
      // Test without evidence
      println("\n4b. Testing WITHOUT evidence:")
      val withoutEvidenceResult = FilteringVerification.verifyWithoutEvidence(evidenceItem, DefaultAnsweringEvaluation)
      withoutEvidenceResult match {
        case Some(result) =>
          println(s"  Passed: ${result.passed}")
          println(s"  Model Answer: ${result.lastModelAnswer}")
          result.failureReason.foreach(reason => println(s"  Failure Reason: $reason"))
          
          // For abstention, we expect the model to say "I don't know" without evidence
          // So if the model gave the abstention answer without evidence, that's actually correct behavior
          if (result.lastModelAnswer.toLowerCase.contains("no information") || 
              result.lastModelAnswer.toLowerCase.contains("don't know")) {
            println("  ⚠️  Model correctly abstained even without evidence context!")
          }
        case None =>
          println("  ERROR: Failed to get verification result")
      }
      
    } catch {
      case e: Exception =>
        println(s"\nError during generation: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}