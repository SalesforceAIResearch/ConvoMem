package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators._
import com.salesforce.crmmembench.{Config, Personas}
import io.circe.generic.auto._
import io.circe.syntax._

/**
 * Utility class for iterative development and testing of Evidence Generators.
 * Allows running and inspecting each phase of generation independently.
 */
object IterativeGeneratorDevelopment {
  
  // Helper to temporarily enable debug mode
  def withDebug[T](action: => T): T = {
    val originalDebug = Config.DEBUG
    try {
      // Can't modify Config.DEBUG directly as it's a val
      // Instead, we'll just assume debug mode is on
      action
    } finally {
      // Nothing to restore since we can't modify it
    }
  }
  
  /**
   * Run Phase 1: Generate use cases and print them for inspection
   */
  def testUseCaseGeneration(
    generator: EvidenceGenerator, 
    person: Personas.Person,
    count: Int = 5
  ): List[EvidenceUseCase] = {
    withDebug {
      println("\n" + "="*80)
      println(s"PHASE 1: USE CASE GENERATION for ${person.getPrimitiveRoleName}")
      println("="*80)
      
      // Note: generateUseCases doesn't take a count parameter, it uses the generator's internal config
      val useCases = generator.generateUseCases(person)
      
      println(s"\nGenerated ${useCases.length} use cases:")
      useCases.zipWithIndex.foreach { case (useCase, idx) =>
        println(s"\n--- Use Case ${idx + 1} ---")
        println(s"Category: ${useCase.category}")
        println(s"Scenario: ${useCase.scenario_description}")
      }
      
      println("\n" + "-"*80)
      println("JSON representation:")
      println(EvidenceUseCases(useCases).asJson.spaces2)
      
      useCases
    }
  }
  
  /**
   * Run Phase 2: Generate evidence cores for given use cases
   */
  def testEvidenceCoreGeneration(
    generator: EvidenceGenerator,
    person: Personas.Person,
    useCases: List[EvidenceUseCase]
  ): List[(EvidenceUseCase, GeneratedEvidenceCore)] = {
    withDebug {
      println("\n" + "="*80)
      println(s"PHASE 2: EVIDENCE CORE GENERATION for ${person.getPrimitiveRoleName}")
      println("="*80)
      
      val results = useCases.map { useCase =>
        println(s"\n--- Generating Core for Use Case ---")
        println(s"Category: ${useCase.category}")
        println(s"Scenario: ${useCase.scenario_description}")
        
        val core = generator.generateEvidenceCore(person, useCase)
        
        println(s"\nGenerated Core:")
        println(s"Question: ${core.question}")
        println(s"Answer: ${core.answer}")
        println(s"Evidence Messages (${core.message_evidences.length}):")
        core.message_evidences.zipWithIndex.foreach { case (msg, idx) =>
          println(s"  ${idx + 1}. [${msg.speaker}]: ${msg.text}")
        }
        
        (useCase, core)
      }
      
      println("\n" + "-"*80)
      println("JSON representation of first core:")
      results.headOption.foreach { case (_, core) =>
        println(core.asJson.spaces2)
      }
      
      results
    }
  }
  
  /**
   * Run Phase 3: Generate conversations from evidence cores
   */
  def testConversationGeneration(
    generator: EvidenceGenerator,
    person: Personas.Person,
    coresWithUseCases: List[(EvidenceUseCase, GeneratedEvidenceCore)],
    conversationsPerCore: Int = 2
  ): List[EvidenceItem] = {
    withDebug {
      println("\n" + "="*80)
      println(s"PHASE 3: CONVERSATION GENERATION for ${person.getPrimitiveRoleName}")
      println("="*80)
      
      val results = coresWithUseCases.flatMap { case (useCase, core) =>
        println(s"\n--- Generating Conversations for Core ---")
        println(s"Question: ${core.question}")
        println(s"Answer: ${core.answer}")
        
        // generateConversationsFromCore doesn't take conversationsPerCore parameter
        // The number of conversations is determined by the generator's config
        val conversations = generator.generateConversationsFromCore(
          person, useCase, core
        )
        
        conversations.zipWithIndex.foreach { case (conv, idx) =>
          println(s"\n  Conversation ${idx + 1} (${conv.messages.length} messages):")
          // Print first and last 3 messages
          val messages = conv.messages
          if (messages.length <= 6) {
            messages.foreach { msg =>
              println(s"    [${msg.speaker}]: ${msg.text.take(100)}${if (msg.text.length > 100) "..." else ""}")
            }
          } else {
            messages.take(3).foreach { msg =>
              println(s"    [${msg.speaker}]: ${msg.text.take(100)}${if (msg.text.length > 100) "..." else ""}")
            }
            println(s"    ... (${messages.length - 6} messages omitted) ...")
            messages.takeRight(3).foreach { msg =>
              println(s"    [${msg.speaker}]: ${msg.text.take(100)}${if (msg.text.length > 100) "..." else ""}")
            }
          }
        }
        
        // Create evidence items (without verification for quick testing)
        val evidenceItem = EvidenceItem(
          question = core.question,
          answer = core.answer,
          message_evidences = core.message_evidences,
          conversations = conversations,
          category = useCase.category,
          scenario_description = Some(useCase.scenario_description),
          personId = Some(person.id)
        )
        
        Some(evidenceItem)
      }
      
      results
    }
  }
  
  /**
   * Run all phases sequentially with inspection at each step
   */
  def testFullPipeline(
    generator: EvidenceGenerator,
    person: Personas.Person,
    useCaseCount: Int = 2,
    conversationsPerCore: Int = 2
  ): List[EvidenceItem] = {
    println("\n" + "="*80)
    println("FULL PIPELINE TEST")
    println("="*80)
    
    // Phase 1: Use Cases
    val useCases = testUseCaseGeneration(generator, person, useCaseCount)
    
    println("\n\nPress Enter to continue to Phase 2...")
    scala.io.StdIn.readLine()
    
    // Phase 2: Evidence Cores
    val coresWithUseCases = testEvidenceCoreGeneration(generator, person, useCases)
    
    println("\n\nPress Enter to continue to Phase 3...")
    scala.io.StdIn.readLine()
    
    // Phase 3: Conversations
    val evidenceItems = testConversationGeneration(generator, person, coresWithUseCases, conversationsPerCore)
    
    println(s"\n\nGenerated ${evidenceItems.length} complete evidence items")
    evidenceItems
  }
  
  /**
   * Quick test runner for a specific generator
   */
  def quickTest(generatorType: String = "user_facts", evidenceCount: Int = 1): Unit = {
    val generator = generatorType.toLowerCase match {
      case "user_facts" => new UserFactsEvidenceGenerator(evidenceCount)
      case "assistant_facts" => new AssistantFactsEvidenceGenerator(evidenceCount)
      case "changing" => new ChangingEvidenceGenerator(evidenceCount)
      case "abstention" => new AbstentionEvidenceGenerator(evidenceCount)
      case "preference" => new PreferenceEvidenceGenerator(evidenceCount)
      case _ => throw new IllegalArgumentException(s"Unknown generator type: $generatorType")
    }
    
    // Load personas from the enriched backgrounds directory
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = allPersons.find(_.getPrimitiveRoleName == "Marketing_Manager")
      .getOrElse(allPersons.head)
    
    testFullPipeline(generator, person)
  }
  
  /**
   * Main method for standalone testing
   */
  def main(args: Array[String]): Unit = {
    val generatorType = args.headOption.getOrElse("user_facts")
    val evidenceCount = args.lift(1).map(_.toInt).getOrElse(1)
    
    println(s"Testing $generatorType generator with $evidenceCount evidence...")
    quickTest(generatorType, evidenceCount)
  }
}