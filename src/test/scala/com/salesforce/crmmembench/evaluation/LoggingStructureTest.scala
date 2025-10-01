package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterEach
import java.io.File
import java.nio.file.{Files, Path, Paths}
import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Message, Conversation}
import com.salesforce.crmmembench.evaluation.memory.AnswerResult
import scala.io.Source
import io.circe._
import io.circe.parser._

/**
 * Test to verify the new logging directory structure.
 */
class LoggingStructureTest extends AnyFunSuite with BeforeAndAfterEach {
  
  // Use a separate test directory to avoid polluting production logs
  val testLogsDir = "test-logs/evaluations"
  val originalLogsDir = "logs/evaluations"
  
  override def beforeEach(): Unit = {
    // Clean up test logs directory if it exists
    deleteDirectory(new File("test-logs"))
    
    // Temporarily redirect EvaluationLogger to use test directory
    EvaluationLogger.setBaseLogsDir("test-logs/evaluations")
  }
  
  override def afterEach(): Unit = {
    // Reset logger state
    if (EvaluationLogger.isInitialized) {
      EvaluationLogger.finalizeRun()
    }
    
    // Reset to default logs directory
    EvaluationLogger.resetBaseLogsDir()
    
    // Clean up test logs directory after test
    deleteDirectory(new File("test-logs"))
  }
  
  def deleteDirectory(dir: File): Unit = {
    if (dir.exists()) {
      dir.listFiles().foreach { file =>
        if (file.isDirectory) {
          deleteDirectory(file)
        } else {
          file.delete()
        }
      }
      dir.delete()
    }
  }
  
  test("EvaluationLogger creates correct directory structure") {
    // Initialize the logger
    val runId = EvaluationLogger.initializeRun(
      caseType = "user_facts",
      memorySystem = "long_context", 
      modelName = "gemini-2.5-flash",
      evidenceCount = 3
    )
    
    // Verify timestamp format (should be YYYY-MM-DD_HH-mm-ss)
    assert(runId.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"))
    
    // Verify directory structure exists
    val expectedPath = s"test-logs/evaluations/user_facts/long_context/gemini_2_5_flash/3_evidence/$runId"
    val runDir = new File(expectedPath)
    assert(runDir.exists(), s"Expected directory $expectedPath to exist")
    assert(runDir.isDirectory(), s"Expected $expectedPath to be a directory")
    
    // Verify log files exist
    val correctFile = new File(runDir, "correct_responses.json")
    val incorrectFile = new File(runDir, "incorrect_responses.json")
    assert(correctFile.exists(), "correct_responses.json should exist")
    assert(incorrectFile.exists(), "incorrect_responses.json should exist")
    
    EvaluationLogger.finalizeRun()
  }
  
  test("EvaluationLogger sanitizes model names correctly") {
    // Test with a model name that has special characters
    val runId = EvaluationLogger.initializeRun(
      caseType = "abstention",
      memorySystem = "mem0",
      modelName = "claude-3.5-sonnet@20241022",
      evidenceCount = 1
    )
    
    // Verify sanitized model name in path
    val expectedPath = s"test-logs/evaluations/abstention/mem0/claude_3_5_sonnet_20241022/1_evidence/$runId"
    val runDir = new File(expectedPath)
    assert(runDir.exists(), s"Expected directory $expectedPath to exist")
    
    EvaluationLogger.finalizeRun()
  }
  
  test("EvaluationLogger logs entries with memory system") {
    // Initialize the logger
    val runId = EvaluationLogger.initializeRun(
      caseType = "user_facts",
      memorySystem = "long_context",
      modelName = "test-model",
      evidenceCount = 2
    )
    
    // Create test data
    val evidenceItem = EvidenceItem(
      question = "What is my favorite color?",
      answer = "Blue",
      message_evidences = List(
        Message(speaker = "User", text = "My favorite color is blue")
      ),
      conversations = List.empty,
      category = "test",
      scenario_description = Some("Test scenario")
    )
    
    val correctResult = ContextTestResult(
      evidenceItem = evidenceItem,
      modelAnswer = "Blue",
      isCorrect = true,
      contextType = "mixed",
      contextSize = 10,
      retrievedRelevantConversations = 1
    )
    
    val incorrectResult = ContextTestResult(
      evidenceItem = evidenceItem,
      modelAnswer = "Red",
      isCorrect = false,
      contextType = "mixed",
      contextSize = 10,
      retrievedRelevantConversations = 0
    )
    
    // Create answer results
    val correctAnswerResult = AnswerResult(
      answer = Some("Blue"),
      retrievedConversationIds = List("conv1"),
      inputTokens = Some(1000),
      outputTokens = Some(50),
      cost = Some(0.0015),
      cachedInputTokens = Some(800),
      memorySystemResponses = List("""{"memories": [{"memory": "User's favorite color is blue", "run_id": "conv1"}]}""")
    )
    
    val incorrectAnswerResult = AnswerResult(
      answer = Some("Red"),
      retrievedConversationIds = List.empty,
      inputTokens = Some(1200),
      outputTokens = Some(45),
      cost = Some(0.0018),
      cachedInputTokens = Some(600),
      memorySystemResponses = List.empty
    )
    
    // Log results
    EvaluationLogger.logResult(correctResult, "user_facts", "long_context", "batched", 150L, Some(correctAnswerResult))
    EvaluationLogger.logResult(incorrectResult, "user_facts", "long_context", "batched", 200L, Some(incorrectAnswerResult))
    
    // Finalize to close files
    EvaluationLogger.finalizeRun()
    
    // Read and verify the logged data
    val expectedPath = s"test-logs/evaluations/user_facts/long_context/test_model/2_evidence/$runId"
    val correctFile = new File(expectedPath, "correct_responses.json")
    val incorrectFile = new File(expectedPath, "incorrect_responses.json")
    
    // Parse correct responses
    val correctContent = Source.fromFile(correctFile).mkString
    val correctJson = parse(correctContent).getOrElse(Json.Null)
    assert(correctJson.isArray)
    val correctArray = correctJson.asArray.get
    assert(correctArray.size == 1)
    
    val correctEntry = correctArray.head
    
    // Check top-level fields
    val cursor = correctEntry.hcursor
    assert(cursor.downField("memorySystem").as[String].getOrElse("") == "long_context")
    assert(cursor.downField("testCaseGeneratorType").as[String].getOrElse("") == "batched")
    assert(cursor.downField("evidenceType").as[String].getOrElse("") == "user_facts")
    assert(cursor.downField("responseTimeMs").as[Long].getOrElse(0L) == 150L)
    
    // Check contextTestResult nested fields
    val contextTestResultCursor = cursor.downField("contextTestResult")
    assert(contextTestResultCursor.downField("modelAnswer").as[String].getOrElse("") == "Blue")
    assert(contextTestResultCursor.downField("isCorrect").as[Boolean].getOrElse(false) == true)
    assert(contextTestResultCursor.downField("contextType").as[String].getOrElse("") == "mixed")
    assert(contextTestResultCursor.downField("contextSize").as[Int].getOrElse(0) == 10)
    assert(contextTestResultCursor.downField("retrievedRelevantConversations").as[Int].getOrElse(0) == 1)
    
    // Check evidenceItem inside contextTestResult
    val evidenceItemCursor = contextTestResultCursor.downField("evidenceItem")
    assert(evidenceItemCursor.downField("question").as[String].getOrElse("") == "What is my favorite color?")
    assert(evidenceItemCursor.downField("answer").as[String].getOrElse("") == "Blue")
    assert(evidenceItemCursor.downField("category").as[String].getOrElse("") == "test")
    assert(evidenceItemCursor.downField("scenario_description").as[Option[String]].isRight)
    
    // Check answerResult nested fields - Circe handles Option properly
    val answerResultOpt = cursor.downField("answerResult").as[Option[Json]].getOrElse(None)
    assert(answerResultOpt.isDefined)
    
    answerResultOpt.foreach { answerResultJson =>
      val answerCursor = answerResultJson.hcursor
      assert(answerCursor.downField("answer").as[Option[String]].isRight)
      assert(answerCursor.downField("retrievedConversationIds").as[List[String]].isRight)
      assert(answerCursor.downField("inputTokens").as[Option[Int]].isRight)
      assert(answerCursor.downField("outputTokens").as[Option[Int]].isRight)
      assert(answerCursor.downField("cachedInputTokens").as[Option[Int]].isRight)
      assert(answerCursor.downField("cost").as[Option[Double]].isRight)
      
      // Check memorySystemResponses
      val memoryResponses = answerCursor.downField("memorySystemResponses").as[List[String]].getOrElse(List.empty)
      assert(memoryResponses.size == 1)
      assert(memoryResponses.head.contains("favorite color is blue"))
    }
    
    // Parse incorrect responses
    val incorrectContent = Source.fromFile(incorrectFile).mkString
    val incorrectJson = parse(incorrectContent).getOrElse(Json.Null)
    assert(incorrectJson.isArray)
    val incorrectArray = incorrectJson.asArray.get
    assert(incorrectArray.size == 1)
    
    val incorrectEntry = incorrectArray.head
    
    // Check top-level fields
    val incorrectCursor = incorrectEntry.hcursor
    assert(incorrectCursor.downField("memorySystem").as[String].getOrElse("") == "long_context")
    assert(incorrectCursor.downField("testCaseGeneratorType").as[String].getOrElse("") == "batched")
    assert(incorrectCursor.downField("evidenceType").as[String].getOrElse("") == "user_facts")
    assert(incorrectCursor.downField("responseTimeMs").as[Long].getOrElse(0L) == 200L)
    
    // Check contextTestResult nested fields
    val contextTestResultCursor2 = incorrectCursor.downField("contextTestResult")
    assert(contextTestResultCursor2.downField("modelAnswer").as[String].getOrElse("") == "Red")
    assert(contextTestResultCursor2.downField("isCorrect").as[Boolean].getOrElse(true) == false)
    
    // Check answerResult nested fields - Circe handles Option properly
    val answerResultOpt2 = incorrectCursor.downField("answerResult").as[Option[Json]].getOrElse(None)
    assert(answerResultOpt2.isDefined)
    
    answerResultOpt2.foreach { answerResultJson =>
      val answerCursor = answerResultJson.hcursor
      assert(answerCursor.downField("inputTokens").as[Option[Int]].isRight)
      assert(answerCursor.downField("outputTokens").as[Option[Int]].isRight)
      assert(answerCursor.downField("cachedInputTokens").as[Option[Int]].isRight)
      assert(answerCursor.downField("cost").as[Option[Double]].isRight)
      
      // Check memorySystemResponses field for incorrect response (should be empty)
      val memoryResponses = answerCursor.downField("memorySystemResponses").as[List[String]].getOrElse(List.empty)
      assert(memoryResponses.isEmpty)
    }
  }
}