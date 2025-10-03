package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.evaluation.memory.AnswerResult
import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Message, Conversation}

import java.io.{File, FileWriter, PrintWriter}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.collection.JavaConverters._

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

/**
 * Comprehensive logging system for evaluation results.
 * 
 * Logs evaluation results in JSON format with separate files for correct and incorrect responses.
 * Creates a unique directory for each evaluation run with proper case subfolders.
 */
object EvaluationLogger {
  
  // Allow configurable base directory for testing
  var baseLogsDir: String = "logs/evaluations"
  
  /**
   * Set the base logs directory (primarily for testing).
   * @param dir The base directory path
   */
  def setBaseLogsDir(dir: String): Unit = {
    baseLogsDir = dir
  }
  
  /**
   * Reset to default logs directory.
   */
  def resetBaseLogsDir(): Unit = {
    baseLogsDir = "logs/evaluations"
  }

  case class EvaluationLogEntry(
    contextTestResult: ContextTestResult,
    answerResult: Option[AnswerResult],
    evidenceType: String, // Case type like "user_facts", "abstention"
    memorySystem: String, // The memory system used (e.g., "long_context", "mem0")
    testCaseGeneratorType: String, // Type of generator used (e.g., "standard", "batched", "stitched")
    responseTimeMs: Long
  )

  // Use Circe for JSON serialization (handles Scala types properly)
  var currentRunId: String = _
  var currentRunDir: File = _
  var correctLogFile: File = _
  var incorrectLogFile: File = _
  var correctWriter: PrintWriter = _
  var incorrectWriter: PrintWriter = _
  val loggedCount = new AtomicInteger(0)
  
  // Thread-safe collections for batch writing
  val correctEntries = mutable.ListBuffer[EvaluationLogEntry]()
  val incorrectEntries = mutable.ListBuffer[EvaluationLogEntry]()
  var correctCount = new AtomicInteger(0)
  var incorrectCount = new AtomicInteger(0)
  val lock = new Object()
  var initialized = false
  
  def isInitialized: Boolean = initialized

  /**
   * Initialize logging for a new evaluation run.
   * Creates unique directory structure and prints run ID.
   * @param caseType Generator use case type (e.g., "user_facts", "abstention")
   * @param memorySystem Memory system (e.g., "long_context", "mem0")
   * @param modelName Model name (e.g., "gemini_2_5_flash")
   * @param evidenceCount Number of evidence items
   */
  def initializeRun(caseType: String, memorySystem: String, modelName: String, evidenceCount: Int): String = {
    // Create timestamp-based directory name
    val timestamp = java.time.LocalDateTime.now().format(
      java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    )
    currentRunId = timestamp
    
    // Sanitize model name by replacing inconvenient characters with underscore
    val sanitizedModelName = modelName.replaceAll("[^a-zA-Z0-9_]", "_")
    
    // Create base logs directory structure
    val logsDir = new File(baseLogsDir)
    if (!logsDir.exists()) logsDir.mkdirs()
    
    // Create directory structure: logs/evaluations/generator_use_case/memory_system/model/evidence_count/timestamp
    val generatorDir = new File(logsDir, caseType)
    if (!generatorDir.exists()) generatorDir.mkdirs()
    
    val memoryDir = new File(generatorDir, memorySystem)
    if (!memoryDir.exists()) memoryDir.mkdirs()
    
    val modelDir = new File(memoryDir, sanitizedModelName)
    if (!modelDir.exists()) modelDir.mkdirs()
    
    val evidenceDir = new File(modelDir, s"${evidenceCount}_evidence")
    if (!evidenceDir.exists()) evidenceDir.mkdirs()
    
    currentRunDir = new File(evidenceDir, timestamp)
    if (!currentRunDir.exists()) currentRunDir.mkdirs()
    
    // Create log files
    correctLogFile = new File(currentRunDir, "correct_responses.json")
    incorrectLogFile = new File(currentRunDir, "incorrect_responses.json")
    
    // Initialize writers
    correctWriter = new PrintWriter(new FileWriter(correctLogFile))
    incorrectWriter = new PrintWriter(new FileWriter(incorrectLogFile))
    
    // Reset counters
    correctCount = new AtomicInteger(0)
    incorrectCount = new AtomicInteger(0)
    initialized = true
    loggedCount.set(0)
    
    // Start JSON arrays
    correctWriter.println("[")
    incorrectWriter.println("[")
    
    println(s"🔍 Evaluation Run ID: $currentRunId")
    println(s"📁 Log Directory: ${currentRunDir.getAbsolutePath}")
    
    currentRunId
  }

  /**
   * Log a single evaluation result.
   * @param result The test result
   * @param caseType The case type (e.g., "user_facts")
   * @param memorySystem The memory system used
   * @param testCaseGeneratorType The type of test case generator used
   * @param responseTimeMs Response time in milliseconds
   * @param answerResult The answer result containing token and cost info
   */
  def logResult(
    result: ContextTestResult, 
    caseType: String, 
    memorySystem: String,
    testCaseGeneratorType: String,
    responseTimeMs: Long,
    answerResult: Option[AnswerResult]
  ): Unit = {
    if (currentRunId == null) {
      throw new IllegalStateException("EvaluationLogger not initialized. Call initializeRun() first.")
    }
    
    val entry = EvaluationLogEntry(
      contextTestResult = result,
      answerResult = answerResult,
      evidenceType = caseType,
      memorySystem = memorySystem,
      testCaseGeneratorType = testCaseGeneratorType,
      responseTimeMs = responseTimeMs
    )
    
    lock.synchronized {
      if (result.isCorrect) {
        val entryCount = correctCount.getAndIncrement()
        val needsComma = if (entryCount > 0) "," else ""
        correctEntries += entry
        val jsonStr = entry.asJson.spaces2
        correctWriter.println(s"$needsComma  $jsonStr")
      } else {
        val entryCount = incorrectCount.getAndIncrement()
        val needsComma = if (entryCount > 0) "," else ""
        incorrectEntries += entry
        val jsonStr = entry.asJson.spaces2
        incorrectWriter.println(s"$needsComma  $jsonStr")
      }
      
      val currentCount = loggedCount.incrementAndGet()
      
      // Flush every 10 entries for real-time reading
      if (currentCount % 10 == 0) {
        correctWriter.flush()
        incorrectWriter.flush()
      }
    }
  }

  /**
   * Flush all pending writes to ensure real-time accessibility.
   */
  def flush(): Unit = {
    lock.synchronized {
      if (correctWriter != null) correctWriter.flush()
      if (incorrectWriter != null) incorrectWriter.flush()
    }
  }

  /**
   * Finalize logging and close files.
   */
  def finalizeRun(): Unit = {
    lock.synchronized {
      if (correctWriter != null) {
        correctWriter.println("]")
        correctWriter.close()
      }
      
      if (incorrectWriter != null) {
        incorrectWriter.println("]")
        incorrectWriter.close()
      }
      
      val totalEntries = correctEntries.size + incorrectEntries.size
      val correctEntryCount = correctEntries.size
      val incorrectEntryCount = incorrectEntries.size
      
      println(s"📊 Logged $totalEntries evaluation results:")
      println(s"   ✅ Correct: $correctEntryCount")
      println(s"   ❌ Incorrect: $incorrectEntryCount")
      println(s"   📁 Logs saved to: ${currentRunDir.getAbsolutePath}")
      
      initialized = false
      
      // Clear state
      correctEntries.clear()
      incorrectEntries.clear()
      loggedCount.set(0)
      currentRunId = null
      currentRunDir = null
      correctLogFile = null
      incorrectLogFile = null
      correctWriter = null
      incorrectWriter = null
    }
  }

  /**
   * Get the current run directory path.
   */
  def getCurrentRunDir: Option[String] = {
    Option(currentRunDir).map(_.getAbsolutePath)
  }

  /**
   * Get the current run ID.
   */
  def getCurrentRunId: Option[String] = {
    Option(currentRunId)
  }
}