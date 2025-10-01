package com.salesforce.crmmembench.evaluation.memory

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse}
import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import scala.util.{Try, Success}

/**
 * Test for BlockBasedMemoryAnswerer with helper model functionality.
 */
class BlockBasedMemoryAnswererHelperModelTest extends AnyFunSuite with Matchers {
  
  // Track which model was used for what
  class TrackingMockModel(name: String, var callCount: Int = 0) extends LLMModel {
    def generateContent(prompt: String): Try[LLMResponse] = {
      callCount += 1
      Success(LLMResponse(
        content = if (prompt.contains("extract")) {
          s"Extracted by $name: relevant information"
        } else {
          s"Final answer by $name"
        },
        modelName = name,
        tokenUsage = None,
        cost = 0.0
      ))
    }
    
    def getModelName: String = name
    def getProvider: String = "mock"
    
    def reset(): Unit = callCount = 0
  }
  
  test("BlockBasedMemoryAnswerer uses helper model for extraction and main model for aggregation") {
    // Create two tracking models
    val mainModel = new TrackingMockModel("main-model")
    val helperModel = new TrackingMockModel("helper-model")
    
    // Create answerer with helper model
    val answerer = new BlockBasedMemoryAnswerer(
      model = mainModel,
      blockSize = 2,
      helperModel = helperModel
    )
    
    // Add some test conversations (enough for multiple blocks)
    val conversations = (1 to 5).map { i =>
      Conversation(
        id = Some(s"conv-$i"),
        messages = List(
          Message(speaker = "user", text = s"Message $i"),
          Message(speaker = "assistant", text = s"Response $i")
        ),
        containsEvidence = Some(i == 1)
      )
    }.toList
    
    conversations.foreach(answerer.addConversation)
    
    // Ask a question
    val result = answerer.answerQuestion("What's important?", "test-case-1")
    
    // Verify results
    result.answer shouldBe defined
    
    // Helper model should have been called for block extraction (3 blocks: 2+2+1)
    helperModel.callCount should be > 0
    println(s"Helper model called ${helperModel.callCount} times for block extraction")
    
    // Main model should have been called for final aggregation
    mainModel.callCount should be > 0
    println(s"Main model called ${mainModel.callCount} times for final aggregation")
    
    // The final answer should come from the main model
    result.answer.get should include("main-model")
  }
  
  test("BlockBasedMemoryAnswerer uses specified helper model") {
    val mainModel = new TrackingMockModel("main-model")
    val helperModel = new TrackingMockModel("helper-model")
    
    // Create answerer with helper model (now required)
    val answerer = new BlockBasedMemoryAnswerer(
      model = mainModel,
      blockSize = 2,
      helperModel = helperModel
    )
    
    // Add some test conversations
    val conversations = (1 to 3).map { i =>
      Conversation(
        id = Some(s"conv-$i"),
        messages = List(
          Message(speaker = "user", text = s"Message $i"),
          Message(speaker = "assistant", text = s"Response $i")
        ),
        containsEvidence = Some(false)
      )
    }.toList
    
    conversations.foreach(answerer.addConversation)
    
    // Ask a question
    val result = answerer.answerQuestion("What's important?", "test-case-2")
    
    // Verify results
    result.answer shouldBe defined
    
    // Helper model should have been called for extraction
    helperModel.callCount should be > 0
    println(s"Helper model called ${helperModel.callCount} times for extraction")
    
    // Main model should have been called for aggregation
    mainModel.callCount should be > 0
    println(s"Main model called ${mainModel.callCount} times for aggregation")
  }
  
  test("MemoryAnswererFactory correctly passes helper model to BlockBasedMemoryAnswerer") {
    val mainModel = new TrackingMockModel("main-model")
    val helperModel = new TrackingMockModel("helper-model")
    
    // Use factory to create answerer
    val answerer = BlockBasedMemoryFactory.create(
      model = Some(mainModel),
      helperModel = Some(helperModel)
    )
    
    // Verify it's a BlockBasedMemoryAnswerer (through behavior)
    answerer.getMemoryType shouldBe "block_based"
    
    // Add a conversation and test
    val conversation = Conversation(
      id = Some("test-conv"),
      messages = List(
        Message(speaker = "user", text = "Test message"),
        Message(speaker = "assistant", text = "Test response")
      ),
      containsEvidence = Some(false)
    )
    
    answerer.addConversation(conversation)
    val result = answerer.answerQuestion("Test question", "test-case-3")
    
    result.answer shouldBe defined
  }
}