package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.generators.TemporalEvidenceGenerator
import com.salesforce.crmmembench.{Config, Personas}
import com.salesforce.crmmembench.questions.evidence._

object TestTemporalGenerator {
  def main(args: Array[String]): Unit = {
    println("Testing Temporal Evidence Generator")
    println("="*80)
    
    // Get a test person
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = allPersons.find(_.getPrimitiveRoleName == "Marketing_Manager")
      .getOrElse(allPersons.head)
    
    // Test with different evidence counts
    val evidenceCounts = List(1, 2, 3)
    
    for (count <- evidenceCounts) {
      println(s"\nTesting with $count evidence item(s):")
      println("-"*60)
      
      val generator = new TemporalEvidenceGenerator(count)
      
      // Test use case generation
      println("\n1. Testing Use Case Generation:")
      val useCases = generator.generateUseCases(person).take(3)
      
      println(s"\nGenerated ${useCases.length} Use Cases for ${person.getPrimitiveRoleName}")
      useCases.zipWithIndex.foreach { case (uc, i) =>
        println(s"\n${i+1}. Category: ${uc.category}")
        println(s"   Scenario: ${uc.scenario_description}")
      }
      
      // Test evidence core generation
      println("\n2. Testing Evidence Core Generation:")
      val testUseCase = EvidenceUseCase(
        id = 1,
        category = "Temporal Reasoning",
        scenario_description = count match {
          case 1 => "This is a temporal question about duration calculation. The user mentioned starting marathon training 'three months ago' and wants to know how long they've been training."
          case 2 => "This is about event sequencing. The user got their driver's license 'two years ago' and bought their first car 'six months later', asking which happened first."
          case _ => s"This involves tracking a $count-stage process. The user went through multiple job interviews over 'the past two months' and wants to understand the timeline."
        }
      )
      
      println(s"\nUse Case: ${testUseCase.scenario_description}")
      val core = generator.generateEvidenceCore(person, testUseCase)
      
      println(s"\nGenerated Core:")
      println(s"Question: ${core.question}")
      println(s"Answer: ${core.answer}")
      println(s"\nEvidence Messages:")
      core.message_evidences.foreach { msg =>
        println(s"  [${msg.speaker}]: ${msg.text}")
      }
      
      // Test conversation generation
      println("\n3. Testing Conversation Generation:")
      val conversations = generator.generateConversationsFromCore(person, testUseCase, core)
      
      println(s"\nGenerated ${conversations.length} conversation(s)")
      conversations.headOption.foreach { conv =>
        println(s"First conversation has ${conv.messages.length} messages")
        println("\nFirst 3 messages:")
        conv.messages.take(3).foreach { msg =>
          println(s"  [${msg.speaker}]: ${msg.text.take(100)}${if(msg.text.length > 100) "..." else ""}")
        }
        println("\n...")
        println("\nLast 3 messages:")
        conv.messages.takeRight(3).foreach { msg =>
          println(s"  [${msg.speaker}]: ${msg.text.take(100)}${if(msg.text.length > 100) "..." else ""}")
        }
      }
    }
    
    println("\n" + "="*80)
    println("Temporal Evidence Generator Testing Complete!")
    println("\nNext steps:")
    println("1. Review the generated outputs above")
    println("2. Adjust prompts in TemporalEvidenceGenerator if needed")
    println("3. Run more comprehensive tests")
    println("4. Create short runners once satisfied with quality")
    println("\nTo see debug output (prompts used), set Config.DEBUG = true")
  }
}