package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, Gemini}
import com.salesforce.crmmembench.evaluation.memory.MemoryAnswererFactory
import com.salesforce.crmmembench.questions.evidence.EvidenceItem
import com.salesforce.crmmembench.questions.evidence.generation.AnsweringEvaluation
import com.salesforce.crmmembench.{Config, StatsProvider, StatsReporter, Utils}
import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.ScheduledExecutorService

/**
 * Handles multithreaded evaluation with test cases.
 * This version is refactored to work with TestCase objects instead of 
 * directly mixing evidence items and conversations.
 */
class MultithreadedEvaluator(
  val caseType: String,
  val memoryFactory: MemoryAnswererFactory,
  val model: Option[LLMModel],
  val helperModel: Option[LLMModel] = None,
  val testCaseThreads: Int,
  val answeringEvaluation: AnsweringEvaluation,
  val testCaseGeneratorType: String,
  val judgeModel: LLMModel = Gemini.flash,
  val evidenceCount: Int = 1
) {
  
  /**
   * Run the multithreaded evaluation with test cases.
   * 
   * @param testCases List of test cases to evaluate
   */
  def runEvaluation(testCases: List[TestCase]): Unit = {
    // Early exit if no test cases
    if (testCases.isEmpty) {
      println("❌ ERROR: No test cases provided to MultithreadedEvaluator. Halting.")
      return
    }
    
    // Extract generator info for initialization
    val (generatorName, evidenceCount) = extractGeneratorInfo(caseType, testCases)
    val modelName = model.map(_.getModelName).getOrElse("gemini-2.5-flash")
    val helperModelName = helperModel.map(_.getModelName)
    
    // Initialize logging for this evaluation run
    EvaluationLogger.initializeRun(generatorName, memoryFactory.name, modelName, evidenceCount)
    
    // Create balanced batches instead of shuffling all test cases
    val batches = BatchUtils.createBalancedBatches(testCases, numBatches = 30)
    
    println(f"🚀 Using ${testCaseThreads} threads for processing ${testCases.size} test cases in ${batches.size} balanced batches")
    
    // Create stats tracker with test cases (always overwriting CSV)
    val statsTracker = new EvaluationStatsTracker(testCases, caseType, memoryFactory.name, testCaseGeneratorType, true)
    val statsReporter = statsTracker.startPeriodicReporting(
      intervalSeconds = Config.Reporting.EVALUATION_STATS_INTERVAL_SECONDS
    )
    
    // Generator info already extracted above
    
    // Schedule periodic CSV export (every 5 minutes)
    val csvScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    csvScheduler.scheduleAtFixedRate(new Runnable {
      def run(): Unit = {
        val modelName = model.map(_.getModelName).getOrElse("gemini-2.5-flash")
        val helperModelName = helperModel.map(_.getModelName)
        statsTracker.exportToCSV(generatorName, evidenceCount, memoryFactory.name, modelName, helperModelName, isFinalExport = false)
      }
    }, 5, 5, TimeUnit.MINUTES) // Initial delay: 5 minutes, period: 5 minutes
    
    try {
      // Main evaluation phase - process batches sequentially
      if (batches.nonEmpty) {
        println(s"\n🔄 Evaluating ${testCases.size} test cases in ${batches.size} batches...")
        
        var shouldContinue = true
        var batchNumber = 0
        
        // Process each batch
        for (batch <- batches if shouldContinue) {
          batchNumber += 1
          println(s"\n📦 Processing batch $batchNumber/${batches.size} (${batch.size} test cases)...")
          
          // Split batch into two pools based on conversation count
          val (largeContexts, smallContexts) = batch.partition(_.conversationCount >= 30)
          
          // Process both pools in parallel
          val largeContextThread = new Thread(() => {
            if (largeContexts.nonEmpty) {
              println(s"  🔹 Processing ${largeContexts.size} large context test cases (>= 30 conversations)")
              Utils.parallelForeach(largeContexts, threadCount = testCaseThreads) { testCase =>
                evaluateTestCase(testCase, statsTracker)
              }
            }
          })
          
          val smallContextThread = new Thread(() => {
            if (smallContexts.nonEmpty) {
              println(s"  🔸 Processing ${smallContexts.size} small context test cases (< 30 conversations)")
              Utils.parallelForeach(smallContexts, threadCount = testCaseThreads) { testCase =>
                evaluateTestCase(testCase, statsTracker)
              }
            }
          })
          
          // Start both threads and wait for completion
          largeContextThread.start()
          smallContextThread.start()
          largeContextThread.join()
          smallContextThread.join()
          
          // After each batch, check if we should terminate early
          if (statsTracker.shouldTerminateEarly()) {
            // Condition details are printed by shouldTerminateEarly() method
            println(s"✅ Processed $batchNumber out of ${batches.size} batches")
            shouldContinue = false
          } else if (batchNumber < batches.size) {
            // Print progress when moving to next batch
            println(s"\n✅ Completed batch $batchNumber/${batches.size}")
            println(s"📊 Current stats: ${statsTracker.getTotalProcessed} items processed, $$${statsTracker.getTotalCost}%.2f spent")
            println(s"➡️  Moving to next batch...")
          }
        }
        
        // Print final completion message if we processed all batches
        if (shouldContinue && batchNumber == batches.size) {
          println(s"\n✅ Completed all ${batches.size} batches successfully!")
          println(s"📊 Final stats: ${statsTracker.getTotalProcessed} items processed, $$${statsTracker.getTotalCost}%.2f spent")
        }
      }
    } finally {
      // Shutdown CSV scheduler
      csvScheduler.shutdown()
      
      // Export final CSV with history
      val modelName = model.map(_.getModelName).getOrElse("gemini-2.5-flash")
      val helperModelName = helperModel.map(_.getModelName)
      statsTracker.exportToCSV(generatorName, evidenceCount, memoryFactory.name, modelName, helperModelName, isFinalExport = true)
      
      // Finalize logging system
      EvaluationLogger.finalizeRun()
      
      // Final comprehensive report
      println("\n" + "="*80)
      println("FINAL COMPREHENSIVE EVALUATION RESULTS")
      println("="*80)
      statsTracker.printFinalStatsAndCancel(statsReporter)
    }
  }
  
  /**
   * Evaluate a single test case.
   * Currently evaluates each evidence item in the test case independently.
   * In the future, may evaluate multiple evidence items together.
   */
  def evaluateTestCase(
    testCase: TestCase,
    statsTracker: EvaluationStatsTracker
  ): Unit = {
    // Create memory answerer for this test case
    val memoryAnswerer = memoryFactory.create(model, helperModel)
    memoryAnswerer.initialize()
    
    // Load all conversations for this test case
    memoryAnswerer.addConversations(testCase.conversations)
    
    // Evaluate each evidence item in the test case
    // In the future, this could evaluate multiple items together
    testCase.evidenceItems.foreach { evidenceItem =>
      // Measure response time
      val startTime = System.currentTimeMillis()
      
      // Answer the question
      val answerResult = memoryAnswerer.answerQuestion(evidenceItem.question, testCase.id)
      val answerOpt = answerResult.answer
      val retrievedConvIds = answerResult.retrievedConversationIds
      
      // Calculate response time
      val responseTimeMs = System.currentTimeMillis() - startTime
      
      // Count how many evidence conversations were retrieved
      val evidenceConvIds = testCase.getEvidenceConversations.flatMap(_.id).toSet
      val retrievedRelevantCount = retrievedConvIds.count(evidenceConvIds.contains)
      
      // Verify the answer
      answerOpt match {
        case Some(modelAnswer) =>
          EvaluationUtils.verifyAnswerCorrectness(
            evidenceItem.question, 
            evidenceItem.answer, 
            modelAnswer, 
            evidenceItem.message_evidences,
            answeringEvaluation,
            judgeModel
          ) match {
            case Some(isCorrect) =>
              val result = ContextTestResult(
                evidenceItem = evidenceItem,
                contextType = "mixed",
                contextSize = testCase.conversationCount,
                modelAnswer = modelAnswer,
                isCorrect = isCorrect,
                retrievedRelevantConversations = retrievedRelevantCount
              )
              // Record the result in stats tracker with response time and answer result
              statsTracker.recordEvidenceResult(
                testCase, 
                result, 
                responseTimeMs,
                Some(answerResult)
              )
            case None =>
              println(s"      Failed to verify answer for test case ${testCase.id}")
          }
        case None =>
          println(s"      Failed to get answer for test case ${testCase.id}")
      }
    }
    
    // Notify stats tracker that test case is completed
    statsTracker.recordTestCaseCompleted(testCase)
    
    // Clean up memory answerer
    memoryAnswerer.cleanup()
  }
  
  /**
   * Extract generator name and evidence count from case type and test cases.
   */
  def extractGeneratorInfo(caseType: String, testCases: List[TestCase]): (String, Int) = {
    // Extract generator name from caseType (e.g., "standard user fact" -> "user_fact")
    val generatorName = caseType.toLowerCase
      .replace("standard ", "")
      .replace(" generator", "")
      .replace(" ", "_")
    
    // Use the evidence count from the generator configuration
    // This is passed in through the constructor
    (generatorName, evidenceCount)
  }
  
}

