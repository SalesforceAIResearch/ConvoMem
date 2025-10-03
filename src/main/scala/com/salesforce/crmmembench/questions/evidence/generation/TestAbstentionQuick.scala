package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators._
import com.salesforce.crmmembench.{Config, Personas}

object TestAbstentionQuick {
  
  def main(args: Array[String]): Unit = {
    println("=== Testing Abstention Evidence Generator ===\n")
    
    // Create generator with 1 evidence piece
    val generator = new AbstentionEvidenceGenerator(1)
    
    // Load a test person
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = allPersons.find(_.getPrimitiveRoleName == "Marketing_Manager")
      .getOrElse(allPersons.head)
    
    println(s"Testing with person: ${person.getPrimitiveRoleName}")
    
    // Test 1: Generate use cases
    println("\n1. GENERATING USE CASES:")
    val useCases = generator.generateUseCases(person).take(5)
    useCases.zipWithIndex.foreach { case (uc, i) =>
      println(s"\nUse Case ${i+1}:")
      println(s"  Category: ${uc.category}")
      println(s"  Scenario: ${uc.scenario_description}")
    }
    
    // Test 2: Generate evidence core
    val useCase = useCases.head
    println(s"\n\n2. GENERATING EVIDENCE CORE for first use case:")
    try {
      val evidenceCore = generator.generateEvidenceCore(person, useCase)
      println(s"\nGenerated Evidence Core:")
      println(s"  Question: ${evidenceCore.question}")
      println(s"  Answer: ${evidenceCore.answer}")
      println(s"  Evidence messages (${evidenceCore.message_evidences.length}):")
      evidenceCore.message_evidences.foreach { msg =>
        println(s"    [${msg.speaker}]: ${msg.text}")
      }
      
      // Check if answer is the expected abstention answer
      val expectedAnswer = "There is no information in prior conversations to answer this question"
      if (evidenceCore.answer == expectedAnswer) {
        println("\n✅ Answer is correctly formatted for abstention")
      } else {
        println(s"\n❌ Answer doesn't match expected abstention format!")
        println(s"   Expected: $expectedAnswer")
        println(s"   Got: ${evidenceCore.answer}")
      }
      
      // Test 3: Quick verification test
      println("\n\n3. TESTING VERIFICATION LOGIC:")
      
      // Create minimal evidence item for testing
      val dummyConversation = Conversation(
        messages = evidenceCore.message_evidences ++ List(
          Message("User", "So what else can I help you with?"),
          Message("Assistant", "I'm here to help with any questions you have."),
          Message("User", evidenceCore.question),
          Message("Assistant", "Let me check what we've discussed...")
        ),
        id = Some("test-conv"),
        containsEvidence = Some(true)
      )
      
      val evidenceItem = EvidenceItem(
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        message_evidences = evidenceCore.message_evidences,
        conversations = List(dummyConversation),
        category = useCase.category,
        scenario_description = Some(useCase.scenario_description),
        personId = Some(person.id)
      )
      
      // Test without evidence (using dummy conversation)
      println("\n3a. Testing WITHOUT evidence verification:")
      val withoutEvidenceResult = FilteringVerification.verifyWithoutEvidence(evidenceItem, DefaultAnsweringEvaluation)
      withoutEvidenceResult match {
        case Some(result) =>
          println(s"  Passed: ${result.passed}")
          println(s"  Model Answer: ${result.lastModelAnswer}")
          
          // Analyze the result
          val modelSaidIDontKnow = result.lastModelAnswer.toLowerCase.contains("no information") || 
                                   result.lastModelAnswer.toLowerCase.contains("don't know") ||
                                   result.lastModelAnswer.toLowerCase.contains("not available") ||
                                   result.lastModelAnswer.toLowerCase.contains("cannot answer")
          
          println(s"  Model abstained: $modelSaidIDontKnow")
          
          if (!result.passed && modelSaidIDontKnow) {
            println("\n  ⚠️  ISSUE IDENTIFIED: Model correctly abstained without evidence,")
            println("     but verification marked it as failure!")
            println("     This happens because abstention answer is always 'correct'")
            println("     even without context.")
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