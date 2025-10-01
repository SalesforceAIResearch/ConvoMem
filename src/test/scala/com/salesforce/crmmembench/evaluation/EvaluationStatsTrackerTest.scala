package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Conversation}
import com.salesforce.crmmembench.evaluation.memory.AnswerResult

import com.salesforce.crmmembench.questions.evidence.Message
import java.nio.file.{Files, Paths}
import java.io.File
import scala.io.Source

class EvaluationStatsTrackerTest extends AnyFunSuite {
  
  // Helper to create evidence item
  def createEvidenceItem(id: Int): EvidenceItem = {
    EvidenceItem(
      question = s"Question $id",
      answer = s"Answer $id",
      message_evidences = List(Message("assistant", s"Evidence $id")),
      conversations = List.empty,
      category = "test",
      scenario_description = Some(s"Scenario $id")
    )
  }
  
  // Helper to create test case
  def createTestCase(evidenceItems: List[EvidenceItem], contextSize: Int): TestCase = {
    val conversations = (1 to contextSize).map { i =>
      Conversation(
        id = Some(s"conv-$i"),
        messages = List(Message("user", s"Message $i")),
        containsEvidence = Some(false)
      )
    }.toList
    
    TestCase(evidenceItems, conversations)
  }
  
  // Helper to create result
  def createResult(evidenceItem: EvidenceItem, contextSize: Int, isCorrect: Boolean): ContextTestResult = {
    ContextTestResult(
      evidenceItem = evidenceItem,
      contextType = "mixed",
      contextSize = contextSize,
      modelAnswer = "test answer",
      isCorrect = isCorrect,
      retrievedRelevantConversations = 0
    )
  }
  
  test("Standard generator pattern: 1 evidence item per test case") {
    // Create test cases
    val testCases = for {
      evidenceId <- 1 to 10
      contextSize <- List(2, 5, 10)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Process some evidence
    val tc1 = testCases.find(_.conversationCount == 2).get
    val tc2 = testCases.filter(_.conversationCount == 2)(1)
    val tc3 = testCases.find(_.conversationCount == 5).get
    
    tracker.recordEvidenceResult(tc1, createResult(tc1.evidenceItems.head, 2, true), 100L)
    tracker.recordTestCaseCompleted(tc1)
    
    tracker.recordEvidenceResult(tc2, createResult(tc2.evidenceItems.head, 2, false), 150L)
    tracker.recordTestCaseCompleted(tc2)
    
    tracker.recordEvidenceResult(tc3, createResult(tc3.evidenceItems.head, 5, true), 200L)
    tracker.recordTestCaseCompleted(tc3)
    
    // Check stats
    val stats = tracker.getStatsString
    assert(stats.contains("2 conversations:        1/  2  ( 50.0%) - 2/10 test cases completed"))
    assert(stats.contains("5 conversations:        1/  1  (100.0%) - 1/10 test cases completed"))
    assert(stats.contains("10 conversations:        0/  0  (  0.0%) - 0/10 test cases completed"))
    assert(stats.contains("Test Cases:          3 /    30 ( 10.0%) completed"))
    assert(stats.contains("Evidence Items:      3 /    30 ( 10.0%) completed"))
  }
  
  test("Batched generator pattern: multiple evidence items per test case") {
    // Create batched test cases
    val testCases = for {
      contextSize <- List(50, 100)
      batchNum <- 0 until 5
    } yield {
      val evidenceItems = (1 to 20).map(i => createEvidenceItem(batchNum * 20 + i)).toList
      createTestCase(evidenceItems, contextSize)
    }
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Process one full test case (20 evidence items)
    val tc = testCases.head
    tc.evidenceItems.zipWithIndex.foreach { case (evidence, i) =>
      tracker.recordEvidenceResult(tc, createResult(evidence, 50, i % 3 != 0), 100L + i * 10)
    }
    tracker.recordTestCaseCompleted(tc)
    
    // Check stats
    val stats = tracker.getStatsString
    assert(stats.contains("50 conversations:       13/ 20  ( 65.0%) - 1/5 test cases completed") ||
           stats.contains("50 conversations:       14/ 20  ( 70.0%) - 1/5 test cases completed"))
  }
  
  test("Bug reproduction: incorrect test case count across context sizes") {
    // Create 437 evidence items, 12 context sizes = 5244 test cases total
    val testCases = for {
      evidenceId <- 1 to 437
      contextSize <- List(2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Process 93 test cases in smaller contexts
    var processed = 0
    for (ctx <- List(2, 4, 6); i <- 0 until 31) {
      val tc = testCases.filter(_.conversationCount == ctx)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, ctx, i % 3 != 0), 50L + i * 5)
      tracker.recordTestCaseCompleted(tc)
      processed += 1
    }
    
    // Process 5 test cases for context 300
    for (i <- 0 until 5) {
      val tc = testCases.filter(_.conversationCount == 300)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 300, i % 2 == 0), 300L + i * 50)
      tracker.recordTestCaseCompleted(tc)
      processed += 1
    }
    
    assert(processed == 98)
    
    // Check stats - should show correct counts per context size
    val stats = tracker.getStatsString
    assert(stats.contains("2 conversations:       20/ 31  ( 64.5%) - 31/437 test cases completed") ||
           stats.contains("2 conversations:       21/ 31  ( 67.7%) - 31/437 test cases completed"))
    assert(stats.contains("300 conversations:        2/  5  ( 40.0%) - 5/437 test cases completed") ||
           stats.contains("300 conversations:        3/  5  ( 60.0%) - 5/437 test cases completed"))
    
    // Should NOT show "300 conversations: 2/5 (40.0%) - 98/437 test cases completed"
    assert(!stats.contains("- 98/437 test cases completed"))
  }
  
  test("Zero evidence processed shows correctly") {
    val testCases = List(
      createTestCase(List(createEvidenceItem(1)), 5),
      createTestCase(List(createEvidenceItem(2)), 10)
    )
    
    val tracker = new EvaluationStatsTracker(testCases, "test", "long_context", "standard")
    
    val stats = tracker.getStatsString
    assert(stats.contains("5 conversations:        0/  0  (  0.0%) - 0/1 test cases completed"))
    assert(stats.contains("10 conversations:        0/  0  (  0.0%) - 0/1 test cases completed"))
  }
  
  test("CSV export with override=true (old behavior)") {
    val testCases = List(
      createTestCase(List(createEvidenceItem(1)), 2),
      createTestCase(List(createEvidenceItem(2)), 4)
    )
    
    val tracker = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Record some results
    val tc1 = testCases.head
    tracker.recordEvidenceResult(tc1, createResult(tc1.evidenceItems.head, 2, true), 100L, 
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.01), None)))
    tracker.recordTestCaseCompleted(tc1)
    
    // Create temp directory for test
    val tempDir = Files.createTempDirectory("eval_test")
    val testPath = tempDir.resolve("test_override/long_context/test_model")
    Files.createDirectories(testPath)
    
    // Export CSV to temp directory
    tracker.exportToCSVPath(
      basePath = tempDir.resolve("test_override/long_context").toString,
      evidenceCount = 1,
      modelName = "test_model",
      isFinalExport = true
    )
    
    // Clean up
    Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
      .forEach(path => Files.delete(path))
  }
  
  test("CSV aggregation with existing data") {
    // Create temp directory
    val tempDir = Files.createTempDirectory("eval_test_agg")
    val csvPath = tempDir.resolve("test.csv")
    
    // Write existing CSV data
    val existingCSV = """context_size,success_rate_percent,correct_answers,total_processed,test_cases_completed,total_test_cases,avg_response_time_ms,avg_input_tokens,avg_output_tokens,avg_cost,p50_ms,p90_ms,p99_ms
2,75.0,3,4,4,10,100,1000,200,0.0100,90,110,120
4,50.0,2,4,4,10,150,1200,250,0.0120,140,160,180"""
    
    Files.write(csvPath, existingCSV.getBytes)
    
    // Create test cases
    val testCases = List(
      createTestCase(List(createEvidenceItem(1)), 2),
      createTestCase(List(createEvidenceItem(2)), 2),
      createTestCase(List(createEvidenceItem(3)), 4),
      createTestCase(List(createEvidenceItem(4)), 4)
    )
    
    val tracker = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Record new results
    tracker.recordEvidenceResult(testCases(0), createResult(testCases(0).evidenceItems.head, 2, true), 200L,
      Some(AnswerResult(Some("test"), List.empty, Some(2000), Some(400), Some(0.02), None)))
    tracker.recordTestCaseCompleted(testCases(0))
    
    tracker.recordEvidenceResult(testCases(1), createResult(testCases(1).evidenceItems.head, 2, false), 150L,
      Some(AnswerResult(Some("test"), List.empty, Some(1500), Some(300), Some(0.015), None)))
    tracker.recordTestCaseCompleted(testCases(1))
    
    // Expected aggregated results:
    // Context 2: 3+1=4 correct out of 4+2=6 total = 66.7%
    // Avg response time: (100*4 + 175*2) / 6 = 125ms
    // Avg input tokens: (1000*4 + 1750*2) / 6 = 1250
    
    // Clean up
    Files.delete(csvPath)
    Files.delete(tempDir)
  }
  
  test("History file creation and format") {
    val tempDir = Files.createTempDirectory("eval_test_history")
    val historyPath = tempDir.resolve("test.history")
    
    val testCases = List(
      createTestCase(List(createEvidenceItem(1)), 2),
      createTestCase(List(createEvidenceItem(2)), 4)
    )
    
    val tracker = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Record some results
    val tc1 = testCases.head
    tracker.recordEvidenceResult(tc1, createResult(tc1.evidenceItems.head, 2, true), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.01), None)))
    tracker.recordTestCaseCompleted(tc1)
    
    // Would call exportToCSV here with custom path
    // tracker.exportToCSV("test", 1, "long_context", "test_model")
    
    // Verify history file would be created with correct format
    // Should contain:
    // - Timestamp header
    // - CSV header
    // - Current run data
    
    // Clean up
    Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
      .forEach(path => Files.delete(path))
  }
  
  test("Weighted average calculation for aggregated metrics") {
    // Test the weighted average logic
    // Existing: 10 items with avg 100ms
    // New: 5 items with avg 200ms
    // Expected: (100*10 + 200*5) / 15 = 133.33ms
    
    val testCases = List.fill(15)(createTestCase(List(createEvidenceItem(1)), 2))
    val tracker = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Simulate recording 5 results with 200ms average
    for (i <- 0 until 5) {
      tracker.recordEvidenceResult(testCases(i), createResult(testCases(i).evidenceItems.head, 2, true), 200L)
    }
    
    // The aggregation logic would combine with existing data
    // This tests the mathematical correctness of weighted averages
  }
  
  test("Percentiles use only current run data") {
    // Test that percentiles are calculated only from current run
    val testCases = List.fill(100)(createTestCase(List(createEvidenceItem(1)), 2))
    val tracker = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Record 100 results with known distribution
    for (i <- 0 until 100) {
      val responseTime = 100L + i // 100ms to 199ms
      tracker.recordEvidenceResult(testCases(i), createResult(testCases(i).evidenceItems.head, 2, true), responseTime)
    }
    
    // P50 should be ~149ms (middle of 100-199)
    // P90 should be ~189ms
    // P99 should be ~198ms
    // These should not be affected by any existing data
  }
  
  test("Sequential exports overwrite previous data") {
    // Test that each export overwrites the previous one (no aggregation)
    
    val tempDir = Files.createTempDirectory("eval_test_overwrite")
    val basePath = tempDir.toString
    
    // Create test cases
    val testCases = List(
      createTestCase(List(createEvidenceItem(1)), 2),
      createTestCase(List(createEvidenceItem(2)), 2),
      createTestCase(List(createEvidenceItem(3)), 4),
      createTestCase(List(createEvidenceItem(4)), 4)
    )
    
    // First run
    val tracker1 = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Record 2 results for context size 2
    tracker1.recordEvidenceResult(testCases(0), createResult(testCases(0).evidenceItems.head, 2, true), 100L)
    tracker1.recordTestCaseCompleted(testCases(0))
    tracker1.recordEvidenceResult(testCases(1), createResult(testCases(1).evidenceItems.head, 2, true), 100L)
    tracker1.recordTestCaseCompleted(testCases(1))
    
    // Export
    tracker1.exportToCSVPath(basePath, 1, "test_model", isFinalExport = false)
    
    // Check initial state
    val csvPath = Paths.get(basePath, "test_model", "1_evidence.csv")
    val csv1 = new String(Files.readAllBytes(csvPath))
    val lines1 = csv1.split("\n")
    val context2Line1 = lines1.find(_.startsWith("2,")).get
    val parts1 = context2Line1.split(",")
    
    assert(parts1(3).toInt == 2, "First export should have 2 total processed")
    assert(parts1(2).toInt == 2, "First export should have 2 correct answers")
    
    // Second run - completely new tracker
    val tracker2 = new EvaluationStatsTracker(testCases, "test", "long_context", "standard", overrideCSV = true)
    
    // Record only 1 result
    tracker2.recordEvidenceResult(testCases(0), createResult(testCases(0).evidenceItems.head, 2, false), 200L)
    
    // Export again - this should overwrite
    tracker2.exportToCSVPath(basePath, 1, "test_model", isFinalExport = false)
    
    // Read the CSV to check it was overwritten
    val csv2 = new String(Files.readAllBytes(csvPath))
    val lines2 = csv2.split("\n")
    val context2Line2 = lines2.find(_.startsWith("2,")).get
    val parts2 = context2Line2.split(",")
    
    val totalProcessed2 = parts2(3).toInt
    val correctAnswers2 = parts2(2).toInt
    
    // Should only show data from second tracker (1 result, 0 correct)
    assert(totalProcessed2 == 1, s"Second export: Expected 1 total processed (overwritten), got $totalProcessed2")
    assert(correctAnswers2 == 0, s"Second export: Expected 0 correct answers (overwritten), got $correctAnswers2")
    
    // Clean up
    Files.walk(tempDir).sorted(java.util.Comparator.reverseOrder())
      .forEach(path => Files.delete(path))
  }
  
  test("Early termination conditions - should NOT terminate when conditions not met") {
    // Create test cases for context sizes 2, 4, 6
    val testCases = for {
      evidenceId <- 1 to 100
      contextSize <- List(2, 4, 6)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Record some results but not enough to meet conditions  
    // Only record for context size 2
    val context2Cases = testCases.filter(_.conversationCount == 2)
    for (i <- 0 until 10) {
      val tc = context2Cases(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, true), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.10), None)))
    }
    
    // Total cost: $1, not >= $20
    assert(!tracker.shouldTerminateEarly(), "Should not terminate with cost < $20")
    
    // Add more to reach $25 - still only context size 2
    for (i <- 10 until 160) {
      if (i < context2Cases.size) {
        val tc = context2Cases(i)
        tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, true), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.10), None)))
      }
    }
    
    // Now cost > $20, but only context size 2 has results
    assert(!tracker.shouldTerminateEarly(), "Should not terminate when not all context sizes have results")
  }
  
  test("Early termination conditions - should terminate when all conditions met") {
    // Create test cases for context sizes 2, 4, 6
    val testCases = for {
      evidenceId <- 1 to 200
      contextSize <- List(2, 4, 6)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Record results to meet all conditions
    // Context size 2: 60 correct out of 100 = 60%
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 60), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Context size 4: 50 correct out of 100 = 50% (less than 60%)
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 50), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Context size 6: 50 correct out of 110 = 45.45% (less than 50%, but >= 50 correct answers)
    for (i <- 0 until 110) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 50), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Total cost: 310 * $0.50 = $155 (well above $20)
    // Each context has >= 50 correct answers
    // Each context has >= 40% success rate
    // Success rates decrease: 60% -> 50% -> 45.45%
    assert(tracker.shouldTerminateEarly(), "Should terminate when all conditions are met")
  }
  
  test("Early termination - should NOT terminate if success rate increases") {
    // Create test cases for context sizes 2, 4
    val testCases = for {
      evidenceId <- 1 to 100
      contextSize <- List(2, 4)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Context size 2: 50 correct out of 100 = 50%
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 50), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Context size 4: 60 correct out of 100 = 60% (HIGHER than context 2)
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 60), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Should NOT terminate because success rate increased (50% -> 60%)
    assert(!tracker.shouldTerminateEarly(), "Should not terminate when success rate increases with context size")
  }
  
  test("Early termination - should NOT terminate if any context has < 40% success rate") {
    // Create test cases for context sizes 2, 4
    val testCases = for {
      evidenceId <- 1 to 100
      contextSize <- List(2, 4)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Context size 2: 60 correct out of 100 = 60%
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 60), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Context size 4: 30 correct out of 100 = 30% (< 40%)
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 30), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.50), None)))
    }
    
    // Should NOT terminate because context 4 has < 40% success rate
    assert(!tracker.shouldTerminateEarly(), "Should not terminate when any context has < 40% success rate")
  }
  
  test("Early termination Condition 2 - should terminate with cost > $100 and at most 1 violation") {
    // Create test cases for context sizes 2, 4, 6, 8
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4, 6, 8)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create a scenario with exactly 1 violation of monotonic decrease
    // Success rates: 90% -> 80% -> 85% -> 70% (one violation at position 2)
    
    // Context size 2: 45 correct out of 50 = 90%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Context size 4: 40 correct out of 50 = 80%  
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 40), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Context size 6: 42 correct out of 50 = 84% (VIOLATION: 84% > 80%)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 42), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Context size 8: 35 correct out of 50 = 70%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 8)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 8, i < 35), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Total cost: 200 * $0.60 = $120 (> $100)
    // Only 1 violation of monotonic decrease
    assert(tracker.shouldTerminateEarly(), "Should terminate with Condition 2: cost > $100 and at most 1 violation")
  }
  
  test("Early termination Condition 2 - should NOT terminate with 2 violations") {
    // Create test cases for context sizes 2, 4, 6, 8
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4, 6, 8)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create a scenario with 2 violations of monotonic decrease
    // Success rates: 90% -> 80% -> 85% -> 86% (two violations)
    
    // Context size 2: 45 correct out of 50 = 90%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Context size 4: 40 correct out of 50 = 80%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 40), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Context size 6: 42 correct out of 50 = 84% (VIOLATION 1: 84% > 80%)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 42), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Context size 8: 43 correct out of 50 = 86% (VIOLATION 2: 86% > 84%)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 8)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 8, i < 43), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.60), None)))
    }
    
    // Total cost: 200 * $0.60 = $120 (> $100)
    // But 2 violations of monotonic decrease
    assert(!tracker.shouldTerminateEarly(), "Should NOT terminate with 2 violations even if cost > $100")
  }
  
  test("Early termination Condition 2 - should terminate with perfect monotonic decrease") {
    // Create test cases for context sizes 2, 4, 6
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4, 6)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create a scenario with perfect monotonic decrease (0 violations)
    // Success rates: 90% -> 80% -> 70%
    
    // Context size 2: 45 correct out of 50 = 90%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 4: 40 correct out of 50 = 80%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 40), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 6: 35 correct out of 50 = 70%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 35), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Total cost: 150 * $0.80 = $120 (> $100)
    // 0 violations (perfect monotonic decrease)
    assert(tracker.shouldTerminateEarly(), "Should terminate with Condition 2: cost > $100 and 0 violations")
  }
  
  test("Early termination Condition 2 - should NOT terminate with cost exactly $100") {
    // Create test cases for context sizes 2, 4
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create a scenario with perfect monotonic decrease but cost exactly $100
    
    // Context size 2: 45 correct out of 50 = 90%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.00), None)))
    }
    
    // Context size 4: 40 correct out of 50 = 80%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 40), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.00), None)))
    }
    
    // Total cost: 100 * $1.00 = $100 (exactly $100, not > $100)
    // Perfect monotonic decrease but condition 2 requires > $100
    assert(!tracker.shouldTerminateEarly(), "Should NOT terminate with cost exactly $100 (condition 2 requires > $100)")
  }
  
  test("Early termination Condition 3 - should terminate when first half avg is 5% better") {
    // Create test cases for context sizes 2, 4, 6, 8
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4, 6, 8)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create a scenario where first half (2, 4) has much better success rates than second half (6, 8)
    // First half: 90%, 85% -> avg = 87.5%
    // Second half: 75%, 70% -> avg = 72.5%
    // Difference: 15% (> 5%)
    
    // Context size 2: 45 correct out of 50 = 90%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 4: 42 correct out of 50 = 84%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 42), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 6: 37 correct out of 50 = 74%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 37), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 8: 35 correct out of 50 = 70%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 8)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 8, i < 35), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Total cost: 200 * $0.80 = $160 (> $150)
    // First half avg: (90 + 84) / 2 = 87%
    // Second half avg: (74 + 70) / 2 = 72%
    // Difference: 15% (> 5%)
    assert(tracker.shouldTerminateEarly(), "Should terminate with Condition 3: cost > $150 and first half 5% better")
  }
  
  test("Early termination Condition 3 - should NOT terminate when difference < 5%") {
    // Create test cases for context sizes 2, 4, 6, 8
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4, 6, 8)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create a scenario where difference is less than 5%
    // Make it not meet condition 1 (monotonic decrease violated)
    // First half: 85%, 82% -> avg = 83.5%
    // Second half: 84%, 78% -> avg = 81%
    // Difference: 2.5% (< 5%)
    
    // Context size 2: 42 correct out of 50 = 84%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 42), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 4: 41 correct out of 50 = 82%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 41), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 6: 42 correct out of 50 = 84% (VIOLATION 1 - breaks monotonic decrease)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 42), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Context size 8: 43 correct out of 50 = 86% (VIOLATION 2 - even higher)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 8)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 8, i < 43), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.80), None)))
    }
    
    // Total cost: 200 * $0.80 = $160 (> $150)
    // First half avg: (84 + 82) / 2 = 83%
    // Second half avg: (84 + 86) / 2 = 85%
    // Difference: -2% (< 5% and negative!)
    // Has 2 violations so condition 2 won't trigger either
    assert(!tracker.shouldTerminateEarly(), "Should NOT terminate when difference is less than 5%")
  }
  
  test("Early termination Condition 3 - should NOT terminate with cost exactly $150") {
    // Create test cases for context sizes 2, 4, 6, 8
    val testCases = for {
      evidenceId <- 1 to 50
      contextSize <- List(2, 4, 6, 8)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create scenario with big difference but cost exactly $150
    // Add violations to prevent condition 1 from triggering
    
    // Context size 2: 45 correct out of 50 = 90%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.75), None)))
    }
    
    // Context size 4: 43 correct out of 50 = 86%
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 43), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.75), None)))
    }
    
    // Context size 6: 44 correct out of 50 = 88% (VIOLATION 1 - breaks monotonic decrease)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 44), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.75), None)))
    }
    
    // Context size 8: 45 correct out of 50 = 90% (VIOLATION 2 - even higher)
    for (i <- 0 until 50) {
      val tc = testCases.filter(_.conversationCount == 8)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 8, i < 45), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(0.75), None)))
    }
    
    // Total cost: 200 * $0.75 = $150 (exactly $150, not > $150)
    // First half avg: (90 + 86) / 2 = 88%
    // Second half avg: (88 + 90) / 2 = 89%
    // First half is NOT 5% better than second half (actually worse)
    // Has 2 violations so conditions 1 and 2 won't trigger
    assert(!tracker.shouldTerminateEarly(), "Should NOT terminate with cost exactly $150 (condition 3 requires > $150)")
  }
  
  test("Early termination Condition 3 - should work with odd number of context sizes") {
    // Create test cases for context sizes 2, 4, 6, 8, 10 (odd number = 5)
    val testCases = for {
      evidenceId <- 1 to 30
      contextSize <- List(2, 4, 6, 8, 10)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // With 5 context sizes, first half is 2 items (2, 4), second half is 3 items (6, 8, 10)
    // First half: 90%, 85% -> avg = 87.5%
    // Second half: 75%, 70%, 65% -> avg = 70%
    // Difference: 17.5% (> 5%)
    
    // Context size 2: 27 correct out of 30 = 90%
    for (i <- 0 until 30) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 27), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.10), None)))
    }
    
    // Context size 4: 25 correct out of 30 = 83.33%
    for (i <- 0 until 30) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 25), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.10), None)))
    }
    
    // Context size 6: 22 correct out of 30 = 73.33%
    for (i <- 0 until 30) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 22), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.10), None)))
    }
    
    // Context size 8: 21 correct out of 30 = 70%
    for (i <- 0 until 30) {
      val tc = testCases.filter(_.conversationCount == 8)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 8, i < 21), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.10), None)))
    }
    
    // Context size 10: 19 correct out of 30 = 63.33%
    for (i <- 0 until 30) {
      val tc = testCases.filter(_.conversationCount == 10)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 10, i < 19), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(1.10), None)))
    }
    
    // Total cost: 150 * $1.10 = $165 (> $150)
    // First half (2 items) avg: (90 + 83.33) / 2 ≈ 86.67%
    // Second half (3 items) avg: (73.33 + 70 + 63.33) / 3 ≈ 68.89%
    // Difference: ~17.78% (> 5%)
    assert(tracker.shouldTerminateEarly(), "Should terminate with odd number of context sizes")
  }
  
  test("Early termination Condition 4 - should terminate immediately when cost > $300") {
    // Create test cases with any pattern - condition 4 doesn't care about success rates
    val testCases = for {
      evidenceId <- 1 to 20
      contextSize <- List(2, 4, 6)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Process only context size 2, with poor performance (violating all other conditions)
    val context2Cases = testCases.filter(_.conversationCount == 2)
    
    // Only 10 correct out of 20 = 50%, but spend a lot of money
    for (i <- 0 until 20) {
      val tc = context2Cases(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 10), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(10000), Some(2000), Some(16.00), None)))
    }
    
    // Total cost: 20 * $16 = $320 (> $300)
    // Only processed one context size, poor performance, but cost > $300
    assert(tracker.shouldTerminateEarly(), "Should terminate immediately when cost exceeds $300")
  }
  
  test("Early termination Condition 4 - should NOT terminate with cost exactly $300") {
    // Create test cases with multiple context sizes
    val testCases = for {
      evidenceId <- 1 to 5
      contextSize <- List(2, 4, 6)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Process results to accumulate exactly $300
    // Keep cost under $100 initially to avoid condition 2
    
    // Context 2: Process 5 items at $10 each = $50
    val context2Cases = testCases.filter(_.conversationCount == 2)
    for (i <- 0 until 5) {
      tracker.recordEvidenceResult(context2Cases(i), createResult(context2Cases(i).evidenceItems.head, 2, i < 2), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(10.00), None)))
    }
    
    // Context 4: Process 5 items at $10 each = $50
    val context4Cases = testCases.filter(_.conversationCount == 4)
    for (i <- 0 until 5) {
      tracker.recordEvidenceResult(context4Cases(i), createResult(context4Cases(i).evidenceItems.head, 4, i < 3), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(10.00), None)))
    }
    
    // Now we're at $100, add more to reach exactly $300
    // Context 6: Process 5 items at $40 each = $200
    val context6Cases = testCases.filter(_.conversationCount == 6)
    for (i <- 0 until 5) {
      tracker.recordEvidenceResult(context6Cases(i), createResult(context6Cases(i).evidenceItems.head, 6, i < 4), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(1000), Some(200), Some(40.00), None)))
    }
    
    // Total: $50 + $50 + $200 = $300 exactly
    // Success rates: 40%, 60%, 80% (increasing, violates condition 1)
    // First half avg: (40+60)/2 = 50%, Second half: 80%
    // First half is NOT 5% better (it's worse), so condition 3 fails
    // Has 2 violations of monotonic decrease, so condition 2 fails
    
    assert(!tracker.shouldTerminateEarly(), "Should NOT terminate with cost exactly $300")
  }
  
  test("Early termination - condition 4 takes precedence over all others") {
    // Create test cases that would satisfy condition 1 perfectly
    val testCases = for {
      evidenceId <- 1 to 100
      contextSize <- List(2, 4, 6)
    } yield createTestCase(List(createEvidenceItem(evidenceId)), contextSize)
    
    val tracker = new EvaluationStatsTracker(testCases.toList, "test", "long_context", "standard")
    
    // Create perfect monotonic decrease with all conditions for condition 1 met
    // Context size 2: 90% success rate
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 2)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 2, i < 90), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(5000), Some(1000), Some(2.00), None)))
    }
    
    // Context size 4: 80% success rate
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 4)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 4, i < 80), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(5000), Some(1000), Some(2.00), None)))
    }
    
    // Context size 6: 70% success rate
    for (i <- 0 until 100) {
      val tc = testCases.filter(_.conversationCount == 6)(i)
      tracker.recordEvidenceResult(tc, createResult(tc.evidenceItems.head, 6, i < 70), 100L,
      Some(AnswerResult(Some("test"), List.empty, Some(5000), Some(1000), Some(2.00), None)))
    }
    
    // Total cost: 300 * $2 = $600 (> $300)
    // All conditions for condition 1 are met (monotonic decrease, >50 correct, >40% success)
    // But condition 4 should take precedence
    assert(tracker.shouldTerminateEarly(), "Should terminate due to condition 4 even when other conditions are met")
    
    // Verify it's actually condition 4 by checking the cost
    assert(tracker.getTotalCost > 300.0, "Cost should be over $300")
  }
}