package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse, TokenUsage}
import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.{Success, Try}

class LongContextMemoryAnswererTest extends AnyFunSuite with Matchers {

  // Mock LLM model for testing
  class MockLLMModel(var lastPrompt: String = "") extends LLMModel {
    override def getModelName: String = "mock-model"
    override def getProvider: String = "mock-provider"
    
    override def generateContent(prompt: String): Try[LLMResponse] = {
      lastPrompt = prompt
      Success(LLMResponse(
        content = "Mock response based on prompt",
        modelName = "mock-model",
        tokenUsage = Some(TokenUsage(100, 50, 150, Some(25))),
        cost = 0.001
      ))
    }
  }

  test("LongContextMemoryAnswerer should use MemoryPromptUtils for prompt building") {
    val mockModel = new MockLLMModel()
    val answerer = new LongContextMemoryAnswerer(mockModel)
    
    // Add some conversations
    val conversation1 = Conversation(
      messages = List(
        Message("User", "My name is Alice."),
        Message("Assistant", "Nice to meet you, Alice!")
      ),
      id = Some("conv1")
    )
    
    val conversation2 = Conversation(
      messages = List(
        Message("User", "I work at Salesforce."),
        Message("Assistant", "That's great! Salesforce is a leading CRM company.")
      ),
      id = Some("conv2")
    )
    
    answerer.addConversation(conversation1)
    answerer.addConversation(conversation2)
    
    // Ask a question
    val question = "What is my name?"
    val result = answerer.answerQuestion(question, "test-id-1")
    
    // Verify the result structure
    result.answer shouldBe defined
    result.answer.get should include("Mock response")
    result.inputTokens shouldBe Some(100)
    result.outputTokens shouldBe Some(50)
    result.cachedInputTokens shouldBe Some(25)
    result.cost shouldBe Some(0.001)
    
    // Verify the prompt includes judge criteria
    mockModel.lastPrompt should include("Core Information Accuracy")
    mockModel.lastPrompt should include("Completeness")
    mockModel.lastPrompt should include("Factual Correctness")
    
    // Verify the prompt includes conversations
    mockModel.lastPrompt should include("My name is Alice")
    mockModel.lastPrompt should include("I work at Salesforce")
    
    // Verify the prompt includes the question
    mockModel.lastPrompt should include(question)
    
    // Verify instruction about incorporating all knowledge
    mockModel.lastPrompt should include("incorporate ALL relevant prior knowledge")
  }

  test("LongContextMemoryAnswerer should handle empty conversations") {
    val mockModel = new MockLLMModel()
    val answerer = new LongContextMemoryAnswerer(mockModel)
    
    // Try to answer without adding any conversations
    val result = answerer.answerQuestion("What is my name?", "test-id-2")
    
    // Should return empty result
    result.answer shouldBe None
    result.retrievedConversationIds shouldBe empty
  }

  test("LongContextMemoryAnswerer should clear memory correctly") {
    val mockModel = new MockLLMModel()
    val answerer = new LongContextMemoryAnswerer(mockModel)
    
    // Add a conversation
    val conversation = Conversation(
      messages = List(Message("User", "Hello")),
      id = Some("test")
    )
    answerer.addConversation(conversation)
    
    // Clear memory
    answerer.clearMemory()
    
    // Should return empty result after clearing
    val result = answerer.answerQuestion("What did I say?", "test-id-3")
    result.answer shouldBe None
  }

  test("LongContextMemoryAnswerer should handle multiple conversations in order") {
    val mockModel = new MockLLMModel()
    val answerer = new LongContextMemoryAnswerer(mockModel)
    
    // Add multiple conversations
    for (i <- 1 to 5) {
      val conversation = Conversation(
        messages = List(
          Message("User", s"Message $i"),
          Message("Assistant", s"Response $i")
        ),
        id = Some(s"conv$i")
      )
      answerer.addConversation(conversation)
    }
    
    // Ask a question
    answerer.answerQuestion("What did I say?", "test-id-4")
    
    // Verify all conversations are in the prompt in order
    for (i <- 1 to 5) {
      mockModel.lastPrompt should include(s"Message $i")
      mockModel.lastPrompt should include(s"conv$i")
    }
    
    // Verify conversations are numbered
    mockModel.lastPrompt should include("Conversation 1")
    mockModel.lastPrompt should include("Conversation 5")
  }

  test("LongContextMemoryAnswerer should preserve conversation structure") {
    val mockModel = new MockLLMModel()
    val answerer = new LongContextMemoryAnswerer(mockModel)
    
    // Add a multi-turn conversation
    val conversation = Conversation(
      messages = List(
        Message("User", "What's the weather?"),
        Message("Assistant", "I don't have access to weather data."),
        Message("User", "Can you help me with code?"),
        Message("Assistant", "Yes, I can help with programming!")
      ),
      id = Some("multi-turn")
    )
    
    answerer.addConversation(conversation)
    answerer.answerQuestion("What can you help with?", "test-id-5")
    
    // Verify conversation structure is preserved
    mockModel.lastPrompt should include("User: What's the weather?")
    mockModel.lastPrompt should include("Assistant: I don't have access to weather data.")
    mockModel.lastPrompt should include("User: Can you help me with code?")
    mockModel.lastPrompt should include("Assistant: Yes, I can help with programming!")
  }
}