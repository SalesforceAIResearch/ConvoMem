package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence.generators.ImplicitConnectionEvidenceGenerator

/**
 * Test runner for ImplicitConnectionEvidenceGenerator development
 */
object TestImplicitConnection {
  
  def main(args: Array[String]): Unit = {
    val phase = args.headOption.getOrElse("all")
    
    // Get a test person
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = allPersons.find(_.getPrimitiveRoleName == "Sales_Representative")
      .getOrElse(allPersons.head)
    
    println(s"Testing ImplicitConnectionEvidenceGenerator with: ${person.role_name}")
    println("="*80)
    
    val generator = new ImplicitConnectionEvidenceGenerator(1)
    
    phase match {
      case "use-case" => testUseCases(generator, person)
      case "core" => testCore(generator, person)
      case "conversation" => testConversation(generator, person)
      case "all" => testAll(generator, person)
      case _ => println(s"Unknown phase: $phase. Use: use-case, core, conversation, or all")
    }
  }
  
  def testUseCases(generator: ImplicitConnectionEvidenceGenerator, person: Personas.Person): Unit = {
    println("\n=== TESTING USE CASE GENERATION ===")
    
    val useCases = generator.generateUseCases(person)
    println(s"\nGenerated ${useCases.length} use cases:")
    
    useCases.zipWithIndex.foreach { case (uc, i) =>
      println(s"\n${i+1}. Category: ${uc.category}")
      println(s"   Scenario: ${uc.scenario_description}")
    }
    
    // Show the prompt used
    println("\n" + "-"*80)
    println("PROMPT USED:")
    println("-"*80)
    println(generator.getUseCaseSummaryPrompt(person))
  }
  
  def testCore(generator: ImplicitConnectionEvidenceGenerator, person: Personas.Person): Unit = {
    println("\n=== TESTING EVIDENCE CORE GENERATION ===")
    
    // First generate a use case
    val useCases = generator.generateUseCases(person)
    val useCase = useCases.head
    
    println(s"\nUse Case: ${useCase.category}")
    println(s"Scenario: ${useCase.scenario_description}")
    
    // Generate core
    val core = generator.generateEvidenceCore(person, useCase)
    
    println(s"\nGenerated Core:")
    println(s"Question: ${core.question}")
    println(s"\nAnswer (Rubric):")
    println(core.answer)
    println(s"\nEvidence Messages:")
    core.message_evidences.foreach { msg =>
      println(s"  [${msg.speaker}]: ${msg.text}")
    }
    
    // Show the prompt
    println("\n" + "-"*80)
    println("PROMPT USED:")
    println("-"*80)
    println(generator.getEvidenceCorePrompt(person, useCase))
  }
  
  def testConversation(generator: ImplicitConnectionEvidenceGenerator, person: Personas.Person): Unit = {
    println("\n=== TESTING CONVERSATION GENERATION ===")
    
    // Generate use case and core first
    val useCases = generator.generateUseCases(person)
    val useCase = useCases.head
    val core = generator.generateEvidenceCore(person, useCase)
    
    println(s"Context: ${useCase.category}")
    println(s"Question: ${core.question}")
    
    // Generate conversation
    val conversations = generator.generateConversationsFromCore(person, useCase, core)
    
    println(s"\nGenerated ${conversations.length} conversation(s)")
    
    conversations.foreach { conv =>
      println(s"\nConversation (${conv.messages.length} messages):")
      
      // Find where evidence appears
      val evidenceIndices = conv.messages.zipWithIndex.filter { case (msg, _) =>
        core.message_evidences.exists(e => msg.text.contains(e.text.take(50)))
      }.map(_._2)
      
      println(s"Evidence appears at message(s): ${evidenceIndices.mkString(", ")}")
      
      // Show a few messages around the evidence
      if (evidenceIndices.nonEmpty) {
        val idx = evidenceIndices.head
        val start = Math.max(0, idx - 2)
        val end = Math.min(conv.messages.length, idx + 3)
        
        println(s"\nMessages ${start+1} to ${end}:")
        conv.messages.slice(start, end).zipWithIndex.foreach { case (msg, i) =>
          val marker = if (i + start == idx) " <-- EVIDENCE" else ""
          println(s"  ${i+start+1}. [${msg.speaker}]: ${msg.text.take(100)}...${marker}")
        }
      }
      
      // Show where the question would be asked
      println(s"\nLater in conversation, user would ask: '${core.question}'")
      println("Good response should follow this rubric:")
      println(core.answer.split("\n").map("  " + _).mkString("\n"))
    }
  }
  
  def testAll(generator: ImplicitConnectionEvidenceGenerator, person: Personas.Person): Unit = {
    testUseCases(generator, person)
    println("\n" + "="*80)
    testCore(generator, person)
    println("\n" + "="*80)
    testConversation(generator, person)
  }
}