package com.salesforce.crmmembench.questions.evidence.generators.longmemeval

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.{Personas, Utils, Config}
import scala.collection.concurrent.TrieMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base trait for all LongMemEval generators.
 * These are read-only generators that load pre-converted data from LongMemEval dataset.
 * They extend EvidenceGenerator to work with the existing framework but don't generate new data.
 */
trait LongMemEvalReadOnlyGenerator extends EvidenceGenerator {
  
  /**
   * Override generateEvidence to run verification checks on loaded data.
   * This simulates the normal generation flow but with pre-existing data.
   */
  override def generateEvidence(): Unit = {
    println("="*80)
    println(s"${getEvidenceTypeName.toUpperCase} - VERIFICATION MODE")
    println("="*80)
    println()
    println("This generator reads pre-converted LongMemEval data and runs verification checks.")
    println(s"Data location: ${config.resourcePath}")
    
    try {
      val items = loadEvidenceItems()
      println(s"✅ Successfully loaded ${items.length} evidence items")
      
      // Limit items for short mode
      val itemsToVerify = if (runShort) items.take(10) else items
      
      if (runShort) {
        println(s"🚀 Running in SHORT mode: verifying first 10 items only")
      }
      
      // Print basic statistics
      val itemsByPerson = itemsToVerify.groupBy(_.personId.getOrElse("unknown"))
      println(s"📊 Found ${itemsByPerson.size} unique personas")
      
      // Additional statistics for subclasses
      printAdditionalStatistics(itemsToVerify)
      
      // Sample first few items
      if (itemsToVerify.nonEmpty) {
        println("\n📝 Sample evidence items:")
        itemsToVerify.take(3).foreach { item =>
          println(s"  - Question: ${item.question.take(60)}...")
          println(s"    Answer: ${item.answer.take(40)}...")
          println(s"    Conversations: ${item.conversations.length}")
        }
      }
      
      // Run verification checks if enabled
      val verificationChecks = getVerificationChecks()
      if (verificationChecks.nonEmpty) {
        println(s"\n🔍 Running ${verificationChecks.length} verification checks on ${itemsToVerify.length} items...")
        runVerificationOnLoadedData(itemsToVerify, verificationChecks)
      } else {
        println("\n📌 No verification checks configured for this evidence type.")
      }
      
    } catch {
      case e: Exception =>
        println(s"❌ Error loading evidence: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  /**
   * Run verification checks on loaded data and display statistics.
   * Mimics the verification flow of normal generators.
   */
  def runVerificationOnLoadedData(items: List[EvidenceItem], checks: List[VerificationCheck]): Unit = {
    // Create stats tracking
    val verificationCheckStats: TrieMap[String, (AtomicInteger, AtomicInteger)] = TrieMap.empty
    val startTime = System.currentTimeMillis()
    
    // Track overall pass/fail
    val totalPassed = new AtomicInteger(0)
    val totalFailed = new AtomicInteger(0)
    
    // Run verification in parallel for efficiency
    val threadCount = if (runShort) 2 else Config.Threading.USE_CASE_PROCESSING_THREADS
    val results = Utils.parallelMap(items, threadCount = threadCount) { item =>
      try {
        // Run all verification checks for this item
        val checkResults = checks.map { check =>
          check.verify(item, Some(verificationCheckStats), getAnsweringEvaluation())
        }
        
        // Check if all passed
        val allPassed = checkResults.forall(_.passed)
        if (allPassed) {
          totalPassed.incrementAndGet()
        } else {
          totalFailed.incrementAndGet()
        }
        
        Some((item, checkResults, allPassed))
      } catch {
        case e: Exception =>
          println(s"⚠️ Error verifying item: ${e.getMessage}")
          totalFailed.incrementAndGet()
          None
      }
    }.flatten
    
    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
    
    // Display results
    println("\n" + "="*80)
    println("VERIFICATION RESULTS")
    println("="*80)
    
    // Overall statistics
    val totalItems = totalPassed.get() + totalFailed.get()
    val passRate = if (totalItems > 0) {
      f"${(totalPassed.get().toDouble / totalItems * 100)}%.1f%%"
    } else "0.0%"
    
    println(f"├─ Overall              : ${totalPassed.get()}/${totalItems} passed ($passRate)")
    
    // Per-check statistics
    if (verificationCheckStats.nonEmpty) {
      println("├─ By Check Type:")
      verificationCheckStats.toSeq.sortBy(_._1).foreach { case (checkName, (attempts, passes)) =>
        val checkPassRate = if (attempts.get() > 0) {
          f"${(passes.get().toDouble / attempts.get() * 100)}%.1f%%"
        } else "0.0%"
        println(f"│  └─ $checkName%-20s: ${passes.get()}/${attempts.get()} ($checkPassRate)")
      }
    }
    
    println(f"├─ Runtime              : ${elapsed}%.2f seconds")
    println(f"└─ Items/second         : ${totalItems / elapsed}%.2f")
    
    // Show failed examples if in debug mode
    if (Config.DEBUG && totalFailed.get() > 0) {
      println("\n📋 Failed Items (first 5):")
      results.filter(!_._3).take(5).foreach { case (item, checkResults, _) =>
        println(s"\n  Question: ${item.question.take(80)}...")
        checkResults.filterNot(_.passed).foreach { result =>
          println(s"    ❌ ${result.checkName}: ${result.details}")
        }
      }
    }
  }
  
  /**
   * Hook for subclasses to print additional statistics.
   */
  def printAdditionalStatistics(items: List[EvidenceItem]): Unit = {}

  /**
   * These methods are required by EvidenceGenerator but not used for read-only generators.
   * They return dummy values since we never generate new evidence.
   */
  override def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    UseCaseSummaryPromptParts(
      evidenceTypeDescription = s"LongMemEval ${getEvidenceTypeName} - imported data",
      coreTaskDescription = "Testing memory recall on imported LongMemEval data",
      evidenceDistributionDescription = "Evidence is pre-distributed from LongMemEval dataset",
      exampleUseCase = EvidenceUseCase(
        id = 0,
        category = getEvidenceTypeName,
        scenario_description = "Imported from LongMemEval dataset"
      ),
      additionalRequirements = None
    )
  }
  
  override def getEvidenceCorePromptParts(
    person: Personas.Person,
    useCase: EvidenceUseCase
  ): EvidenceCorePromptParts = {
    EvidenceCorePromptParts(
      scenarioType = getEvidenceTypeName,
      taskDescription = s"LongMemEval ${getEvidenceTypeName} - imported data",
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
      evidenceType = getEvidenceTypeName,
      scenarioDescription = s"LongMemEval ${getEvidenceTypeName} evidence",
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = config.evidenceCount
    )
  }
}