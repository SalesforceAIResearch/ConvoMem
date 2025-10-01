package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence.EvidenceUseCase

/**
 * Simple runner to test conversation core generation.
 * This generates conversation cores (starters) for use cases.
 */
object TestConversationCoreGeneration {
  
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("CONVERSATION CORE GENERATION TEST")
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
    
    // First generate use cases
    println("\n" + "="*80)
    println("GENERATING USE CASES...")
    println("="*80)
    
    try {
      val useCases = generator.generateUseCases(randomPerson)
      println(s"Generated ${useCases.length} use cases")
      
      // Pick a few random use cases to test core generation
      val testUseCases = scala.util.Random.shuffle(useCases).take(3)
      
      testUseCases.foreach { useCase =>
        println("\n" + "="*80)
        println(s"USE CASE: ${useCase.category}")
        println("="*80)
        println(s"Scenario: ${useCase.scenario_description}")
        
        // Show the prompt that will be generated
        println("\n" + "-"*40)
        println("EVIDENCE CORE PROMPT:")
        println("-"*40)
        val promptParts = generator.getEvidenceCorePromptParts(randomPerson, useCase)
        println(s"Task: ${promptParts.taskDescription}")
        println(s"\nInstructions: ${promptParts.specificInstructions}")
        promptParts.additionalGuidance.foreach(g => println(s"\nGuidance: $g"))
        
        // Generate the evidence core
        println("\n" + "-"*40)
        println("GENERATING EVIDENCE CORE...")
        println("-"*40)
        
        try {
          val evidenceCore = generator.generateEvidenceCore(randomPerson, useCase)
          
          println("\nGenerated Evidence Core:")
          println(s"Question (Conversation Starter):")
          println(s"  ${evidenceCore.question}")
          println(s"\nAnswer (Expected Flow Description):")
          println(s"  ${evidenceCore.answer}")
          println(s"\nMessage Evidences (Opening Message):")
          evidenceCore.message_evidences.foreach { msg =>
            println(s"  ${msg.speaker}: ${msg.text}")
          }
          
          // Verify the core makes sense
          println("\n" + "-"*40)
          println("VERIFICATION:")
          println("-"*40)
          
          // Check that question and message_evidences match
          val messageText = evidenceCore.message_evidences.headOption.map(_.text).getOrElse("")
          val questionMatchesMessage = evidenceCore.question.trim == messageText.trim
          println(s"Question matches message evidence: $questionMatchesMessage")
          
          // Check that answer describes conversation flow
          val answerDescribesFlow = evidenceCore.answer.toLowerCase.contains("conversation") || 
                                   evidenceCore.answer.toLowerCase.contains("discuss") ||
                                   evidenceCore.answer.toLowerCase.contains("explore")
          println(s"Answer describes conversation flow: $answerDescribesFlow")
          
          // Check message is from User
          val messageFromUser = evidenceCore.message_evidences.headOption.exists(_.speaker == "User")
          println(s"Message is from User: $messageFromUser")
          
        } catch {
          case e: Exception =>
            println(s"Error generating evidence core: ${e.getMessage}")
            e.printStackTrace()
        }
      }
      
    } catch {
      case e: Exception =>
        println(s"\nError in generation: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}