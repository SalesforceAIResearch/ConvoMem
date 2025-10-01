package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse, TokenUsage}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.{Success, Try}

/**
 * Integration test that verifies ExtractedContextMemoryAnswerer works correctly
 * without making actual API calls
 */
class ExtractedContextMemoryAnswererIntegrationTest extends AnyFunSuite with Matchers {

  /**
   * Deterministic mock model for testing
   */
  class DeterministicMockLLM(name: String, isHelper: Boolean = false) extends LLMModel {
    override def generateContent(prompt: String): Try[LLMResponse] = {
      val content = if (isHelper && prompt.contains("extract ALL relevant information")) {
        // Helper model extracting context
        if (prompt.contains("favorite color") && prompt.contains("blue")) {
          "Extracted information: User mentioned their favorite color is blue."
        } else if (prompt.contains("software engineer") && prompt.contains("Scala")) {
          "Extracted information: User is a software engineer who works with Scala and React."
        } else if (prompt.contains("hiking") && prompt.contains("mountains")) {
          "Extracted information: User enjoys hiking in the mountains."
        } else {
          "No relevant information found in the conversations."
        }
      } else if (!isHelper && prompt.contains("Based on ALL the extracted information above")) {
        // Main model answering based on extracted context
        if (prompt.contains("favorite color is blue")) {
          "The user's favorite color is blue."
        } else if (prompt.contains("software engineer")) {
          "The user is a software engineer working with Scala and React."
        } else if (prompt.contains("hiking in the mountains")) {
          "The user enjoys hiking in the mountains as a hobby."
        } else if (prompt.contains("No relevant information")) {
          "I don't know the answer based on the available information."
        } else {
          "Unable to determine from the context provided."
        }
      } else {
        "Mock response"
      }
      
      Success(LLMResponse(
        content = content,
        modelName = name,
        tokenUsage = Some(TokenUsage(100, 50, 150, Some(30))),
        cost = 0.002
      ))
    }
    
    override def getModelName: String = name
    override def getProvider: String = "mock"
  }

  test("ExtractedContextMemoryAnswerer should extract and answer about favorite color") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false)
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    // Add conversation about favorite color
    answerer.addConversation(Conversation(
      messages = List(
        Message("User", "My favorite color is blue."),
        Message("Assistant", "Blue is a nice color!"),
        Message("User", "Yes, I find it very calming.")
      ),
      id = Some("color-conv"),
      containsEvidence = Some(true)
    ))
    
    // Ask about favorite color
    val result = answerer.answerQuestion("What is the user's favorite color?", "test-1")
    
    result.answer should not be empty
    result.answer.get shouldBe "The user's favorite color is blue."
    result.memorySystemResponses.head should include("favorite color is blue")
    result.inputTokens shouldBe Some(100)
    result.outputTokens shouldBe Some(50)
    result.cost shouldBe Some(0.002)
    result.cachedInputTokens shouldBe Some(30)
  }

  test("ExtractedContextMemoryAnswerer should extract and answer about profession") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false)
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    // Add conversation about profession
    answerer.addConversation(Conversation(
      messages = List(
        Message("User", "I work as a software engineer."),
        Message("Assistant", "That's interesting! What technologies do you use?"),
        Message("User", "Mainly Scala and React for web applications.")
      ),
      id = Some("work-conv"),
      containsEvidence = Some(true)
    ))
    
    // Ask about profession
    val result = answerer.answerQuestion("What does the user do for work?", "test-2")
    
    result.answer should not be empty
    result.answer.get should include("software engineer")
    result.memorySystemResponses.head should include("software engineer")
  }

  test("ExtractedContextMemoryAnswerer should handle no relevant information") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false)
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    // Add conversation without relevant info
    answerer.addConversation(Conversation(
      messages = List(
        Message("User", "Hello!"),
        Message("Assistant", "Hi there! How can I help you?"),
        Message("User", "Just saying hi.")
      ),
      id = Some("greeting-conv")
    ))
    
    // Ask about something not in conversations
    val result = answerer.answerQuestion("What is the user's favorite food?", "test-3")
    
    result.answer shouldBe None
    result.memorySystemResponses.head should include("No relevant information")
  }

  test("ExtractedContextMemoryAnswerer should handle multiple conversations") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false)
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    // Add multiple conversations
    val conversations = List(
      Conversation(
        messages = List(Message("User", "I love hiking in the mountains.")),
        id = Some("hobby-1")
      ),
      Conversation(
        messages = List(Message("User", "The mountains are so peaceful.")),
        id = Some("hobby-2")
      ),
      Conversation(
        messages = List(Message("User", "I go hiking every weekend.")),
        id = Some("hobby-3")
      )
    )
    
    answerer.addConversations(conversations)
    
    // Ask about hobbies
    val result = answerer.answerQuestion("What are the user's hobbies?", "test-4")
    
    result.answer should not be empty
    result.answer.get should include("hiking")
    result.memorySystemResponses should not be empty
  }

  test("ExtractedContextMemoryAnswerer should clear memory properly") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false)
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    // Add conversation
    answerer.addConversation(Conversation(
      messages = List(Message("User", "Test message")),
      id = Some("test-conv")
    ))
    
    // Clear memory
    answerer.clearMemory()
    
    // After clearing, should find no relevant information
    val result = answerer.answerQuestion("What did the user say?", "test-5")
    
    result.answer shouldBe None
    result.memorySystemResponses.head should include("No relevant information")
  }

  test("ExtractedContextMemoryFactory should create working instances") {
    // Create mock models
    val helperModel = new DeterministicMockLLM("factory-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("factory-main", isHelper = false)
    
    // Create instance via factory
    val answerer = ExtractedContextMemoryFactory.create(
      model = Some(mainModel),
      helperModel = Some(helperModel)
    )
    
    answerer.getMemoryType shouldBe "extracted_context"
    answerer.initialize()
    
    // Verify it works
    answerer.addConversation(Conversation(
      messages = List(Message("User", "My favorite color is blue.")),
      id = Some("factory-test")
    ))
    
    val result = answerer.answerQuestion("What is the user's favorite color?", "factory-test-1")
    
    result.answer should not be empty
    result.answer.get should include("blue")
  }

  test("ExtractedContextMemoryAnswerer should handle 'I don't know' response") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false) {
      override def generateContent(prompt: String): Try[LLMResponse] = {
        Success(LLMResponse(
          content = "I don't know the answer to that question.",
          modelName = "mock-main",
          tokenUsage = Some(TokenUsage(100, 50, 150)),
          cost = 0.002
        ))
      }
    }
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "Some random conversation")),
      id = Some("random")
    ))
    
    val result = answerer.answerQuestion("What is something specific?", "test-idk")
    
    // Should return None when model says "I don't know"
    result.answer shouldBe None
    result.memorySystemResponses should not be empty
  }

  test("ExtractedContextMemoryAnswerer should handle empty conversation list") {
    val helperModel = new DeterministicMockLLM("mock-helper", isHelper = true)
    val mainModel = new DeterministicMockLLM("mock-main", isHelper = false)
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    
    answerer.initialize()
    
    // Don't add any conversations
    val result = answerer.answerQuestion("What is the user's name?", "test-empty")
    
    // Should handle gracefully
    result.answer shouldBe None
    result.memorySystemResponses should not be empty
  }
}