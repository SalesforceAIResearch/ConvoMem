package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse, TokenUsage}
import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Try}

/**
 * Unit tests for BlockBasedMemoryAnswerer.
 */
class BlockBasedMemoryAnswererTest extends AnyFlatSpec with Matchers {

  /**
   * Mock LLM model for testing that provides controlled responses.
   */
  class MockLLMModel(
    extractionResponses: Map[Int, String],
    finalAnswerResponse: String
  ) extends LLMModel {
    
    var extractionCallCount = 0
    var finalAnswerCallCount = 0
    
    def generateContent(prompt: String): Try[LLMResponse] = {
      if (prompt.contains("extract ALL information")) {
        // This is an extraction call
        extractionCallCount += 1
        val response = extractionResponses.getOrElse(extractionCallCount, "No information is relevant.")
        Success(LLMResponse(
          content = response,
          modelName = "mock-model",
          tokenUsage = Some(TokenUsage(100, 50, 150, Some(25))),
          cost = 0.001
        ))
      } else if (prompt.contains("synthesize ALL the provided information")) {
        // This is a final answer call
        finalAnswerCallCount += 1
        Success(LLMResponse(
          content = finalAnswerResponse,
          modelName = "mock-model",
          tokenUsage = Some(TokenUsage(200, 100, 300, Some(50))),
          cost = 0.002
        ))
      } else {
        Success(LLMResponse(
          content = "Unexpected prompt",
          modelName = "mock-model",
          tokenUsage = Some(TokenUsage(10, 10, 20, None)),
          cost = 0.0001
        ))
      }
    }
    
    def getModelName: String = "mock-model"
    def getProvider: String = "mock-provider"
    
    def getExtrationCallCount: Int = extractionCallCount
    def getFinalAnswerCallCount: Int = finalAnswerCallCount
  }

  /**
   * Helper to create a simple conversation.
   */
  def createConversation(id: String, userMessage: String, assistantMessage: String): Conversation = {
    Conversation(
      id = Some(id),
      messages = List(
        Message("User", userMessage),
        Message("Assistant", assistantMessage)
      ),
      containsEvidence = Some(true)
    )
  }

  "BlockBasedMemoryAnswerer" should "handle empty conversations gracefully" in {
    val model = new MockLLMModel(Map.empty, "")
    val answerer = new BlockBasedMemoryAnswerer(model, helperModel = model)
    
    val result = answerer.answerQuestion("What is the user's name?", "test-id-1")
    
    result.answer should be(None)
    result.retrievedConversationIds should be(empty)
    result.memorySystemResponses should be(empty)
  }

  it should "process a single block of conversations (less than 10)" in {
    val extractionResponses = Map(
      1 -> "The user's name is John and he lives in San Francisco."
    )
    val finalAnswer = "Based on the conversations, the user's name is John and he lives in San Francisco."
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 10, helperModel = model)
    
    // Add 5 conversations (less than block size)
    for (i <- 1 to 5) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i from user",
        s"Response $i from assistant"
      ))
    }
    
    val result = answerer.answerQuestion("What is the user's name and where does he live?", "test-id-2")
    
    result.answer should be(Some(finalAnswer))
    result.memorySystemResponses should have size 1
    result.memorySystemResponses.head should be("The user's name is John and he lives in San Francisco.")
    model.getExtrationCallCount should be(1)
    model.getFinalAnswerCallCount should be(1)
  }

  it should "process multiple blocks of conversations" in {
    val extractionResponses = Map(
      1 -> "Block 1: The user's name is Alice.",
      2 -> "Block 2: Alice works as a software engineer.",
      3 -> "Block 3: Alice has 10 years of experience."
    )
    val finalAnswer = "Alice is a software engineer with 10 years of experience."
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 10, helperModel = model)
    
    // Add 25 conversations (3 blocks: 10, 10, 5)
    for (i <- 1 to 25) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i from user",
        s"Response $i from assistant"
      ))
    }
    
    val result = answerer.answerQuestion("Tell me about Alice's professional background.", "test-id-3")
    
    result.answer should be(Some(finalAnswer))
    result.memorySystemResponses should have size 3
    result.memorySystemResponses should contain("Block 1: The user's name is Alice.")
    result.memorySystemResponses should contain("Block 2: Alice works as a software engineer.")
    result.memorySystemResponses should contain("Block 3: Alice has 10 years of experience.")
    model.getExtrationCallCount should be(3)
    model.getFinalAnswerCallCount should be(1)
  }

  it should "handle blocks with no relevant information" in {
    val extractionResponses = Map(
      1 -> "No information is relevant.",
      2 -> "The user prefers coffee over tea.",
      3 -> "No information is relevant."
    )
    val finalAnswer = "The user prefers coffee over tea."
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 10, helperModel = model)
    
    // Add 25 conversations
    for (i <- 1 to 25) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i",
        s"Response $i"
      ))
    }
    
    val result = answerer.answerQuestion("What are the user's beverage preferences?", "test-id-4")
    
    result.answer should be(Some(finalAnswer))
    result.memorySystemResponses should have size 3
    // Should filter out "No information is relevant" for final answer but keep in responses
    val relevantExtractions = result.memorySystemResponses.filter(_ != "No information is relevant.")
    relevantExtractions should have size 1
    relevantExtractions.head should be("The user prefers coffee over tea.")
  }

  it should "handle all blocks returning no relevant information" in {
    val extractionResponses = Map(
      1 -> "No information is relevant.",
      2 -> "No information is relevant."
    )
    val finalAnswer = "" // Won't be called
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 10, helperModel = model)
    
    // Add 15 conversations (2 blocks)
    for (i <- 1 to 15) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i",
        s"Response $i"
      ))
    }
    
    val result = answerer.answerQuestion("What is the user's favorite color?", "test-id-5")
    
    result.answer should be(Some("I don't have any information to answer this question."))
    result.memorySystemResponses should have size 2
    result.memorySystemResponses.forall(_ == "No information is relevant.") should be(true)
    model.getExtrationCallCount should be(2)
    model.getFinalAnswerCallCount should be(0) // Should not call final answer
  }

  it should "correctly aggregate token usage and costs" in {
    val extractionResponses = Map(
      1 -> "Info from block 1",
      2 -> "Info from block 2"
    )
    val finalAnswer = "Combined answer"
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 10, helperModel = model)
    
    // Add 15 conversations (2 blocks)
    for (i <- 1 to 15) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i",
        s"Response $i"
      ))
    }
    
    val result = answerer.answerQuestion("Test question", "test-id-6")
    
    // Verify token counts (only final answer input tokens, but aggregated output tokens and costs)
    result.inputTokens should be(Some(200)) // Only final answer input tokens
    result.outputTokens should be(Some(50 * 2 + 100)) // 200 total output tokens from all phases
    result.cachedInputTokens should be(Some(50)) // Only final answer cached tokens
    result.cost should be(Some(0.001 * 2 + 0.002)) // 0.004 total cost from all phases
  }

  it should "handle custom block sizes" in {
    val extractionResponses = Map(
      1 -> "Block 1 info",
      2 -> "Block 2 info",
      3 -> "Block 3 info",
      4 -> "Block 4 info",
      5 -> "Block 5 info"
    )
    val finalAnswer = "Answer based on 5 blocks"
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 3, helperModel = model) // Custom block size
    
    // Add 13 conversations (5 blocks: 3, 3, 3, 3, 1)
    for (i <- 1 to 13) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i",
        s"Response $i"
      ))
    }
    
    val result = answerer.answerQuestion("Test with custom block size", "test-id-7")
    
    result.answer should be(Some(finalAnswer))
    result.memorySystemResponses should have size 5
    model.getExtrationCallCount should be(5)
    model.getFinalAnswerCallCount should be(1)
  }

  it should "format conversations correctly in the context" in {
    // This test verifies that conversations are formatted correctly in the extraction prompts
    var capturedPrompts = List.empty[String]
    
    val model = new LLMModel {
      def generateContent(prompt: String): Try[LLMResponse] = {
        capturedPrompts = capturedPrompts :+ prompt
        Success(LLMResponse(
          content = if (prompt.contains("extract")) "Extracted info" else "Final answer",
          modelName = "mock-model",
          tokenUsage = Some(TokenUsage(100, 50, 150, None)),
          cost = 0.001
        ))
      }
      def getModelName: String = "mock-model"
      def getProvider: String = "mock-provider"
    }
    
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 2, helperModel = model)
    
    // Add conversations with specific IDs (IDs won't be shown in context)
    answerer.addConversation(createConversation("unique-id-1", "Hello", "Hi"))
    answerer.addConversation(createConversation("unique-id-2", "How are you?", "I'm fine"))
    
    answerer.answerQuestion("Test question", "test-id-8")
    
    // Check that the extraction prompt includes conversation content properly formatted
    val extractionPrompt = capturedPrompts.find(_.contains("extract")).get
    extractionPrompt should include("Conversation 1:")
    extractionPrompt should include("User: Hello")
    extractionPrompt should include("Assistant: Hi")
    extractionPrompt should include("Conversation 2:")
    extractionPrompt should include("User: How are you?")
    extractionPrompt should include("Assistant: I'm fine")
  }

  it should "clear memory when clearMemory is called" in {
    val model = new MockLLMModel(Map(1 -> "Some info"), "Answer")
    val answerer = new BlockBasedMemoryAnswerer(model, helperModel = model)
    
    // Add conversations
    answerer.addConversation(createConversation("1", "Hello", "Hi"))
    answerer.addConversation(createConversation("2", "Test", "Response"))
    
    // Clear memory
    answerer.clearMemory()
    
    // Try to answer question - should get empty result
    val result = answerer.answerQuestion("What was said?", "test-id-9")
    result.answer should be(None)
    result.memorySystemResponses should be(empty)
  }

  it should "return correct memory type" in {
    val model = new MockLLMModel(Map.empty, "")
    val answerer = new BlockBasedMemoryAnswerer(model, helperModel = model)
    
    answerer.getMemoryType should be("block_based")
  }

  it should "generate extraction prompts with detailed quote instructions" in {
    // Test that the extraction prompt contains instructions for detailed extraction
    var capturedPrompts = List.empty[String]
    
    val model = new LLMModel {
      def generateContent(prompt: String): Try[LLMResponse] = {
        capturedPrompts = capturedPrompts :+ prompt
        Success(LLMResponse(
          content = "Extracted information with quotes",
          modelName = "mock-model",
          tokenUsage = Some(TokenUsage(100, 50, 150, None)),
          cost = 0.001
        ))
      }
      def getModelName: String = "mock-model"
      def getProvider: String = "mock-provider"
    }
    
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 2, helperModel = model)
    
    answerer.addConversation(createConversation("id-1", "Test message", "Test response"))
    answerer.answerQuestion("Test question", "test-id-8")
    
    // Check that the extraction prompt contains detailed instructions
    val extractionPrompt = capturedPrompts.find(_.contains("extract")).get
    extractionPrompt should include("DIRECT QUOTES")
    extractionPrompt should include("exact quote here")
    extractionPrompt should include("Include exact wording, not paraphrases or summaries")
    extractionPrompt should include("Provide the full context around relevant statements")
    extractionPrompt should include("Specify which conversation and speaker provided each piece of information")
    extractionPrompt should include("EXAMPLE of detailed extraction")
  }

  it should "handle variations of 'No information is relevant' with fuzzy matching" in {
    // Test various realistic variations that LLMs might produce
    val extractionResponses = Map(
      1 -> "No information is relevant",  // Missing period
      2 -> "no information is relevant.",  // Lowercase
      3 -> "No information is relevant!",  // Different punctuation
      4 -> "No informations is relevant.", // Typo: "informations"
      5 -> "No infomation is relevant.",   // Typo: "infomation"
      6 -> "The user likes pizza.",        // Actual information
      7 -> "No relevant information.",     // Slight variation
      8 -> "No info is relevant."          // Abbreviation
    )
    val finalAnswer = "The user likes pizza."
    
    val model = new MockLLMModel(extractionResponses, finalAnswer)
    val answerer = new BlockBasedMemoryAnswerer(model, blockSize = 1, helperModel = model) // 1 conversation per block to test all responses
    
    // Add 8 conversations (8 blocks with blockSize=1)
    for (i <- 1 to 8) {
      answerer.addConversation(createConversation(
        s"conv-$i",
        s"Message $i",
        s"Response $i"
      ))
    }
    
    val result = answerer.answerQuestion("What does the user like?", "test-id-11")
    
    result.answer should be(Some(finalAnswer))
    result.memorySystemResponses should have size 8
    
    // Check that variations 1-5 are filtered out (Levenshtein distance <= 5)
    // Variations 7-8 have distance > 5 so they're kept along with actual info (6)
    val expectedFiltered = Set(
      "No information is relevant",
      "no information is relevant.",
      "No information is relevant!",
      "No informations is relevant.",
      "No infomation is relevant."
    )
    
    // All 8 responses should be in memorySystemResponses
    val actualResponses = result.memorySystemResponses.toSet
    actualResponses should have size 8
    
    // All extraction responses should be present
    actualResponses should contain("The user likes pizza.")
    actualResponses should contain("No information is relevant")
    actualResponses should contain("no information is relevant.")
    actualResponses should contain("No information is relevant!")
    actualResponses should contain("No informations is relevant.")
    actualResponses should contain("No infomation is relevant.")
    actualResponses should contain("No relevant information.")
    actualResponses should contain("No info is relevant.")
    
    // Model should have been called for final answer since there's relevant info
    model.getFinalAnswerCallCount should be(1)
  }
}