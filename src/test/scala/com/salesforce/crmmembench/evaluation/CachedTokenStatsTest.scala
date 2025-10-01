package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.evaluation.memory.AnswerResult
import com.salesforce.crmmembench.questions.evidence.{Conversation, Message, EvidenceItem}
import java.nio.file._

class CachedTokenStatsTest extends AnyFunSuite {
  
  test("EvaluationStatsTracker should track cached token stats") {
    // Create test data
    val evidenceItem = EvidenceItem(
      question = "What is the sky color?",
      answer = "blue",
      message_evidences = List(Message("User", "The sky is blue")),
      conversations = List.empty,
      category = "test",
      scenario_description = Some("Test scenario")
    )
    
    val testCase = TestCase(
      evidenceItems = List(evidenceItem),
      conversations = List.empty
    )
    
    val testCases = List(testCase)
    
    // Create stats tracker
    val statsTracker = new EvaluationStatsTracker(testCases, "test_case_type", "long_context", "standard")
    
    // Record a result with cached tokens
    val result = ContextTestResult(
      evidenceItem = evidenceItem,
      contextType = "test_context",
      contextSize = 10,
      modelAnswer = "blue",
      isCorrect = true,
      retrievedRelevantConversations = 1
    )
    
    statsTracker.recordEvidenceResult(
      testCase = testCase,
      result = result,
      responseTimeMs = 100L,
      answerResult = Some(AnswerResult(
        answer = Some("blue"),
        retrievedConversationIds = List.empty,
        inputTokens = Some(1000),
        outputTokens = Some(200),
        cost = Some(0.0015),
        cachedInputTokens = Some(800) // 80% cached
      ))
    )
    
    // Get stats string and verify it contains cached token info
    val statsString = statsTracker.getStatsString
    assert(statsString.contains("800 cached"))
    assert(statsString.contains("80.0%"))
    
    // Test exporting to CSV
    val tempDir = Files.createTempDirectory("test_cached_tokens")
    try {
      // Use exportToCSVPath to write to temp directory instead of main resources
      statsTracker.exportToCSVPath(
        basePath = tempDir.toString,
        evidenceCount = 1,
        modelName = "test-model",
        isFinalExport = true
      )
      
      val csvPath = tempDir.resolve("test_model/1_evidence.csv")
      // Verify the CSV was created in the temp directory
      assert(Files.exists(csvPath), s"CSV file should exist at $csvPath")
    } finally {
      // Clean up temp directory
      import java.nio.file._
      import java.nio.file.attribute.BasicFileAttributes
      
      Files.walkFileTree(tempDir, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(dir: Path, exc: java.io.IOException): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }
  }
  
  test("EvaluationStatsTracker should handle results without cached tokens") {
    val evidenceItem = EvidenceItem(
      question = "What is 2+2?",
      answer = "4",
      message_evidences = List(Message("User", "2+2 equals 4")),
      conversations = List.empty,
      category = "test",
      scenario_description = Some("Test scenario")
    )
    
    val testCase = TestCase(
      evidenceItems = List(evidenceItem),
      conversations = List.empty
    )
    
    val testCases = List(testCase)
    val statsTracker = new EvaluationStatsTracker(testCases, "test_case_type", "long_context", "standard")
    
    val result = ContextTestResult(
      evidenceItem = evidenceItem,
      contextType = "test_context",
      contextSize = 5,
      modelAnswer = "4",
      isCorrect = true,
      retrievedRelevantConversations = 1
    )
    
    // Record without cached tokens
    statsTracker.recordEvidenceResult(
      testCase = testCase,
      result = result,
      responseTimeMs = 150L,
      answerResult = Some(AnswerResult(
        answer = Some("blue"),
        retrievedConversationIds = List.empty,
        inputTokens = Some(500),
        outputTokens = Some(100),
        cost = Some(0.001),
        cachedInputTokens = None
      ))
    )
    
    // Get stats string and verify it doesn't contain cached token info
    val statsString = statsTracker.getStatsString
    assert(!statsString.contains("cached"))
  }
  
  test("CSV export should include cached token columns") {
    val evidenceItem = EvidenceItem(
      question = "Test question",
      answer = "Test answer",
      message_evidences = List(Message("User", "Test message")),
      conversations = List.empty,
      category = "test",
      scenario_description = Some("Test scenario")
    )
    
    val testCase = TestCase(
      evidenceItems = List(evidenceItem),
      conversations = List.empty
    )
    
    val testCases = List(testCase)
    val statsTracker = new EvaluationStatsTracker(testCases, "test_case_type", "long_context", "standard", overrideCSV = true)
    
    // Record multiple results with varying cached tokens
    for (i <- 1 to 5) {
      val result = ContextTestResult(
        evidenceItem = evidenceItem,
        contextType = "test_context",
        contextSize = 10,
        modelAnswer = "Test answer",
        isCorrect = true,
        retrievedRelevantConversations = 1
      )
      
      statsTracker.recordEvidenceResult(
        testCase = testCase,
        result = result,
        responseTimeMs = 100L + i * 10,
        answerResult = Some(AnswerResult(
          answer = Some("test"),
          retrievedConversationIds = List.empty,
          inputTokens = Some(1000),
          outputTokens = Some(200),
          cost = Some(0.002),
          cachedInputTokens = Some(i * 100) // Varying cached tokens
        ))
      )
    }
    
    statsTracker.recordTestCaseCompleted(testCase)
    
    // Verify stats show average cached tokens
    val statsString = statsTracker.getStatsString
    println(statsString) // For debugging
    
    // Average cached tokens should be (100+200+300+400+500)/5 = 300
    assert(statsString.contains("300 cached"))
    // Cache ratio should be 300/1000 * 100 = 30%
    assert(statsString.contains("30.0%"))
  }
}