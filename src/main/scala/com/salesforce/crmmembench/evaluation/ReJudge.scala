package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.evaluation.EvaluationLogger.EvaluationLogEntry
import com.salesforce.crmmembench.evaluation.memory.{CachedLogMemoryFactory, MemoryAnswererFactory}
import com.salesforce.crmmembench.questions.evidence.generation.AnsweringEvaluation
import io.circe.generic.auto._
import io.circe.parser._

import java.io.File
import scala.io.Source

/**
 * Abstract base class for re-judging existing evaluation logs with different criteria.
 * 
 * This class extends Evaluator to leverage the existing evaluation infrastructure while
 * re-evaluating existing model responses without re-running expensive inference.
 * 
 * ReJudge works by:
 * 1. Parsing existing evaluation logs to extract questions and model answers
 * 2. Creating test cases from these logs (grouped by context size)
 * 3. Using a cached memory answerer that returns the original model answers
 * 4. Re-judging these answers with potentially different evaluation criteria
 * 
 * Useful for:
 * - Experimenting with different judge prompts
 * - Testing stricter or more lenient evaluation criteria  
 * - Changing the judge model (e.g., from Gemini Flash to GPT-4)
 */
abstract class ReJudge extends Evaluator {
  
  /**
   * The original evaluator that was used to generate the logs.
   * Each implementation provides the specific evaluator instance.
   */
  def originalEvaluator: Evaluator
  
  // Lazy initialization to parse logs once
  lazy val parsedEntries = parseOriginalLogs()
  
  // Create log-based test cases generator with cached test cases and mapping
  lazy val logBasedGenerator = new LogBasedTestCasesGenerator(parsedEntries, originalEvaluator.testCasesGenerator.evidenceGenerator)
  
  // Override testCasesGenerator to use log-based generator
  override def testCasesGenerator: TestCasesGenerator = logBasedGenerator
  
  // Override memoryFactory to use cached log memory with the mapping from generator
  override def memoryFactory: MemoryAnswererFactory = 
    new CachedLogMemoryFactory(logBasedGenerator.testCaseIdToLogEntry, originalEvaluator.memoryFactory.name)
  
  // By default, use the same answering evaluation as the original evaluator
  // Subclasses can override this to use different evaluation criteria
  override def answeringEvaluation: AnsweringEvaluation = originalEvaluator.answeringEvaluation

  /**
   * Parse the original evaluation logs.
   * @return List of parsed log entries
   */
  def parseOriginalLogs(): List[EvaluationLogEntry] = {
    // Extract info from original evaluator
    val testCasesGen = originalEvaluator.testCasesGenerator
    val memoryType = originalEvaluator.memoryFactory.name
    val model = originalEvaluator.model
    val caseType = testCasesGen.generatorType.toLowerCase
      .replace(" generator", "")
      .replace(" ", "_")  // Replace spaces with underscores for directory names
    val modelName = model.getModelName
    
    // Get evidence count from the ORIGINAL evaluator's generator (simple public method!)
    val evidenceCount = originalEvaluator.testCasesGenerator.getEvidenceCount()
    
    println("="*80)
    println(s"REJUDGE - ${caseType.toUpperCase} - ${memoryType.toUpperCase}")
    println("="*80)
    
    if (runShort) {
      println("🚀 Running in SHORT mode: re-judging limited entries for quick testing")
    }
    
    // Find latest log directory
    val logDir = findLatestLogDirectory(caseType, memoryType, modelName, evidenceCount)
    println(s"📁 Re-judging from: $logDir")
    
    // Parse existing logs
    val logEntries = parseLogFiles(logDir)
    println(s"📊 Found ${logEntries.size} entries to re-judge")
    
    // Apply short mode limit if needed
    if (runShort) {
      val shortCount = 20
      println(s"SHORT mode: Re-judging only first $shortCount entries")
      logEntries.take(shortCount)
    } else {
      logEntries
    }
  }
  
  def findLatestLogDirectory(caseType: String, memoryType: String, modelName: String, evidenceCount: Int): String = {
    val sanitizedModelName = modelName.replaceAll("[^a-zA-Z0-9_]", "_")
    val basePath = s"logs/evaluations/$caseType/$memoryType/$sanitizedModelName/${evidenceCount}_evidence"
    
    val baseDir = new File(basePath)
    if (!baseDir.exists() || !baseDir.isDirectory) {
      throw new RuntimeException(s"No evaluation logs found at: $basePath")
    }
    
    val timestampDirs = baseDir.listFiles()
      .filter(_.isDirectory)
      .filter(_.getName.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"))
      .sortBy(_.getName)
      .reverse
    
    if (timestampDirs.isEmpty) {
      throw new RuntimeException(s"No evaluation runs found in: $basePath")
    }
    
    timestampDirs.head.getAbsolutePath
  }
  
  def parseLogFiles(logDir: String): List[EvaluationLogEntry] = {
    val correctFile = new File(s"$logDir/correct_responses.json")
    val incorrectFile = new File(s"$logDir/incorrect_responses.json")
    
    val entries = scala.collection.mutable.ListBuffer[EvaluationLogEntry]()
    var hadParseError = false
    
    // Parse correct responses
    if (correctFile.exists()) {
      val correctJson = Source.fromFile(correctFile).mkString
      val repairedJson = attemptJsonRepair(correctJson)
      parse(repairedJson) match {
        case Right(json) =>
          json.as[List[EvaluationLogEntry]] match {
            case Right(correctEntries) => 
              entries ++= correctEntries
              if (repairedJson != correctJson) {
                println(s"⚠️  WARNING: Repaired incomplete JSON in correct_responses.json (recovered ${correctEntries.size} entries)")
              }
            case Left(error) => 
              println(s"❌ ERROR: Failed to parse correct responses: $error")
              hadParseError = true
          }
        case Left(error) =>
          println(s"❌ ERROR: Failed to parse correct responses JSON: $error")
          hadParseError = true
      }
    }
    
    // Parse incorrect responses  
    if (incorrectFile.exists()) {
      val incorrectJson = Source.fromFile(incorrectFile).mkString
      val repairedJson = attemptJsonRepair(incorrectJson)
      parse(repairedJson) match {
        case Right(json) =>
          json.as[List[EvaluationLogEntry]] match {
            case Right(incorrectEntries) => 
              entries ++= incorrectEntries
              if (repairedJson != incorrectJson) {
                println(s"⚠️  WARNING: Repaired incomplete JSON in incorrect_responses.json (recovered ${incorrectEntries.size} entries)")
              }
            case Left(error) =>
              println(s"❌ ERROR: Failed to parse incorrect responses: $error")
              hadParseError = true
          }
        case Left(error) =>
          println(s"❌ ERROR: Failed to parse incorrect responses JSON: $error")
          hadParseError = true
      }
    }
    
    // Fail immediately if we had parsing errors
    if (hadParseError) {
      throw new RuntimeException("Failed to parse evaluation logs. Halting re-judge operation.")
    }
    
    // Check if we have any entries to re-judge
    if (entries.isEmpty) {
      throw new RuntimeException("Found 0 entries to re-judge. Halting re-judge operation.")
    }
    
    entries.toList
  }
  
  /**
   * Attempts to repair incomplete JSON by finding complete objects.
   * This handles cases where the evaluation was interrupted mid-write.
   */
  def attemptJsonRepair(jsonString: String): String = {
    val trimmed = jsonString.trim
    
    // If it starts with [ but doesn't end with ], it's likely an incomplete array
    if (trimmed.startsWith("[") && !trimmed.endsWith("]")) {
      // Track nested structure (braces and brackets)
      var braceCount = 0
      var bracketCount = 0
      var lastCompleteTopLevelObjectEnd = -1
      var inString = false
      var escapeNext = false
      var inTopLevelObject = false
      
      // Start after the opening [
      bracketCount = 1
      
      for (i <- 1 until trimmed.length) {
        val char = trimmed(i)
        
        if (!escapeNext) {
          if (char == '"' && !inString) {
            inString = true
          } else if (char == '"' && inString) {
            inString = false
          } else if (char == '\\' && inString) {
            escapeNext = true
          } else if (!inString) {
            char match {
              case '{' => 
                braceCount += 1
                if (braceCount == 1 && bracketCount == 1) {
                  inTopLevelObject = true
                }
              case '}' => 
                braceCount -= 1
                // If we're back to zero braces and at top level, we have a complete top-level object
                if (braceCount == 0 && bracketCount == 1 && inTopLevelObject) {
                  lastCompleteTopLevelObjectEnd = i
                  inTopLevelObject = false
                }
              case '[' => 
                bracketCount += 1
              case ']' => 
                bracketCount -= 1
              case _ => // ignore other chars
            }
          }
        } else {
          escapeNext = false
        }
      }
      
      // If we found at least one complete top-level object, repair the JSON
      if (lastCompleteTopLevelObjectEnd > 0) {
        // Truncate after the last complete object and close the array
        return trimmed.substring(0, lastCompleteTopLevelObjectEnd + 1) + "]"
      } else if (trimmed.length > 1 && trimmed(1) == ']') {
        // Edge case: empty array that's already complete
        return "[]"
      }
    }
    
    // Return original if we can't repair or it appears complete
    jsonString
  }
}