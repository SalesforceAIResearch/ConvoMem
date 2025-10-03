package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators._
import com.salesforce.crmmembench.{Config, Personas}
// No need to import ConversationGenerator separately

/**
 * Quick test runners for evidence generator development.
 * Each method can be run independently to test specific phases.
 */
object QuickGeneratorTest {
  
  // Helper to get a test person
  def getTestPerson(): Personas.Person = {
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    allPersons.find(_.getPrimitiveRoleName == "Marketing_Manager")
      .getOrElse(allPersons.head)
  }
  
  /**
   * Test just the use case generation phase
   */
  def testUseCaseOnly(evidenceCount: Int = 1): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount)
    val person = getTestPerson()
    
    // Generate and display use cases for diversity testing
    // IMPORTANT: Generate at least 20 for proper diversity validation
    val useCases = generator.generateUseCases(person).take(20)
    
    println("\n" + "="*80)
    println(s"Generated ${useCases.length} Use Cases for ${person.getPrimitiveRoleName}")
    println("="*80)
    
    useCases.zipWithIndex.foreach { case (uc, i) =>
      println(s"\n${i+1}. Category: ${uc.category}")
      println(s"   Scenario: ${uc.scenario_description}")
    }
    
    // Show the prompt that was used
    if (Config.DEBUG) {
      println("\n" + "-"*80)
      println("PROMPT USED:")
      println("-"*80)
      println(generator.getUseCaseSummaryPrompt(person))
    }
  }
  
  /**
   * Test core generation with a predefined use case
   */
  def testCoreOnly(evidenceCount: Int = 1): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount)
    val person = getTestPerson()
    
    // Create a test use case
    val useCase = EvidenceUseCase(
      id = 1,
      category = "Professional Preferences",
      scenario_description = "The marketing manager discusses their preferred marketing analytics tools and later needs to recall which specific tool they mentioned for campaign tracking."
    )
    
    println("\n" + "="*80)
    println(s"Generating Evidence Core for Use Case")
    println("="*80)
    println(s"Category: ${useCase.category}")
    println(s"Scenario: ${useCase.scenario_description}")
    
    // Generate core
    val core = generator.generateEvidenceCore(person, useCase)
    
    println(s"\nGenerated Core:")
    println(s"Question: ${core.question}")
    println(s"Answer: ${core.answer}")
    println(s"\nEvidence Messages:")
    core.message_evidences.foreach { msg =>
      println(s"  [${msg.speaker}]: ${msg.text}")
    }
    
    // Show the prompt
    if (Config.DEBUG) {
      println("\n" + "-"*80)
      println("PROMPT USED:")
      println("-"*80)
      println(generator.getEvidenceCorePrompt(person, useCase))
    }
  }
  
  /**
   * Test conversation generation with predefined core
   */
  def testConversationOnly(evidenceCount: Int = 1): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount)
    val person = getTestPerson()
    
    // Create test data
    val useCase = EvidenceUseCase(
      id = 1,
      category = "Professional Tools",
      scenario_description = "Marketing manager shares their go-to tool for tracking campaign performance."
    )
    
    val core = GeneratedEvidenceCore(
      question = "What tool do I use for tracking campaign performance?",
      answer = "HubSpot",
      message_evidences = List(
        Message("User", "For tracking campaign performance, I always use HubSpot. It gives me the best insights into conversion rates and ROI.")
      )
    )
    
    println("\n" + "="*80)
    println("Generating Conversations from Core")
    println("="*80)
    println(s"Question: ${core.question}")
    println(s"Answer: ${core.answer}")
    
    // Generate conversations
    // Generate conversations (count is determined by the generator's config)
    val conversations = generator.generateConversationsFromCore(person, useCase, core)
    
    conversations.zipWithIndex.foreach { case (conv, i) =>
      println(s"\n--- Conversation ${i+1} (${conv.messages.length} messages) ---")
      
      // Show first few and last few messages
      val toShow = 3
      conv.messages.take(toShow).foreach { msg =>
        println(s"[${msg.speaker}]: ${msg.text.take(80)}${if(msg.text.length > 80) "..." else ""}")
      }
      
      if (conv.messages.length > toShow * 2) {
        println(s"... (${conv.messages.length - toShow * 2} messages omitted) ...")
        conv.messages.takeRight(toShow).foreach { msg =>
          println(s"[${msg.speaker}]: ${msg.text.take(80)}${if(msg.text.length > 80) "..." else ""}")
        }
      } else if (conv.messages.length > toShow) {
        conv.messages.drop(toShow).foreach { msg =>
          println(s"[${msg.speaker}]: ${msg.text.take(80)}${if(msg.text.length > 80) "..." else ""}")
        }
      }
    }
  }
  
  /**
   * Test with custom prompts - modify and experiment
   */
  def testWithModifiedPrompts(): Unit = {
    // Create a custom generator with modified prompt parts
    val generator = new UserFactsEvidenceGenerator(1) {
      override def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
        val original = super.getUseCaseSummaryPromptParts(person)
        // Modify the prompt parts here to experiment
        original.copy(
          additionalRequirements = Some(
            """Focus on very specific, memorable facts like:
              |- Exact product names or model numbers
              |- Specific dates or deadlines
              |- Precise quantities or measurements
              |- Unique personal preferences with clear details""".stripMargin
          )
        )
      }
    }
    
    val person = getTestPerson()
    // Use the custom generator
    val useCases = generator.generateUseCases(person).take(3)
    println("\n" + "="*80)
    println(s"Generated ${useCases.length} Modified Use Cases for ${person.getPrimitiveRoleName}")
    println("="*80)
    
    useCases.zipWithIndex.foreach { case (uc, i) =>
      println(s"\n${i+1}. Category: ${uc.category}")
      println(s"   Scenario: ${uc.scenario_description}")
    }
  }
  
  /**
   * Simple demo method to show the utilities working
   */
  def runSimpleDemo(): Unit = {
    println("\n" + "="*80)
    println("EVIDENCE GENERATOR DEVELOPMENT UTILITIES DEMO")
    println("="*80)
    
    val generator = new UserFactsEvidenceGenerator(1)
    val person = getTestPerson()
    
    println(s"\nTesting with person: ${person.role_name}")
    println(s"Role: ${person.description}")
    
    // Step 1: Generate a use case
    println("\n--- STEP 1: Generating Use Case ---")
    val useCases = generator.generateUseCases(person).take(1)
    val useCase = useCases.head
    println(s"Category: ${useCase.category}")
    println(s"Scenario: ${useCase.scenario_description}")
    
    // Step 2: Generate evidence core
    println("\n--- STEP 2: Generating Evidence Core ---")
    val core = generator.generateEvidenceCore(person, useCase)
    println(s"Question: ${core.question}")
    println(s"Answer: ${core.answer}")
    println(s"Evidence Messages:")
    core.message_evidences.foreach { msg =>
      println(s"  [${msg.speaker}]: ${msg.text}")
    }
    
    // Step 3: Generate conversations
    println("\n--- STEP 3: Generating Conversations ---")
    val conversations = generator.generateConversationsFromCore(person, useCase, core)
    println(s"Generated ${conversations.length} conversation(s)")
    conversations.headOption.foreach { conv =>
      println(s"First conversation has ${conv.messages.length} messages")
      println("First 3 messages:")
      conv.messages.take(3).foreach { msg =>
        println(s"  [${msg.speaker}]: ${msg.text.take(100)}${if(msg.text.length > 100) "..." else ""}")
      }
    }
    
    println("\n" + "="*80)
    println("DEMO COMPLETE")
    println("="*80)
    println("\nYou can now modify the generator prompts and re-run to see different results!")
  }
  
  /**
   * Test use case generation with diversity validation
   */
  def testUseCaseDiversity(evidenceCount: Int = 1, generatorClass: String = "UserFacts"): Unit = {
    val generator = generatorClass match {
      case "UserFacts" => new UserFactsEvidenceGenerator(evidenceCount)
      case "Temporal" => new TemporalEvidenceGenerator(evidenceCount)
      case "Preference" => new PreferenceEvidenceGenerator(evidenceCount)
      case _ => new UserFactsEvidenceGenerator(evidenceCount)
    }
    
    val person = getTestPerson()
    
    println("\n" + "="*80)
    println(s"DIVERSITY VALIDATION TEST - ${generatorClass} Generator")
    println("="*80)
    println(s"Generating 20 use cases for: ${person.role_name}")
    println(s"Evidence count: $evidenceCount")
    
    val useCases = generator.generateUseCases(person).take(20)
    
    println(s"\nGenerated ${useCases.length} use cases:")
    println("="*80)
    
    // Group by category
    val byCategory = useCases.groupBy(_.category)
    println(s"\nCategory Distribution:")
    byCategory.foreach { case (cat, cases) =>
      println(s"  $cat: ${cases.length} cases")
    }
    
    // Display all use cases
    println("\nAll Use Cases:")
    println("-"*80)
    useCases.zipWithIndex.foreach { case (uc, i) =>
      println(s"\n${i+1}. [${uc.category}]")
      println(s"   ${uc.scenario_description}")
    }
    
    // Diversity analysis prompts
    println("\n" + "="*80)
    println("DIVERSITY CHECKLIST:")
    println("="*80)
    println("[ ] Topics vary (not all about the same subject)")
    println("[ ] Complexity varies (simple to complex scenarios)")
    println("[ ] Context varies (work, home, hobbies, relationships)")
    println("[ ] Time scales vary (if temporal)")
    println("[ ] Question types vary (if applicable)")
    println("[ ] Language patterns vary (not formulaic)")
    println("[ ] No more than 2-3 cases follow identical patterns")
    println("\nIf diversity is insufficient, modify generator prompts and re-test.")
  }
  
  /**
   * Main method with menu
   */
  def main(args: Array[String]): Unit = {
    val command = args.headOption.getOrElse("menu")
    val evidenceCount = args.lift(1).map(_.toInt).getOrElse(1)
    
    // Note: Config.DEBUG is a val and cannot be modified at runtime
    // To see debug output, set Config.DEBUG = true in the Config object
    
    val generatorClass = args.lift(2).getOrElse("UserFacts")
    
    command match {
      case "demo" => runSimpleDemo()
      case "use-case" => testUseCaseOnly(evidenceCount)
      case "diversity" => testUseCaseDiversity(evidenceCount, generatorClass)
      case "core" => testCoreOnly(evidenceCount)
      case "conversation" => testConversationOnly(evidenceCount)
      case "modified" => testWithModifiedPrompts()
      case _ =>
        println("""
          |Usage: QuickGeneratorTest <command> [evidenceCount] [generatorClass]
          |
          |Commands:
          |  demo         - Run a simple demo of all phases
          |  use-case     - Test only use case generation (shows 20 for diversity)
          |  diversity    - Full diversity validation test (20 use cases with analysis)
          |  core         - Test only evidence core generation
          |  conversation - Test only conversation generation
          |  modified     - Test with modified prompts
          |
          |Generator Classes (for diversity command):
          |  UserFacts, Temporal, Preference
          |
          |Examples:
          |  QuickGeneratorTest use-case 2
          |  QuickGeneratorTest diversity 1 Temporal
          """.stripMargin)
    }
  }
}