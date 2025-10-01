package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel, LLMResponse, TokenUsage}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.{Success, Failure, Try}

/**
 * Comprehensive unit tests for ExtractedContextMemoryAnswerer
 */
class ExtractedContextMemoryAnswererTest extends AnyFunSuite with Matchers {

  /**
   * Mock LLM model that simulates real LLM behavior for testing
   */
  class TestLLMModel(
    name: String,
    responses: Map[String, String] = Map.empty,
    shouldFail: Boolean = false,
    failureMessage: String = "Mock failure"
  ) extends LLMModel {
    
    var callCount = 0
    var lastPrompt: String = ""
    
    override def generateContent(prompt: String): Try[LLMResponse] = {
      callCount += 1
      lastPrompt = prompt
      
      if (shouldFail) {
        Failure(new RuntimeException(failureMessage))
      } else {
        // Look for specific patterns in prompt to determine response
        val response = if (prompt.contains("extract ALL relevant information")) {
          responses.getOrElse("extraction", 
            """Extracted information:
              |- User's favorite color: blue
              |- User works as: software engineer
              |- User's hobbies: hiking, reading
              |- User lives in: San Francisco""".stripMargin)
        } else if (prompt.contains("Based on ALL the extracted information above")) {
          responses.getOrElse("answer", "The user's favorite color is blue.")
        } else {
          responses.getOrElse("default", "Test response")
        }
        
        Success(LLMResponse(
          content = response,
          modelName = name,
          tokenUsage = Some(TokenUsage(100, 50, 150, Some(20))),
          cost = 0.001
        ))
      }
    }
    
    override def getModelName: String = name
    override def getProvider: String = "test"
  }

  test("ExtractedContextMemoryAnswerer should initialize with default models") {
    val mainModel = new TestLLMModel("test-main")
    val helperModel = new TestLLMModel("test-helper")
    val answerer = new ExtractedContextMemoryAnswerer(mainModel, helperModel)
    answerer.initialize()
    answerer.getMemoryType shouldBe "extracted_context"
  }

  test("ExtractedContextMemoryAnswerer should initialize with custom models") {
    val mainModel = new TestLLMModel("test-main")
    val helperModel = new TestLLMModel("test-helper")
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    answerer.getMemoryType shouldBe "extracted_context"
  }

  test("ExtractedContextMemoryAnswerer should add single conversation") {
    val mainModel = new TestLLMModel("test-main")
    val helperModel = new TestLLMModel("test-helper")
    val answerer = new ExtractedContextMemoryAnswerer(mainModel, helperModel)
    answerer.initialize()
    
    val conversation = Conversation(
      messages = List(
        Message("User", "Hello, I'm testing"),
        Message("Assistant", "Hello! How can I help you test?")
      ),
      id = Some("test-conv-1")
    )
    
    answerer.addConversation(conversation)
    // No public way to check conversation count, but should not throw
  }

  test("ExtractedContextMemoryAnswerer should add multiple conversations") {
    val mainModel = new TestLLMModel("test-main")
    val helperModel = new TestLLMModel("test-helper")
    val answerer = new ExtractedContextMemoryAnswerer(mainModel, helperModel)
    answerer.initialize()
    
    val conversations = List(
      Conversation(
        messages = List(Message("User", "First conversation")),
        id = Some("conv-1")
      ),
      Conversation(
        messages = List(Message("User", "Second conversation")),
        id = Some("conv-2")
      ),
      Conversation(
        messages = List(Message("User", "Third conversation")),
        id = Some("conv-3")
      )
    )
    
    answerer.addConversations(conversations)
    // Should not throw
  }

  test("ExtractedContextMemoryAnswerer should clear memory") {
    val mainModel = new TestLLMModel("test-main")
    val helperModel = new TestLLMModel("test-helper")
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    // Add conversations
    answerer.addConversations(List(
      Conversation(
        messages = List(Message("User", "Test message")),
        id = Some("test-1")
      )
    ))
    
    // Clear memory
    answerer.clearMemory()
    
    // After clearing, answering should find no context
    val result = answerer.answerQuestion("What did the user say?", "test-case-1")
    // With empty conversations, extraction should return minimal context
    result.memorySystemResponses should not be empty
  }

  test("ExtractedContextMemoryAnswerer should extract context and answer questions") {
    val extractedInfo = """Extracted relevant information:
      |- User's name: Alice
      |- User's job: Data scientist at TechCorp
      |- User's favorite programming language: Python
      |- User mentioned working on ML projects""".stripMargin
    
    val answer = "Based on the context, the user's name is Alice and she works as a data scientist."
    
    val helperModel = new TestLLMModel("test-helper", Map("extraction" -> extractedInfo))
    val mainModel = new TestLLMModel("test-main", Map("answer" -> answer))
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    // Add conversations with relevant information
    val conversations = List(
      Conversation(
        messages = List(
          Message("User", "Hi, I'm Alice"),
          Message("Assistant", "Nice to meet you, Alice!"),
          Message("User", "I work as a data scientist at TechCorp"),
          Message("Assistant", "That sounds interesting! What kind of projects do you work on?"),
          Message("User", "Mostly ML projects using Python")
        ),
        id = Some("conv-1"),
        containsEvidence = Some(true)
      )
    )
    
    answerer.addConversations(conversations)
    
    // Test answering a question
    val result = answerer.answerQuestion("Who is the user and what do they do?", "test-case-1")
    
    // Verify the result
    result.answer should not be empty
    result.answer.get shouldBe answer
    result.memorySystemResponses should contain(extractedInfo)
    result.inputTokens shouldBe Some(100)  // Only final answer input tokens
    result.outputTokens shouldBe Some(100)  // Aggregated output tokens from both phases (50 + 50)
    result.cost shouldBe Some(0.002)  // Aggregated cost from both phases (0.001 + 0.001)
    result.cachedInputTokens shouldBe Some(20)  // Only final answer cached tokens
    
    // Verify both models were called
    helperModel.callCount shouldBe 1
    mainModel.callCount shouldBe 1
    
    // Verify extraction prompt contains the question
    helperModel.lastPrompt should include("Who is the user and what do they do?")
    helperModel.lastPrompt should include("extract ALL relevant information")
    
    // Verify answer prompt contains extracted context
    mainModel.lastPrompt should include(extractedInfo)
    mainModel.lastPrompt should include("Who is the user and what do they do?")
  }

  test("ExtractedContextMemoryAnswerer should handle 'I don't know' responses") {
    val helperModel = new TestLLMModel("test-helper", 
      Map("extraction" -> "Some context about weather and sports"))
    val mainModel = new TestLLMModel("test-main", 
      Map("answer" -> "I don't know the user's favorite food based on the available context."))
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(
        Message("User", "The weather is nice today"),
        Message("Assistant", "Yes, it's beautiful!"),
        Message("User", "I enjoy playing tennis")
      ),
      id = Some("conv-1")
    ))
    
    val result = answerer.answerQuestion("What is the user's favorite food?", "test-case-1")
    
    // Should return None when model says "I don't know"
    result.answer shouldBe None
    result.memorySystemResponses should not be empty
  }

  test("ExtractedContextMemoryAnswerer should handle no relevant information") {
    val helperModel = new TestLLMModel("test-helper", 
      Map("extraction" -> "No relevant information found."))
    val mainModel = new TestLLMModel("test-main")
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "Hello"), Message("Assistant", "Hi")),
      id = Some("conv-1")
    ))
    
    val result = answerer.answerQuestion("What is the user's age?", "test-case-1")
    
    // Should return None when no relevant information
    result.answer shouldBe None
    result.memorySystemResponses should contain("No relevant information found.")
    
    // Main model should not be called when no relevant context
    mainModel.callCount shouldBe 0
  }

  test("ExtractedContextMemoryAnswerer should handle extraction failure with retry") {
    // Create a helper that fails twice then succeeds
    var attemptCount = 0
    val helperModel = new TestLLMModel("test-helper") {
      override def generateContent(prompt: String): Try[LLMResponse] = {
        attemptCount += 1
        if (attemptCount <= 2) {
          Failure(new RuntimeException(s"Temporary failure $attemptCount"))
        } else {
          Success(LLMResponse(
            "Extracted: User likes blue",
            "test-helper",
            Some(TokenUsage(100, 50, 150)),
            0.001
          ))
        }
      }
    }
    
    val mainModel = new TestLLMModel("test-main", 
      Map("answer" -> "The user likes blue."))
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "I like blue")),
      id = Some("conv-1")
    ))
    
    val result = answerer.answerQuestion("What does the user like?", "test-case-1")
    
    // Should succeed after retries
    result.answer should not be empty
    result.answer.get shouldBe "The user likes blue."
    attemptCount should be > 2 // Verify retries happened
  }

  test("ExtractedContextMemoryAnswerer should handle complete extraction failure") {
    val helperModel = new TestLLMModel("test-helper", 
      shouldFail = true, 
      failureMessage = "Permanent extraction failure")
    val mainModel = new TestLLMModel("test-main")
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "Test message")),
      id = Some("conv-1")
    ))
    
    val result = answerer.answerQuestion("What did the user say?", "test-case-1")
    
    // Should handle failure gracefully
    result.answer shouldBe None
    result.memorySystemResponses.head should include("Failed to extract")
    
    // Main model should not be called when extraction fails
    mainModel.callCount shouldBe 0
  }

  test("ExtractedContextMemoryAnswerer should handle answer generation failure") {
    val helperModel = new TestLLMModel("test-helper", 
      Map("extraction" -> "Extracted: User is a teacher"))
    val mainModel = new TestLLMModel("test-main", 
      shouldFail = true,
      failureMessage = "Answer generation failed")
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "I'm a teacher")),
      id = Some("conv-1")
    ))
    
    val result = answerer.answerQuestion("What is the user's profession?", "test-case-1")
    
    // Should return None on answer generation failure
    result.answer shouldBe None
    result.memorySystemResponses should contain("Extracted: User is a teacher")
  }

  test("ExtractedContextMemoryAnswerer should work with empty conversations") {
    val helperModel = new TestLLMModel("test-helper")
    val mainModel = new TestLLMModel("test-main")
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    // Don't add any conversations
    val result = answerer.answerQuestion("What is the user's name?", "test-case-1")
    
    // Should handle empty conversations gracefully
    result.answer shouldBe None  // Or might have a generic response
    result.memorySystemResponses should not be empty
  }

  test("ExtractedContextMemoryAnswerer should handle complex multi-turn conversations") {
    val extractedInfo = """Extracted information:
      |- User initially worked at Google
      |- User changed jobs to Amazon
      |- User's current role: Senior Engineer at Amazon
      |- User previously worked on search algorithms
      |- User now works on distributed systems""".stripMargin
    
    val answer = "The user currently works as a Senior Engineer at Amazon on distributed systems."
    
    val helperModel = new TestLLMModel("test-helper", Map("extraction" -> extractedInfo))
    val mainModel = new TestLLMModel("test-main", Map("answer" -> answer))
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    // Add complex conversation with changing information
    val conversations = List(
      Conversation(
        messages = List(
          Message("User", "I used to work at Google"),
          Message("Assistant", "How was your experience there?"),
          Message("User", "Great! I worked on search algorithms"),
          Message("Assistant", "That sounds challenging!"),
          Message("User", "Yes, but I recently changed jobs"),
          Message("Assistant", "Where do you work now?"),
          Message("User", "I'm now a Senior Engineer at Amazon"),
          Message("Assistant", "What do you work on there?"),
          Message("User", "Distributed systems")
        ),
        id = Some("conv-1"),
        containsEvidence = Some(true)
      )
    )
    
    answerer.addConversations(conversations)
    
    val result = answerer.answerQuestion("Where does the user currently work?", "test-case-1")
    
    result.answer should not be empty
    result.answer.get shouldBe answer
    result.memorySystemResponses should contain(extractedInfo)
  }

  test("ExtractedContextMemoryFactory should create instances with default models") {
    val answerer = ExtractedContextMemoryFactory.create()
    answerer.getMemoryType shouldBe "extracted_context"
    
    // Should use default models (Gemini.flash and Gemini.flashLite)
    answerer shouldBe a[ExtractedContextMemoryAnswerer]
  }

  test("ExtractedContextMemoryFactory should create instances with custom models") {
    val customMain = new TestLLMModel("custom-main")
    val customHelper = new TestLLMModel("custom-helper")
    
    val answerer = ExtractedContextMemoryFactory.create(
      model = Some(customMain),
      helperModel = Some(customHelper)
    )
    
    answerer.getMemoryType shouldBe "extracted_context"
    answerer shouldBe a[ExtractedContextMemoryAnswerer]
  }

  test("ExtractedContextMemoryFactory should handle None for helper model") {
    val customMain = new TestLLMModel("custom-main")
    
    val answerer = ExtractedContextMemoryFactory.create(
      model = Some(customMain),
      helperModel = None
    )
    
    // Should use default helper model (Gemini.flashLite)
    answerer.getMemoryType shouldBe "extracted_context"
  }

  test("ExtractedContextMemoryAnswerer should handle special characters in conversations") {
    val helperModel = new TestLLMModel("test-helper",
      Map("extraction" -> "User's email: test@example.com, code: print(\"Hello, World!\")"))
    val mainModel = new TestLLMModel("test-main",
      Map("answer" -> "The user's email is test@example.com"))
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    answerer.addConversation(Conversation(
      messages = List(
        Message("User", "My email is test@example.com"),
        Message("Assistant", "Got it!"),
        Message("User", "Here's my code: print(\"Hello, World!\")")
      ),
      id = Some("conv-1")
    ))
    
    val result = answerer.answerQuestion("What is the user's email?", "test-case-1")
    
    result.answer should not be empty
    result.answer.get should include("test@example.com")
  }

  test("ExtractedContextMemoryAnswerer should process multiple conversations from different sources") {
    val extractedInfo = """Extracted information from all conversations:
      |- From conversation 1: User likes pizza
      |- From conversation 2: User plays guitar
      |- From conversation 3: User lives in NYC""".stripMargin
    
    val answer = "The user likes pizza, plays guitar, and lives in NYC."
    
    val helperModel = new TestLLMModel("test-helper", Map("extraction" -> extractedInfo))
    val mainModel = new TestLLMModel("test-main", Map("answer" -> answer))
    
    val answerer = new ExtractedContextMemoryAnswerer(
      model = mainModel,
      helperModel = helperModel
    )
    answerer.initialize()
    
    // Add multiple conversations
    answerer.addConversation(Conversation(
      messages = List(Message("User", "I love pizza")),
      id = Some("conv-1")
    ))
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "I play guitar")),
      id = Some("conv-2")
    ))
    
    answerer.addConversation(Conversation(
      messages = List(Message("User", "I live in NYC")),
      id = Some("conv-3")
    ))
    
    val result = answerer.answerQuestion("Tell me about the user", "test-case-1")
    
    result.answer should not be empty
    result.answer.get shouldBe answer
    
    // Verify extraction was called with all conversations
    helperModel.lastPrompt should include("I love pizza")
    helperModel.lastPrompt should include("I play guitar")
    helperModel.lastPrompt should include("I live in NYC")
  }
}