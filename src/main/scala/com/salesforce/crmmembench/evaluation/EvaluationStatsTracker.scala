package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.{Config, StatsProvider, CostTracker, Utils}
import com.salesforce.crmmembench.evaluation.memory.AnswerResult
import scala.collection.concurrent.TrieMap
import java.util.concurrent.atomic.AtomicInteger
import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.io.Source
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Unified evaluation statistics tracker.
 * Tracks evidence items processed and test cases completed per context size.
 */
class EvaluationStatsTracker(
  testCases: List[TestCase],
  caseType: String,
  memorySystem: String,
  testCaseGeneratorType: String,
  overrideCSV: Boolean = false
) extends StatsProvider {
  
  val startTime = System.currentTimeMillis()
  
  // Track evidence items processed per minute (needs to be volatile for thread safety)
  @volatile var lastMinuteCheck = System.currentTimeMillis()
  @volatile var lastMinuteCount = 0
  
  // Simple internal tracking
  case class ContextStats(
    var correctAnswers: Int = 0,
    var totalProcessed: Int = 0,
    var completedTestCases: Int = 0,
    val totalTestCases: Int,
    val responseTimes: scala.collection.mutable.ArrayBuffer[Long] = scala.collection.mutable.ArrayBuffer[Long](),
    val inputTokens: scala.collection.mutable.ArrayBuffer[Int] = scala.collection.mutable.ArrayBuffer[Int](),
    val outputTokens: scala.collection.mutable.ArrayBuffer[Int] = scala.collection.mutable.ArrayBuffer[Int](),
    val costs: scala.collection.mutable.ArrayBuffer[Double] = scala.collection.mutable.ArrayBuffer[Double](),
    val cachedInputTokens: scala.collection.mutable.ArrayBuffer[Int] = scala.collection.mutable.ArrayBuffer[Int]()
  ) {
    // Calculate total cost for this context size
    def totalCost: Double = costs.sum
  }
  
  // Extract info from test cases
  val allEvidenceItems = testCases.flatMap(_.evidenceItems)
  val totalUniqueEvidenceItems = allEvidenceItems.distinct.size
  val contextSizes = testCases.map(_.conversationCount).distinct.sorted
  
  // Calculate total evidence items across all test cases
  val totalEvidenceItemsInTestCases = testCases.map(_.evidenceItems.size).sum
  
  // Debug logging
  if (Config.DEBUG) {
    println(s"[EvaluationStatsTracker] Total test cases: ${testCases.size}")
    println(s"[EvaluationStatsTracker] Total evidence items (with duplicates): ${allEvidenceItems.size}")
    println(s"[EvaluationStatsTracker] Unique evidence items: $totalUniqueEvidenceItems")
    println(s"[EvaluationStatsTracker] Context sizes: ${contextSizes.mkString(", ")} (count: ${contextSizes.length})")
    println(s"[EvaluationStatsTracker] Expected total: $totalUniqueEvidenceItems × ${contextSizes.length} = ${totalUniqueEvidenceItems * contextSizes.length}")
  }
  
  // Stats by context size
  val statsByContext = TrieMap[Int, ContextStats]()
  val totalProcessed = new AtomicInteger(0)
  
  // Store results for DEBUG retrieval stats
  val resultsByContext = if (Config.DEBUG) {
    Some(TrieMap[Int, List[ContextTestResult]]())
  } else None
  
  // Initialize stats
  testCases.groupBy(_.conversationCount).foreach { case (size, cases) =>
    statsByContext.put(size, ContextStats(totalTestCases = cases.length))
  }
  
  /**
   * Record an evidence item evaluation result.
   */
  def recordEvidenceResult(
    testCase: TestCase, 
    result: ContextTestResult, 
    responseTimeMs: Long,
    answerResult: Option[AnswerResult] = None
  ): Unit = {
    val contextSize = testCase.conversationCount
    
    statsByContext.get(contextSize).foreach { stats =>
      // Synchronize on the stats object to ensure thread safety
      stats.synchronized {
        stats.totalProcessed += 1
        if (result.isCorrect) stats.correctAnswers += 1
        stats.responseTimes += responseTimeMs
        
        // Add token and cost information if available
        answerResult.foreach { ar =>
          ar.inputTokens.foreach(stats.inputTokens += _)
          ar.outputTokens.foreach(stats.outputTokens += _)
          ar.cost.foreach(stats.costs += _)
          ar.cachedInputTokens.foreach(stats.cachedInputTokens += _)
        }
      }
    }
    
    // Store result for DEBUG stats
    resultsByContext.foreach { resultsMap =>
      val currentList = resultsMap.getOrElse(contextSize, List.empty)
      resultsMap.put(contextSize, result :: currentList)
    }
    
    totalProcessed.incrementAndGet()
    
    // Log the result (only if logger is initialized)
    if (EvaluationLogger.isInitialized) {
      EvaluationLogger.logResult(result, caseType, memorySystem, testCaseGeneratorType, responseTimeMs, answerResult)
    }
  }
  
  /**
   * Record test case completion.
   */
  def recordTestCaseCompleted(testCase: TestCase): Unit = {
    statsByContext.get(testCase.conversationCount).foreach { stats =>
      stats.synchronized {
        stats.completedTestCases += 1
      }
    }
  }
  
  override def flush(): Unit = {
    println(getStatsString)
    if (EvaluationLogger.isInitialized) {
      EvaluationLogger.flush()
    }
  }
  
  override def getStatsString: String = {
    val sb = new StringBuilder()
    
    sb.append("\n" + "-"*60 + "\n")
    sb.append("CURRENT EVALUATION STATISTICS\n")
    sb.append("-"*60 + "\n")
    sb.append("📊 EVALUATION RESULTS:\n")
    sb.append("📊 Context Dilution Results:\n")
    
    // Stats for each context size
    contextSizes.foreach { size =>
      statsByContext.get(size).foreach { stats =>
        stats.synchronized {
          val successRate = if (stats.totalProcessed > 0) {
            stats.correctAnswers.toDouble / stats.totalProcessed * 100
          } else 0.0
          
          sb.append(f"   └─ ${size}%3d conversations:      ${stats.correctAnswers}%3d/${stats.totalProcessed}%3d  (${successRate}%5.1f%%) - ${stats.completedTestCases}/${stats.totalTestCases} test cases completed")
          
          // Add averages and timing percentiles if we have data
          if (stats.responseTimes.nonEmpty) {
          // Calculate averages
          val avgResponseTime = stats.responseTimes.sum.toDouble / stats.responseTimes.size
          val avgInputTokens = if (stats.inputTokens.nonEmpty) 
            stats.inputTokens.sum.toDouble / stats.inputTokens.size else 0.0
          val avgOutputTokens = if (stats.outputTokens.nonEmpty) 
            stats.outputTokens.sum.toDouble / stats.outputTokens.size else 0.0
          val avgCost = if (stats.costs.nonEmpty) 
            stats.costs.sum / stats.costs.size else 0.0
          val avgCachedTokens = if (stats.cachedInputTokens.nonEmpty)
            stats.cachedInputTokens.sum.toDouble / stats.cachedInputTokens.size else 0.0
          // Handle Google's confusing API where cached can exceed prompt tokens
          val effectivePromptSize = math.max(avgInputTokens, avgCachedTokens)
          val cacheRatio = if (effectivePromptSize > 0 && avgCachedTokens > 0)
            math.min(avgCachedTokens / effectivePromptSize * 100, 100.0) else 0.0
          
          // Add averages on the same line
          sb.append(f" | Avg: ${avgResponseTime}%.0fms")
          if (stats.inputTokens.nonEmpty) sb.append(f", ${avgInputTokens}%.0f in")
          if (stats.cachedInputTokens.nonEmpty && avgCachedTokens > 0) sb.append(f" (${avgCachedTokens}%.0f cached, ${cacheRatio}%.1f%%)")
          if (stats.outputTokens.nonEmpty) sb.append(f", ${avgOutputTokens}%.0f out")
          if (stats.costs.nonEmpty) sb.append(f", $$${avgCost}%.4f")
          
          // Add timing percentiles on the same line
          val sortedTimes = stats.responseTimes.sorted.toArray
          val p50 = calculatePercentile(sortedTimes, 50)
          val p90 = calculatePercentile(sortedTimes, 90)
          val p99 = calculatePercentile(sortedTimes, 99)
          
          sb.append(f" | P50=${p50}%dms, P90=${p90}%dms, P99=${p99}%dms")
          
          // Add total cost for this context size
          val totalContextCost = stats.totalCost
          sb.append(f" | Total: $$${totalContextCost}%.4f")
        }
        
        sb.append("\n")
        
        // DEBUG: Show retrieval breakdown
        if (Config.DEBUG) {
          resultsByContext.foreach { resultsMap =>
            resultsMap.get(size).foreach { results =>
              printRetrievalBreakdown(sb, results, stats.totalProcessed)
            }
          }
        }
        } // end synchronized
      }
    }
    
    sb.append("-"*60 + "\n")
    
    // Overall progress - show both test cases and evidence items
    val completedTestCases = statsByContext.values.map { stats =>
      stats.synchronized { stats.completedTestCases }
    }.sum
    val totalTestCases = testCases.size
    val testCaseProgressPercent = if (totalTestCases > 0) {
      completedTestCases.toDouble / totalTestCases * 100
    } else 0.0
    
    val currentEvidenceItems = totalProcessed.get()
    val evidenceItemProgressPercent = if (totalEvidenceItemsInTestCases > 0) {
      currentEvidenceItems.toDouble / totalEvidenceItemsInTestCases * 100
    } else 0.0
    
    sb.append(f"\n⏱️  Progress:\n")
    sb.append(f"   └─ Test Cases:      ${completedTestCases}%5d / ${totalTestCases}%5d (${testCaseProgressPercent}%5.1f%%) completed\n")
    sb.append(f"   └─ Evidence Items:  ${currentEvidenceItems}%5d / ${totalEvidenceItemsInTestCases}%5d (${evidenceItemProgressPercent}%5.1f%%) completed\n")
    
    // Add per-minute rate for evidence items
    val currentTime = System.currentTimeMillis()
    this.synchronized {
      if (currentEvidenceItems > 0) {
        // Always calculate overall average
        val totalMinutesElapsed = (currentTime - startTime) / 60000.0
        val avgItemsPerMinute = if (totalMinutesElapsed > 0) {
          currentEvidenceItems / totalMinutesElapsed
        } else 0.0
        
        val minutesElapsed = (currentTime - lastMinuteCheck) / 60000.0
        if (minutesElapsed >= 1.0) {
          // Calculate items processed in the last period
          val itemsInLastPeriod = currentEvidenceItems - lastMinuteCount
          val recentItemsPerMinute = itemsInLastPeriod / minutesElapsed
          
          // Show both recent and average rates
          sb.append(f"   └─ Processing Rate: ${recentItemsPerMinute}%.1f items/min (last ${minutesElapsed}%.0f min), ${avgItemsPerMinute}%.1f items/min (avg)\n")
          
          // Update tracking for next minute
          lastMinuteCheck = currentTime
          lastMinuteCount = currentEvidenceItems
        } else {
          // If less than a minute has passed, show only average
          sb.append(f"   └─ Processing Rate: ${avgItemsPerMinute}%.1f items/min (avg)\n")
        }
      }
    }
    
    // Add cost tracking
    val currentCost = CostTracker.getTotalCost
    val projectedCost = CostTracker.getProjectedCost(currentEvidenceItems, totalEvidenceItemsInTestCases)
    
    // Calculate total cost from all context stats as a backup
    val totalFromContexts = statsByContext.values.map { stats =>
      stats.synchronized { stats.totalCost }
    }.sum
    
    // Use the maximum of CostTracker and context totals
    val displayCost = math.max(currentCost, totalFromContexts)
    val displayProjected = if (currentEvidenceItems > 0 && displayCost > 0) {
      displayCost * (totalEvidenceItemsInTestCases.toDouble / currentEvidenceItems)
    } else {
      projectedCost
    }
    
    // Calculate elapsed time and hourly rate
    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
    val hourlyRate = if (elapsedSeconds > 0) displayCost / (elapsedSeconds / 3600.0) else 0.0
    
    sb.append(f"   └─ Cost:            $$${displayCost.toInt}%d spent, $$${displayProjected.toInt}%d projected total ($$${hourlyRate}%.2f/hour)\n")
    
    // Add estimated time to completion
    val remainingEvidenceItems = totalEvidenceItemsInTestCases - currentEvidenceItems
    val estimatedTimeToCompletion = if (currentEvidenceItems > 0 && elapsedSeconds > 0) {
      val timePerItem = elapsedSeconds / currentEvidenceItems
      val remainingSeconds = (remainingEvidenceItems * timePerItem).toInt
      val hours = remainingSeconds / 3600
      val minutes = (remainingSeconds % 3600) / 60
      f"   └─ Est. Time to Complete: ${hours}%02d:${minutes}%02d\n"
    } else {
      "   └─ Est. Time to Complete: --:--\n"
    }
    sb.append(estimatedTimeToCompletion)
    
    sb.append("\n" + "-"*60)
    
    sb.toString()
  }
  
  /**
   * Calculate percentile from a sorted array of values.
   */
  def calculatePercentile(sortedValues: Array[Long], percentile: Double): Long = {
    if (sortedValues.isEmpty) return 0L
    
    val index = (percentile / 100.0 * (sortedValues.length - 1)).toInt
    sortedValues(index)
  }
  
  /**
   * Print retrieval breakdown for DEBUG mode.
   */
  def printRetrievalBreakdown(sb: StringBuilder, results: List[ContextTestResult], total: Int): Unit = {
    val byRetrieved = results.groupBy(_.retrievedRelevantConversations)
    val maxRetrieved = if (byRetrieved.keys.isEmpty) 0 else byRetrieved.keys.max
    
    for (count <- 0 to maxRetrieved) {
      byRetrieved.get(count).foreach { resultsForCount =>
        val correct = resultsForCount.count(_.isCorrect)
        val incorrect = resultsForCount.size - correct
        
        if (correct > 0) {
          val pct = correct.toDouble / total * 100
          sb.append(f"      └─ $count relevant retrieved (correct): $correct%3d (${pct}%5.1f%%)\n")
        }
        if (incorrect > 0) {
          val pct = incorrect.toDouble / total * 100
          sb.append(f"      └─ $count relevant retrieved (incorrect): $incorrect%3d (${pct}%5.1f%%)\n")
        }
      }
    }
  }
  
  // Simple helper methods for tests
  def getTotalProcessed: Int = totalProcessed.get()
  def totalExpectedEvidenceItems: Int = totalUniqueEvidenceItems * contextSizes.length
  def getTotalCost: Double = statsByContext.values.map { stats =>
    stats.synchronized { stats.totalCost }
  }.sum
  
  /**
   * Export current statistics to CSV file.
   * 
   * @param generatorName The name of the generator used (e.g., "user_facts", "abstention")
   * @param evidenceCount The number of evidence items (e.g., 1, 2, 3)
   * @param memorySystem The memory system being tested (e.g., "long_context", "mem0")
   * @param modelName The model name to include in the directory structure (e.g., "gemini_2_0_flash")
   * @param helperModelName Optional helper model name for block-based processing
   * @param isFinalExport Whether this is the final export at the end of the run
   */
  def exportToCSV(generatorName: String, evidenceCount: Int, memorySystem: String, modelName: String = "gemini-2.5-flash", helperModelName: Option[String] = None, isFinalExport: Boolean = false): Unit = {
    exportToCSVPath(s"src/main/resources/evaluation_results/$generatorName/$memorySystem", evidenceCount, modelName, helperModelName, isFinalExport)
  }
  
  /**
   * Export current statistics to CSV file with custom base path (for testing).
   */
  def exportToCSVPath(basePath: String, evidenceCount: Int, modelName: String = "gemini-2.5-flash", helperModelName: Option[String] = None, isFinalExport: Boolean = false): Unit = {
    try {
      // Sanitize model name by replacing inconvenient characters with underscore
      val sanitizedModelName = modelName.replaceAll("[^a-zA-Z0-9_]", "_")
      
      // Create directory path with model name and optional helper model
      val dirPath = helperModelName match {
        case Some(helper) =>
          val sanitizedHelper = helper.replaceAll("[^a-zA-Z0-9_]", "_")
          s"$basePath/$sanitizedModelName/helper_model_$sanitizedHelper"
        case None =>
          s"$basePath/$sanitizedModelName"
      }
      Files.createDirectories(Paths.get(dirPath))
      
      // Create file paths
      val csvFilePath = s"$dirPath/${evidenceCount}_evidence.csv"
      val historyFilePath = s"$dirPath/${evidenceCount}_evidence.history"
      
      
      // Only save to history on final export
      if (isFinalExport) {
        saveToHistory(historyFilePath)
      }
      
      // Now export aggregated or current data to main CSV
      val writer = new PrintWriter(new File(csvFilePath))
      
      try {
        // Write header
        writer.println("context_size,success_rate_percent,correct_answers,total_processed,test_cases_completed,total_test_cases,avg_response_time_ms,avg_input_tokens,avg_output_tokens,avg_cost,p50_ms,p90_ms,p99_ms,avg_cached_tokens,cache_ratio_percent")
        
        // Write data for each context size
        contextSizes.foreach { size =>
          statsByContext.get(size).foreach { stats =>
            val successRate = if (stats.totalProcessed > 0) {
              stats.correctAnswers.toDouble / stats.totalProcessed * 100
            } else 0.0
            
            // Calculate averages
            val avgResponseTime = if (stats.responseTimes.nonEmpty) 
              stats.responseTimes.sum.toDouble / stats.responseTimes.size else 0.0
            val avgInputTokens = if (stats.inputTokens.nonEmpty) 
              stats.inputTokens.sum.toDouble / stats.inputTokens.size else 0.0
            val avgOutputTokens = if (stats.outputTokens.nonEmpty) 
              stats.outputTokens.sum.toDouble / stats.outputTokens.size else 0.0
            val avgCost = if (stats.costs.nonEmpty) 
              stats.costs.sum / stats.costs.size else 0.0
            val avgCachedTokens = if (stats.cachedInputTokens.nonEmpty)
              stats.cachedInputTokens.sum.toDouble / stats.cachedInputTokens.size else 0.0
            
            // Calculate percentiles
            val (p50, p90, p99) = if (stats.responseTimes.nonEmpty) {
              val sortedTimes = stats.responseTimes.sorted.toArray
              (
                calculatePercentile(sortedTimes, 50),
                calculatePercentile(sortedTimes, 90),
                calculatePercentile(sortedTimes, 99)
              )
            } else {
              (0L, 0L, 0L)
            }
            
            // Calculate cache ratio
            val effectivePromptSize = math.max(avgInputTokens, avgCachedTokens)
            val cacheRatio = if (effectivePromptSize > 0 && avgCachedTokens > 0) {
              math.min(avgCachedTokens / effectivePromptSize * 100, 100.0)
            } else 0.0
            
            writer.println(f"$size,$successRate%.1f,${stats.correctAnswers},${stats.totalProcessed},${stats.completedTestCases},${stats.totalTestCases},$avgResponseTime%.0f,$avgInputTokens%.0f,$avgOutputTokens%.0f,$avgCost%.4f,$p50,$p90,$p99,$avgCachedTokens%.0f,$cacheRatio%.1f")
          }
        }
        
        if (overrideCSV) {
          println(s"📊 Exported evaluation results to: $csvFilePath (override mode)")
        } else {
          println(s"📊 Exported aggregated evaluation results to: $csvFilePath")
        }
      } finally {
        writer.close()
      }
    } catch {
      case e: Exception =>
        println(s"⚠️  Failed to export CSV: ${e.getMessage}")
    }
  }

  /**
   * Check if early termination conditions are met.
   * Returns true if any of these conditions are met:
   * 
   * Condition 1 (original): All of these must be true:
   * - We've spent at least $20
   * - Each context size has at least 50 correctly answered evidence items
   * - Success rate in each context size is >= 40%
   * - As we go to increasing context size, the success rate is <= all success rates with smaller context sizes (monotonic decrease)
   * 
   * Condition 2: All of these must be true:
   * - We've spent more than $100
   * - Only 1 success rate is not on the descending graph (i.e., at most one context size breaks the monotonic decrease)
   * 
   * Condition 3: All of these must be true:
   * - We've spent more than $150
   * - Average success rate for first half of context sizes is at least 5% bigger than the later half
   * 
   * Condition 4: If we've spent more than $300, stop regardless of other conditions
   */
  def shouldTerminateEarly(): Boolean = {
    // Calculate total cost across all contexts
    val totalCost = statsByContext.values.map { stats =>
      stats.synchronized { stats.totalCost }
    }.sum
    
    // Check Condition 4 first - absolute cost limit
    if (totalCost > 300.0) {
      println("\n" + "="*80)
      println("EARLY TERMINATION - CONDITION 4 MET")
      println("="*80)
      println(f"✅ Total cost: $$${totalCost}%.2f (exceeds $$300)")
      println("✅ Stopping to prevent excessive spending")
      println("="*80)
      return true
    }
    
    // First, collect all success rates for processed context sizes
    val successRates = scala.collection.mutable.ArrayBuffer[(Int, Double, Int)]() // (contextSize, successRate, correctAnswers)
    
    for (contextSize <- contextSizes) {
      statsByContext.get(contextSize) match {
        case Some(stats) =>
          val (processed, correct, successRate) = stats.synchronized {
            val p = stats.totalProcessed
            val c = stats.correctAnswers
            val s = if (p > 0) c.toDouble / p * 100 else 0.0
            (p, c, s)
          }
          
          // Only include context sizes that have been processed
          if (processed > 0) {
            successRates += ((contextSize, successRate, correct))
          }
          
        case None => // Skip
      }
    }
    
    // If we don't have enough data, we can't terminate
    if (successRates.isEmpty) {
      return false
    }
    
    // Check Condition 1 (original)
    if (totalCost >= 20.0) {
      var condition1Met = true
      var previousSuccessRate = 100.0
      
      for ((contextSize, successRate, correct) <- successRates) {
        // Check if we have at least 50 correct answers
        if (correct < 50) {
          condition1Met = false
        }
        
        // Check if success rate is >= 40%
        if (successRate < 40.0) {
          condition1Met = false
        }
        
        // Check if success rate is monotonically decreasing
        if (successRate > previousSuccessRate) {
          condition1Met = false
        }
        
        previousSuccessRate = successRate
      }
      
      // Need data for ALL context sizes for condition 1
      if (successRates.size < contextSizes.size) {
        condition1Met = false
      }
      
      if (condition1Met) {
        println("\n" + "="*80)
        println("EARLY TERMINATION - CONDITION 1 MET")
        println("="*80)
        println(f"✅ Total cost: $$${totalCost}%.2f (>= $$20)")
        println("✅ Each context size has >= 50 correct answers")
        println("✅ Each context size has >= 40% success rate")
        println("✅ Success rates are monotonically decreasing with context size")
        println("="*80)
        return true
      }
    }
    
    // Check Condition 2
    if (totalCost > 100.0) {
      // Count how many times the monotonic decrease is violated
      var violationCount = 0
      var previousSuccessRate = 100.0
      
      for ((contextSize, successRate, _) <- successRates) {
        if (successRate > previousSuccessRate) {
          violationCount += 1
        }
        previousSuccessRate = successRate
      }
      
      // If at most 1 violation (i.e., only 1 success rate is not on the descending graph)
      if (violationCount <= 1) {
        println("\n" + "="*80)
        println("EARLY TERMINATION - CONDITION 2 MET")
        println("="*80)
        println(f"✅ Total cost: $$${totalCost}%.2f (> $$100)")
        println(f"✅ Monotonic decrease violations: $violationCount (≤ 1)")
        println("="*80)
        return true
      }
    }
    
    // Check Condition 3
    if (totalCost > 150.0 && successRates.size >= 2) {
      // Split processed context sizes into two halves
      val midpoint = successRates.size / 2
      val firstHalf = successRates.take(midpoint)
      val secondHalf = successRates.drop(midpoint)
      
      // Calculate average success rates for each half
      val avgFirstHalf = if (firstHalf.nonEmpty) {
        firstHalf.map(_._2).sum / firstHalf.size
      } else 0.0
      
      val avgSecondHalf = if (secondHalf.nonEmpty) {
        secondHalf.map(_._2).sum / secondHalf.size
      } else 0.0
      
      // Check if first half average is at least 5% bigger than second half
      if (avgFirstHalf >= avgSecondHalf + 5.0) {
        println("\n" + "="*80)
        println("EARLY TERMINATION - CONDITION 3 MET")
        println("="*80)
        println(f"✅ Total cost: $$${totalCost}%.2f (> $$150)")
        println(f"✅ First half average success rate: ${avgFirstHalf}%.1f%%")
        println(f"✅ Second half average success rate: ${avgSecondHalf}%.1f%%")
        println(f"✅ Difference: ${avgFirstHalf - avgSecondHalf}%.1f%% (>= 5%%)")
        println("="*80)
        return true
      }
    }
    
    // No condition met
    false
  }

  /**
   * Save current run data to history file.
   */
  def saveToHistory(historyFilePath: String): Unit = {
    try {
      val writer = new PrintWriter(new java.io.FileWriter(historyFilePath, true)) // append mode
      
      try {
        // Add timestamp and run info
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val checkpoint = Utils.getGitCheckpoint()
        writer.println(s"\n=== Run at $timestamp ===")
        writer.println(s"Git checkpoint: $checkpoint")
        writer.println()
        
        // Write CSV header
        writer.println("context_size,success_rate_percent,correct_answers,total_processed,test_cases_completed,total_test_cases,avg_response_time_ms,avg_input_tokens,avg_output_tokens,avg_cost,p50_ms,p90_ms,p99_ms,avg_cached_tokens,cache_ratio_percent")
        
        // Write current run data
        contextSizes.foreach { size =>
          statsByContext.get(size).foreach { stats =>
            val successRate = if (stats.totalProcessed > 0) {
              stats.correctAnswers.toDouble / stats.totalProcessed * 100
            } else 0.0
            
            // Calculate averages
            val avgResponseTime = if (stats.responseTimes.nonEmpty) 
              stats.responseTimes.sum.toDouble / stats.responseTimes.size else 0.0
            val avgInputTokens = if (stats.inputTokens.nonEmpty) 
              stats.inputTokens.sum.toDouble / stats.inputTokens.size else 0.0
            val avgOutputTokens = if (stats.outputTokens.nonEmpty) 
              stats.outputTokens.sum.toDouble / stats.outputTokens.size else 0.0
            val avgCost = if (stats.costs.nonEmpty) 
              stats.costs.sum / stats.costs.size else 0.0
            val avgCachedTokens = if (stats.cachedInputTokens.nonEmpty)
              stats.cachedInputTokens.sum.toDouble / stats.cachedInputTokens.size else 0.0
            // Handle Google's confusing API where cached can exceed prompt tokens
            val effectivePromptSize = math.max(avgInputTokens, avgCachedTokens)
            val cacheRatio = if (effectivePromptSize > 0 && avgCachedTokens > 0)
              math.min(avgCachedTokens / effectivePromptSize * 100, 100.0) else 0.0
            
            // Calculate percentiles
            val (p50, p90, p99) = if (stats.responseTimes.nonEmpty) {
              val sortedTimes = stats.responseTimes.sorted.toArray
              (
                calculatePercentile(sortedTimes, 50),
                calculatePercentile(sortedTimes, 90),
                calculatePercentile(sortedTimes, 99)
              )
            } else {
              (0L, 0L, 0L)
            }
            
            writer.println(f"$size,$successRate%.1f,${stats.correctAnswers},${stats.totalProcessed},${stats.completedTestCases},${stats.totalTestCases},$avgResponseTime%.0f,$avgInputTokens%.0f,$avgOutputTokens%.0f,$avgCost%.4f,$p50,$p90,$p99,$avgCachedTokens%.0f,$cacheRatio%.1f")
          }
        }
        writer.println()
      } finally {
        writer.close()
      }
    } catch {
      case e: Exception =>
        println(s"⚠️  Failed to save to history: ${e.getMessage}")
    }
  }
}