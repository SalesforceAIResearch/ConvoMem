package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Interactive test runner for UserFactsEvidenceGenerator development.
 * Allows testing each phase independently with immediate feedback.
 */
object TestUserFactsGenerator {
  
  def main(args: Array[String]): Unit = {
    // Configuration
    val evidenceCount = args.headOption.map(_.toInt).getOrElse(1)
    val runShort = args.contains("--short")
    
    println(s"""
    |================================================================================
    |INTERACTIVE USER FACTS GENERATOR TEST
    |================================================================================
    |Evidence Count: $evidenceCount
    |Run Short: $runShort
    |
    |Commands:
    |  1 - Test Use Case Generation
    |  2 - Test Evidence Core Generation (requires use cases)
    |  3 - Test Conversation Generation (requires cores)
    |  a - Run all phases
    |  u - Generate new use cases
    |  c - Generate cores for existing use cases
    |  v - Generate conversations for existing cores
    |  q - Quit
    |================================================================================
    """.stripMargin)
    
    // Create generator with custom config for short mode
    val generator = if (runShort) {
      new UserFactsEvidenceGenerator(evidenceCount) {
        override val runShort: Boolean = true
      }
    } else {
      new UserFactsEvidenceGenerator(evidenceCount)
    }
    
    // Load personas from the enriched backgrounds directory
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val person = allPersons.find(_.getPrimitiveRoleName == "Sales_Representative")
      .getOrElse(allPersons.head)
    
    var useCases: Option[List[EvidenceUseCase]] = None
    var cores: Option[List[(EvidenceUseCase, GeneratedEvidenceCore)]] = None
    var evidenceItems: Option[List[EvidenceItem]] = None
    
    var continue = true
    while (continue) {
      print("\nCommand: ")
      val command = scala.io.StdIn.readLine().trim.toLowerCase
      
      command match {
        case "1" | "u" =>
          // Test use case generation
          println("\nHow many use cases to generate? (default: 3): ")
          val count = scala.io.StdIn.readLine().trim match {
            case "" => 3
            case n => n.toInt
          }
          useCases = Some(IterativeGeneratorDevelopment.testUseCaseGeneration(generator, person, count))
          
        case "2" | "c" =>
          // Test evidence core generation
          useCases match {
            case Some(cases) =>
              println(s"\nGenerating cores for ${cases.length} use cases...")
              cores = Some(IterativeGeneratorDevelopment.testEvidenceCoreGeneration(generator, person, cases))
            case None =>
              println("No use cases available. Generate use cases first (command: 1)")
          }
          
        case "3" | "v" =>
          // Test conversation generation
          cores match {
            case Some(coreList) =>
              println("\nHow many conversations per core? (default: 2): ")
              val convCount = scala.io.StdIn.readLine().trim match {
                case "" => 2
                case n => n.toInt
              }
              evidenceItems = Some(IterativeGeneratorDevelopment.testConversationGeneration(
                generator, person, coreList, convCount
              ))
            case None =>
              println("No cores available. Generate cores first (command: 2)")
          }
          
        case "a" =>
          // Run all phases
          println("\nRunning full pipeline...")
          println("Use cases to generate (default: 2): ")
          val ucCount = scala.io.StdIn.readLine().trim match {
            case "" => 2
            case n => n.toInt
          }
          println("Conversations per core (default: 2): ")
          val convCount = scala.io.StdIn.readLine().trim match {
            case "" => 2
            case n => n.toInt
          }
          
          val items = IterativeGeneratorDevelopment.testFullPipeline(
            generator, person, ucCount, convCount
          )
          evidenceItems = Some(items)
          
        case "q" =>
          continue = false
          println("Exiting...")
          
        case _ =>
          println("Unknown command. Use 1, 2, 3, a, u, c, v, or q")
      }
    }
  }
  
  /**
   * Quick single-phase test methods
   */
  object QuickTests {
    
    def testUseCasePrompt(): Unit = {
      val generator = new UserFactsEvidenceGenerator(1)
      val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
      val person = allPersons.head
      
      println("USE CASE GENERATION PROMPT:")
      println("="*80)
      println(generator.getUseCaseSummaryPrompt(person))
    }
    
    def testEvidenceCorePrompt(): Unit = {
      val generator = new UserFactsEvidenceGenerator(1)
      val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
      val person = allPersons.head
      val useCase = EvidenceUseCase(
        id = 1,
        category = "Test Category",
        scenario_description = "Test scenario description"
      )
      
      println("EVIDENCE CORE GENERATION PROMPT:")
      println("="*80)
      println(generator.getEvidenceCorePrompt(person, useCase))
    }
    
    def testConversationPrompt(): Unit = {
      val generator = new UserFactsEvidenceGenerator(1)
      val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
      val person = allPersons.head
      val useCase = EvidenceUseCase(
        id = 1,
        category = "Test Category",
        scenario_description = "Test scenario description"
      )
      val core = GeneratedEvidenceCore(
        question = "What is my favorite color?",
        answer = "Blue",
        message_evidences = List(Message("User", "My favorite color is blue."))
      )
      
      val promptParts = generator.getConversationPromptParts(person, useCase, core)
      val conversationGen = new com.salesforce.crmmembench.conversations.ConversationGenerator()
      
      println("CONVERSATION GENERATION PROMPT:")
      println("="*80)
      println(conversationGen.getFullConversationPrompt(person, promptParts))
    }
  }
}