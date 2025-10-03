package com.salesforce.crmmembench.questions.evidence

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * A composite evidence generator that combines evidence items from multiple generators.
 * This generator doesn't generate new evidence; it loads and combines evidence from
 * existing generators.
 * 
 * @param generators List of evidence generators to combine
 * @param name Optional name for this composite generator
 */
class CompositeEvidenceGenerator(
  val generators: List[EvidenceGenerator],
  name: String = "Composite"
) extends EvidenceGenerator {
  
  require(generators.nonEmpty, "CompositeEvidenceGenerator requires at least one generator")
  
  // Use a simple config that just provides the combined evidence count
  override def config: EvidenceConfig = CompositeEvidenceConfig(
    generators.map(_.config.evidenceCount).sum
  )
  
  override def getEvidenceTypeName: String = name
  
  /**
   * Load evidence items from all configured generators and combine them.
   * Evidence items are loaded in the order generators were provided.
   */
  override def loadEvidenceItems(): List[EvidenceItem] = {
    println(s"Loading evidence from ${generators.length} generators...")
    
    val allItems = generators.flatMap { generator =>
      try {
        val items = generator.loadEvidenceItems()
        println(s"  ✓ Loaded ${items.length} items from ${generator.getEvidenceTypeName}")
        items
      } catch {
        case e: Exception =>
          println(s"  ✗ Failed to load from ${generator.getEvidenceTypeName}: ${e.getMessage}")
          List.empty
      }
    }
    
    println(s"Total: ${allItems.length} evidence items loaded")
    allItems
  }
  
  /**
   * Composite generator doesn't generate evidence, only combines existing.
   * This method will load and display statistics about the combined evidence.
   */
  override def generateEvidence(): Unit = {
    println("="*80)
    println(s"COMPOSITE EVIDENCE GENERATOR - ${name.toUpperCase}")
    println("="*80)
    println()
    println(s"Combining evidence from ${generators.length} generators:")
    generators.foreach { gen =>
      println(s"  - ${gen.getEvidenceTypeName} (count: ${gen.config.evidenceCount})")
    }
    println()
    
    val items = loadEvidenceItems()
    
    println("\n" + "="*40)
    println("COMBINED EVIDENCE STATISTICS")
    println("="*40)
    println(s"Total evidence items: ${items.length}")
    
    // Group by persona if available
    val byPersona = items.groupBy(_.personId.getOrElse("unknown"))
    println(s"Unique personas: ${byPersona.size}")
    
    // Show distribution by source generator (approximate by counting)
    var offset = 0
    generators.foreach { gen =>
      val genItems = gen.loadEvidenceItems()
      val count = genItems.length
      println(s"  ${gen.getEvidenceTypeName}: ${count} items")
      offset += count
    }
    
    // Sample items
    if (items.nonEmpty) {
      println("\n📝 Sample combined evidence items (first 5):")
      items.take(5).foreach { item =>
        println(s"  - Q: ${item.question.take(60)}...")
        println(s"    A: ${item.answer.take(40)}...")
      }
    }
  }
  
  // These methods are required by EvidenceGenerator but not used for composite
  override def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    UseCaseSummaryPromptParts(
      evidenceTypeDescription = s"Composite evidence from ${generators.length} generators",
      coreTaskDescription = "Combined evidence testing",
      evidenceDistributionDescription = "Evidence combined from multiple sources",
      exampleUseCase = EvidenceUseCase(
        id = 0,
        category = "Composite",
        scenario_description = "Combined from multiple generators"
      ),
      additionalRequirements = None
    )
  }
  
  override def getEvidenceCorePromptParts(
    person: Personas.Person,
    useCase: EvidenceUseCase
  ): EvidenceCorePromptParts = {
    EvidenceCorePromptParts(
      scenarioType = "Composite",
      taskDescription = "Combined evidence",
      specificInstructions = "",
      fieldDefinitions = "",
      additionalGuidance = None
    )
  }
  
  override def getConversationPromptParts(
    person: Personas.Person,
    useCase: EvidenceUseCase,
    evidenceCore: GeneratedEvidenceCore
  ): ConversationPromptParts = {
    ConversationPromptParts(
      evidenceType = "Composite",
      scenarioDescription = "Combined evidence",
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = config.evidenceCount
    )
  }
  
  override def getVerificationChecks(): List[VerificationCheck] = {
    // Use verification checks from the first generator, or none if empty
    generators.headOption.map(_.getVerificationChecks()).getOrElse(List.empty)
  }
  
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    // Use answering evaluation from the first generator
    generators.headOption.map(_.getAnsweringEvaluation()).getOrElse(DefaultAnsweringEvaluation)
  }
}

/**
 * Specific composite generator for combining LongMemEval abstention generators
 * with different count numbers (1-4).
 */
object AbstentionCompositeGenerator extends CompositeEvidenceGenerator(
  generators = (1 to 4).map { count =>
    new com.salesforce.crmmembench.questions.evidence.generators.longmemeval.LongMemEvalAbstentionGenerator(count)
  }.toList,
  name = "longmemeval_abstention_1to4"
)