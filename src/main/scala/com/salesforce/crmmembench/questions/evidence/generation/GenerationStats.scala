package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.{BaseStats, StatsUtils, CostTracker}
import com.salesforce.crmmembench.questions.evidence.EvidenceItem
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

/**
 * Statistics tracking for evidence generation process.
 * Tracks the entire funnel from use cases to final evidence items.
 */
case class GenerationStats(
  totalPeople: Int,
  totalUseCases: Int,
  peopleCompleted: AtomicInteger,
  useCasesCompleted: AtomicInteger,
  evidenceCoresGenerated: AtomicInteger,
  conversationsGenerated: AtomicInteger,
  evidenceItemsCompleted: AtomicInteger,
  filesGenerated: AtomicInteger,
  verificationAttempts: AtomicInteger = new AtomicInteger(0),
  verificationPasses: AtomicInteger = new AtomicInteger(0),
  verificationFailures: AtomicInteger = new AtomicInteger(0),
  verificationCheckStats: TrieMap[String, (AtomicInteger, AtomicInteger)] = 
    TrieMap.empty,  // Map of check name -> (attempts, passes)
  abandonedEvidenceCores: AtomicInteger = new AtomicInteger(0),
  totalRetryAttempts: AtomicInteger = new AtomicInteger(0),  // Total attempts across all successful evidence
  successfulEvidenceRetries: AtomicInteger = new AtomicInteger(0),  // Number of successful evidence that required retries
  evidenceCount: Int = 1,  // Default evidence count for stats display
  useCaseModelStats: TrieMap[String, AtomicInteger] = TrieMap.empty,  // Map of model name -> count for use cases
  coreModelStats: TrieMap[String, AtomicInteger] = TrieMap.empty,  // Map of model name -> count for cores
  conversationModelStats: TrieMap[String, AtomicInteger] = TrieMap.empty,  // Map of model name -> count for conversations
  conversationVerificationStats: TrieMap[String, (AtomicInteger, AtomicInteger)] = TrieMap.empty,  // Map of model name -> (attempts, passes) for conversation verification
  conversationValidationAttempts: AtomicInteger = new AtomicInteger(0),  // Total attempts at validating conversations
  conversationValidationFailures: AtomicInteger = new AtomicInteger(0),  // Total failures in conversation validation
  conversationValidationFailureCategories: TrieMap[String, AtomicInteger] = TrieMap.empty,  // Map of failure category -> count
  peopleTimedOut: AtomicInteger = new AtomicInteger(0),  // Number of people that timed out during processing
  useCasesTimedOut: AtomicInteger = new AtomicInteger(0)  // Number of use cases that timed out during processing
) extends BaseStats {
  
  /**
   * Track validation failure categories.
   */
  def trackValidationFailureCategories(categories: Set[String]): Unit = {
    categories.foreach { category =>
      conversationValidationFailureCategories.getOrElseUpdate(category, new AtomicInteger(0)).incrementAndGet()
    }
  }
  
  /**
   * Track model usage statistics from an evidence item.
   */
  def trackEvidenceItemModels(evidenceItem: EvidenceItem): Unit = {
    // Track use case model
    evidenceItem.use_case_model_name.foreach { modelName =>
      useCaseModelStats.getOrElseUpdate(modelName, new AtomicInteger(0)).incrementAndGet()
    }
    
    // Track core model
    evidenceItem.core_model_name.foreach { modelName =>
      coreModelStats.getOrElseUpdate(modelName, new AtomicInteger(0)).incrementAndGet()
    }
    
    // Track conversation models (can have multiple conversations)
    evidenceItem.conversations.foreach { conversation =>
      conversation.model_name.foreach { modelName =>
        conversationModelStats.getOrElseUpdate(modelName, new AtomicInteger(0)).incrementAndGet()
      }
    }
  }
  


  def getStatsString: String = getStatsString(evidenceCount)
  
  def getStatsString(evidenceCount: Int): String = {
    val evidencePerMinute = ratePerMinute(evidenceItemsCompleted.get())
    val coresPerSecond = ratePerSecond(evidenceCoresGenerated.get())
    val conversationsPerSecond = ratePerSecond(conversationsGenerated.get())
    val useCaseProgressPercent = progressPercentage(useCasesCompleted.get(), totalUseCases)
    val verificationPassRate = if (verificationAttempts.get() > 0) {
      f"${(verificationPasses.get().toDouble / verificationAttempts.get() * 100)}%.1f%%"
    } else "0.0%"
    val peopleProgressPercent = progressPercentage(peopleCompleted.get(), totalPeople)

    // Calculate average retry attempts
    val avgRetries = if (evidenceItemsCompleted.get() > 0) {
      f"${totalRetryAttempts.get().toDouble / evidenceItemsCompleted.get()}%.1f"
    } else "0.0"
    
    // Helper function to format model stats breakdown
    def formatModelStats(modelStats: TrieMap[String, AtomicInteger]): String = {
      if (modelStats.isEmpty) return ""
      
      // Calculate total from actual model counts
      val modelTotal = modelStats.values.map(_.get()).sum
      
      modelStats.toSeq.sortBy { case (_, count) => -count.get() }.map { case (modelName, count) =>
        val percentage = if (modelTotal > 0) {
          f"${(count.get().toDouble / modelTotal * 100)}%.1f%%"
        } else "0.0%"
        f"│  └─ $modelName%-20s: ${count.get()}%4d ($percentage)"
      }.mkString("\n") + "\n"
    }
    
    // Build use cases line with model breakdown
    val useCasesLine = StatsUtils.formatProgressLine("Use Cases", useCasesCompleted.get(), totalUseCases, useCaseProgressPercent) + "\n"
    val useCaseModelBreakdown = formatModelStats(useCaseModelStats)
    
    // Build cores line with model breakdown
    val coresLine = StatsUtils.formatMetricLine("Evidence Cores", evidenceCoresGenerated.get(), coresPerSecond, "cores") + "\n"
    val coreModelBreakdown = formatModelStats(coreModelStats)
    
    // Build conversations line with model breakdown
    val conversationsLine = StatsUtils.formatMetricLine("Conversations", conversationsGenerated.get(), conversationsPerSecond, "conversations") + "\n"
    val conversationModelBreakdown = formatModelStats(conversationModelStats)
    
    // Calculate conversation validation failure rate
    val validationFailureRate = if (conversationValidationAttempts.get() > 0) {
      f"${(conversationValidationFailures.get().toDouble / conversationValidationAttempts.get() * 100)}%.1f%%"
    } else "0.0%"
    
    // Build validation failure breakdown with human-readable names
    val validationFailureBreakdown = if (conversationValidationFailureCategories.nonEmpty) {
      val categoryNames = Map(
        "invalid_speakers" -> "Invalid Speakers",
        "conversation_count_mismatch" -> "Count Mismatch",
        "evidence_not_found" -> "Evidence Not Found",
        "evidence_in_multiple_conversations" -> "Evidence Duplicated",
        "evidence_in_wrong_conversation" -> "Wrong Conversation Order"
      )
      
      conversationValidationFailureCategories.toSeq.sortBy { case (_, count) => -count.get() }.map { case (category, count) =>
        val displayName = categoryNames.getOrElse(category, category)
        val percentage = if (conversationValidationFailures.get() > 0) {
          f"${(count.get().toDouble / conversationValidationFailures.get() * 100)}%.1f%%"
        } else "0.0%"
        f"│  └─ $displayName%-25s: ${count.get()}%4d ($percentage)"
      }.mkString("\n") + "\n"
    } else ""
    
    // Build verification checks breakdown, sorted by total attempts (descending), then by passes (descending)
    val checkStatsLines = if (verificationCheckStats.nonEmpty) {
      verificationCheckStats.toSeq.sortBy { case (_, (attempts, passes)) =>
        (-attempts.get(), -passes.get())
      }.map { case (checkName, (attempts, passes)) =>
        val passRate = if (attempts.get() > 0) {
          f"${(passes.get().toDouble / attempts.get() * 100)}%.1f%%"
        } else "0.0%"
        f"│  └─ $checkName%-20s: ${passes.get()}/${attempts.get()} ($passRate)"
      }.mkString("\n") + "\n"
    } else ""
    
    // Build conversation verification stats
    val conversationVerificationLines = if (conversationVerificationStats.nonEmpty) {
      val totalConvVerificationAttempts = conversationVerificationStats.values.map(_._1.get()).sum
      val totalConvVerificationPasses = conversationVerificationStats.values.map(_._2.get()).sum
      val convVerificationPassRate = if (totalConvVerificationAttempts > 0) {
        f"${(totalConvVerificationPasses.toDouble / totalConvVerificationAttempts * 100)}%.1f%%"
      } else "0.0%"
      
      val convVerificationHeader = f"├─ Conversations Passed : $totalConvVerificationPasses%3d / $totalConvVerificationAttempts%3d ($convVerificationPassRate)\n"
      val convVerificationBreakdown = conversationVerificationStats.toSeq.sortBy { case (_, (attempts, passes)) =>
        (-attempts.get(), -passes.get())
      }.map { case (modelName, (attempts, passes)) =>
        val passRate = if (attempts.get() > 0) {
          f"${(passes.get().toDouble / attempts.get() * 100)}%.1f%%"
        } else "0.0%"
        f"│  └─ $modelName%-20s: ${passes.get()}/${attempts.get()} ($passRate)"
      }.mkString("\n") + "\n"
      
      convVerificationHeader + convVerificationBreakdown
    } else ""
    
    StatsUtils.formatStatsSection(s"$evidenceCount-EVIDENCE GENERATION STATISTICS",
      useCasesLine + useCaseModelBreakdown +
      coresLine + coreModelBreakdown +
      f"├─ Abandoned Cores      : ${abandonedEvidenceCores.get()}%3d (failed after all retries)\n" +
      conversationsLine + conversationModelBreakdown +
      f"├─ Validation Failures  : ${conversationValidationFailures.get()}/${conversationValidationAttempts.get()} ($validationFailureRate failed)\n" +
      validationFailureBreakdown +
      f"├─ Verification         : ${verificationPasses.get()}/${verificationAttempts.get()} passed ($verificationPassRate)\n" +
      checkStatsLines +
      conversationVerificationLines +
      StatsUtils.formatMetricLinePerMinute("Evidence Items", evidenceItemsCompleted.get(), evidencePerMinute, "evidence") + "\n" +
      f"├─ Avg Retry Attempts   : $avgRetries per successful evidence\n" +
      StatsUtils.formatProgressLine("People", peopleCompleted.get(), totalPeople, peopleProgressPercent) + "\n" +
      (if (peopleTimedOut.get() > 0) f"├─ People Timed Out     : ${peopleTimedOut.get()}%3d (after ${com.salesforce.crmmembench.Config.Generation.PERSON_PROCESSING_TIMEOUT_HOURS} hours)\n" else "") +
      (if (useCasesTimedOut.get() > 0) f"├─ Use Cases Timed Out  : ${useCasesTimedOut.get()}%3d (after ${com.salesforce.crmmembench.Config.Generation.USE_CASE_TIMEOUT_MINUTES} minutes)\n" else "") +
      f"├─ Files                : ${filesGenerated.get()}%3d JSON files written\n" +
      {
        // Add cost tracking
        val currentCost = CostTracker.getTotalCost
        val projectedCost = CostTracker.getProjectedCost(evidenceItemsCompleted.get(), totalUseCases - abandonedEvidenceCores.get())
        val hourlyRate = if (elapsed > 0) currentCost / (elapsed / 3600.0) else 0.0
        f"├─ Cost                 : $$${currentCost}%.4f spent, $$${projectedCost}%.4f projected ($$${hourlyRate}%.2f/hour)\n"
      } +
      {
        // Add estimated time to completion
        val completedEvidenceItems = evidenceItemsCompleted.get()
        val remainingEvidenceItems = (totalUseCases - abandonedEvidenceCores.get()) - completedEvidenceItems
        val estimatedTimeToCompletion = if (completedEvidenceItems > 0 && elapsed > 0) {
          val timePerEvidenceItem = elapsed / completedEvidenceItems
          val remainingSeconds = (remainingEvidenceItems * timePerEvidenceItem).toInt
          val hours = remainingSeconds / 3600
          val minutes = (remainingSeconds % 3600) / 60
          f"├─ Est. Time to Complete: ${hours}%02d:${minutes}%02d\n"
        } else {
          "├─ Est. Time to Complete: --:--\n"
        }
        estimatedTimeToCompletion
      } +
      StatsUtils.formatRuntimeLine(elapsed)
    )
  }
}

/**
 * Factory for creating GenerationStats instances.
 */
object GenerationStats {
  def create(totalPeople: Int, totalUseCases: Int, evidenceCount: Int = 1): GenerationStats = {
    GenerationStats(
      totalPeople = totalPeople,
      totalUseCases = totalUseCases,
      peopleCompleted = new AtomicInteger(0),
      useCasesCompleted = new AtomicInteger(0),
      evidenceCoresGenerated = new AtomicInteger(0),
      conversationsGenerated = new AtomicInteger(0),
      evidenceItemsCompleted = new AtomicInteger(0),
      filesGenerated = new AtomicInteger(0),
      evidenceCount = evidenceCount
    )
  }
}