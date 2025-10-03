package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.evaluation.EvaluationLogger.EvaluationLogEntry
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generation.AnsweringEvaluation

/**
 * Test cases generator that creates test cases from parsed evaluation log entries.
 * This is used by ReJudge to re-evaluate existing results without re-running inference.
 * 
 * @param logEntries Parsed evaluation log entries from previous runs
 * @param evidenceGenerator The original evidence generator (for metadata)
 */
class LogBasedTestCasesGenerator(
  val logEntries: List[EvaluationLogEntry],
  evidenceGenerator: EvidenceGenerator
) extends TestCasesGenerator(evidenceGenerator) {
  
  // Generate test cases once on creation and cache them
  private lazy val cachedTestCases: List[TestCase] = {
    logEntries.map { entry =>
      TestCase(
        evidenceItems = List(entry.contextTestResult.evidenceItem),
        conversations = List.empty,  // We don't have access to original conversations
        contextSize = Some(entry.contextTestResult.contextSize)  // Preserve the exact context size
      )
    }
  }
  
  // Build mapping from test case ID to log entry
  lazy val testCaseIdToLogEntry: Map[String, EvaluationLogEntry] = {
    val idToEntryPairs = cachedTestCases.zip(logEntries).map { case (testCase, logEntry) =>
      (testCase.id, logEntry)
    }
    
    // Validate that all test case IDs are unique
    val duplicateIds = idToEntryPairs.groupBy(_._1).filter(_._2.size > 1).keys.toList
    if (duplicateIds.nonEmpty) {
      System.err.println(s"\n❌ FATAL ERROR: Duplicate test case IDs detected in LogBasedTestCasesGenerator!")
      System.err.println(s"Duplicate IDs: ${duplicateIds.mkString(", ")}")
      duplicateIds.foreach { id =>
        val entries = idToEntryPairs.filter(_._1 == id).map(_._2)
        System.err.println(s"\nID '$id' appears ${entries.size} times:")
        entries.foreach { entry =>
          System.err.println(s"  - Question: '${entry.contextTestResult.evidenceItem.question.take(50)}...'")
          System.err.println(s"    Context size: ${entry.contextTestResult.contextSize}")
          System.err.println(s"    Evidence hash: ${entry.contextTestResult.evidenceItem.hashCode}")
        }
      }
      System.err.println("\nThis is a critical error that will cause incorrect evaluation results.")
      System.err.println("Each test case must have a unique ID.")
      throw new IllegalStateException(s"Duplicate test case IDs found: ${duplicateIds.mkString(", ")}")
    }
    
    idToEntryPairs.toMap
  }
  
  /**
   * Generate test cases from log entries.
   * Returns the cached test cases.
   */
  override def generateTestCases(): List[TestCase] = cachedTestCases
  
  override def generatorClassType: String = "log_based"
  
  override def generatorType: String = s"${evidenceGenerator.getEvidenceTypeName} Generator"
}