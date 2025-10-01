package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.Personas

/**
 * Test runner for full conversation generation.
 * This generates a complete conversation from use case to final messages.
 */
object TestFullConversationGeneration {
  
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("FULL CONVERSATION GENERATION TEST")
    println("="*80)
    
    // Load personas and pick a random one
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val randomPerson = allPersons(scala.util.Random.nextInt(allPersons.length))
    
    println(s"\nSelected Person: ${randomPerson.getPrimitiveRoleName}")
    println(s"Category: ${randomPerson.category}")
    
    // Create the generator
    val generator = new ConversationEvidenceGenerator()
    
    try {
      // Phase 1: Generate use case
      println("\n" + "="*80)
      println("PHASE 1: GENERATING USE CASE")
      println("="*80)
      val useCases = generator.generateUseCases(randomPerson)
      val selectedUseCase = useCases.head
      
      println(s"Category: ${selectedUseCase.category}")
      println(s"Scenario: ${selectedUseCase.scenario_description}")
      
      // Phase 2: Generate evidence core
      println("\n" + "="*80)
      println("PHASE 2: GENERATING EVIDENCE CORE")
      println("="*80)
      val evidenceCore = generator.generateEvidenceCore(randomPerson, selectedUseCase)
      
      println(s"Opening Message: ${evidenceCore.question}")
      println(s"Expected Flow: ${evidenceCore.answer}")
      
      // Phase 3: Generate full conversation
      println("\n" + "="*80)
      println("PHASE 3: GENERATING FULL CONVERSATION")
      println("="*80)
      val conversations = generator.generateConversationsFromCore(randomPerson, selectedUseCase, evidenceCore)
      val conversation = conversations.head
      
      println(s"Generated conversation with ${conversation.messages.length} messages")
      println(s"Contains evidence: ${conversation.containsEvidence.getOrElse(false)}")
      
      // Show first few exchanges
      println("\n" + "-"*40)
      println("FIRST 10 MESSAGES:")
      println("-"*40)
      conversation.messages.take(10).zipWithIndex.foreach { case (msg, idx) =>
        println(s"\n${idx + 1}. ${msg.speaker}:")
        val truncated = if (msg.text.length > 200) msg.text.take(197) + "..." else msg.text
        println(s"   ${truncated}")
      }
      
      // Show conversation statistics
      println("\n" + "-"*40)
      println("CONVERSATION STATISTICS:")
      println("-"*40)
      val userMessages = conversation.messages.count(_.speaker == "User")
      val assistantMessages = conversation.messages.count(_.speaker == "Assistant")
      println(s"Total messages: ${conversation.messages.length}")
      println(s"User messages: $userMessages")
      println(s"Assistant messages: $assistantMessages")
      
      // Verify the conversation makes sense
      println("\n" + "-"*40)
      println("VERIFICATION:")
      println("-"*40)
      
      // Check that it starts with the opening message
      val startsWithOpening = conversation.messages.headOption.exists { msg =>
        msg.speaker == "User" && (msg.text == evidenceCore.question || 
        msg.text == evidenceCore.message_evidences.headOption.map(_.text).getOrElse(""))
      }
      println(s"Starts with opening message: $startsWithOpening")
      
      // Check alternating speakers
      val alternatesSpeakers = conversation.messages.sliding(2).forall {
        case Seq(m1, m2) => m1.speaker != m2.speaker
        case _ => true
      }
      println(s"Speakers alternate properly: $alternatesSpeakers")
      
      // Check reasonable message lengths
      val avgMessageLength = conversation.messages.map(_.text.length).sum / conversation.messages.length
      println(s"Average message length: $avgMessageLength characters")
      
    } catch {
      case e: Exception =>
        println(s"\nError during generation: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}