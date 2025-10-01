package com.salesforce.crmmembench.conversations

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.{Conversation, GeneratedConversations}
import com.salesforce.crmmembench.questions.evidence.Message
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._

/**
 * Unit test to verify conversation JSON parsing works correctly
 * after removing delayInSeconds field and adding containsEvidence field.
 */
class ConversationParsingTest extends AnyFunSuite {

  test("Parse GeneratedConversations JSON without delayInSeconds field") {
    // Test JSON that represents what the LLM would generate
    val jsonString = """
    {
      "conversations": [
        {
          "messages": [
            {
              "speaker": "user",
              "text": "Hello, how are you?"
            },
            {
              "speaker": "assistant",
              "text": "I'm doing well, thank you! How can I help you today?"
            }
          ]
        },
        {
          "messages": [
            {
              "speaker": "user",
              "text": "What's the weather like?"
            },
            {
              "speaker": "assistant",
              "text": "I don't have access to real-time weather data."
            }
          ]
        }
      ]
    }
    """
    
    // Parse the JSON
    val result = decode[GeneratedConversations](jsonString)
    
    // Verify parsing succeeded
    assert(result.isRight, s"Failed to parse JSON: ${result.left.getOrElse("unknown error")}")
    
    val generatedConversations = result.getOrElse(throw new Exception("Should not fail"))
    
    // Verify the structure
    assert(generatedConversations.conversations.length == 2)
    assert(generatedConversations.conversations(0).messages.length == 2)
    assert(generatedConversations.conversations(0).messages(0).speaker == "user")
    assert(generatedConversations.conversations(0).messages(0).text == "Hello, how are you?")
    assert(generatedConversations.conversations(0).id.isEmpty) // No ID by default
    assert(generatedConversations.conversations(0).containsEvidence.isEmpty) // No containsEvidence by default
  }

  test("Serialize and deserialize Conversation with containsEvidence field") {
    // Create a conversation instance
    val conversation = Conversation(
      messages = List(
        Message("user", "My favorite color is blue"),
        Message("assistant", "I'll remember that your favorite color is blue")
      ),
      id = Some("test-conv-123"),
      containsEvidence = Some(true)
    )
    
    // Serialize to JSON
    val json = conversation.asJson
    
    // Verify JSON structure doesn't contain delayInSeconds
    assert(!json.toString.contains("delayInSeconds"))
    
    // Deserialize back
    val decoded = json.as[Conversation]
    assert(decoded.isRight)
    
    val decodedConversation = decoded.getOrElse(throw new Exception("Should not fail"))
    assert(decodedConversation.messages.length == 2)
    assert(decodedConversation.id.contains("test-conv-123"))
    assert(decodedConversation.containsEvidence.contains(true))
  }

  test("Parse JSON with delayInSeconds field should ignore it") {
    // JSON with delayInSeconds field (from old data)
    val jsonWithDelay = """
    {
      "messages": [
        {
          "speaker": "user",
          "text": "Test message"
        }
      ],
      "id": "old-conv-id",
      "delayInSeconds": 5
    }
    """
    
    // Parse should succeed and ignore the extra field
    val result = decode[Conversation](jsonWithDelay)
    
    assert(result.isRight, s"Failed to parse JSON with extra field: ${result.left.getOrElse("unknown error")}")
    
    val conversation = result.getOrElse(throw new Exception("Should not fail"))
    assert(conversation.messages.length == 1)
    assert(conversation.id.contains("old-conv-id"))
    assert(conversation.containsEvidence.isEmpty) // containsEvidence not in old data
    // delayInSeconds is ignored
  }

  test("ConversationGenerator should produce parseable conversations") {
    val generator = new ConversationGenerator()
    
    // Create test prompt parts
    val promptParts = ConversationPromptParts(
      evidenceType = "test",
      scenarioDescription = "Test scenario",
      useCaseScenario = Some("Test use case"),
      evidenceMessages = List(
        Message("user", "Test evidence message")
      ),
      question = "Test question?",
      answer = "Test answer",
      evidenceCount = 1
    )
    
    val testPerson = com.salesforce.crmmembench.Personas.Person(
      id = "test-person",
      category = "Test",
      role_name = "Tester",
      description = "Test person",
      background = Some("Test background")
    )
    
    // Get the prompt that would be sent to LLM
    val fullPrompt = generator.getFullConversationPrompt(testPerson, promptParts)
    
    // Verify prompt doesn't contain delayInSeconds
    assert(!fullPrompt.contains("delayInSeconds"))
    
    // Verify the expected JSON structure in the prompt
    assert(fullPrompt.contains("\"conversations\":"))
    assert(fullPrompt.contains("\"messages\":"))
    assert(fullPrompt.contains("\"speaker\":"))
    assert(fullPrompt.contains("\"text\":"))
  }

  test("containsEvidence field behavior") {
    // Test with containsEvidence = true
    val evidenceConv = Conversation(
      messages = List(Message("user", "Evidence message")),
      id = Some("evidence-conv"),
      containsEvidence = Some(true)
    )
    
    val evidenceJson = evidenceConv.asJson
    assert(evidenceJson.noSpaces.contains("\"containsEvidence\":true"))
    
    // Test with containsEvidence = false
    val irrelevantConv = Conversation(
      messages = List(Message("user", "Irrelevant message")),
      id = Some("irrelevant-conv"),
      containsEvidence = Some(false)
    )
    
    val irrelevantJson = irrelevantConv.asJson
    assert(irrelevantJson.noSpaces.contains("\"containsEvidence\":false"))
    
    // Test with containsEvidence = None (default)
    val defaultConv = Conversation(
      messages = List(Message("user", "Default message")),
      id = Some("default-conv")
    )
    
    val defaultJson = defaultConv.asJson
    // When None, the field appears as null in JSON
    assert(defaultJson.noSpaces.contains("\"containsEvidence\":null"))
  }

  test("Parse JSON with containsEvidence field") {
    val jsonWithContainsEvidence = """
    {
      "messages": [
        {
          "speaker": "user",
          "text": "Test message"
        }
      ],
      "id": "test-conv",
      "containsEvidence": true
    }
    """
    
    val result = decode[Conversation](jsonWithContainsEvidence)
    assert(result.isRight)
    
    val conversation = result.getOrElse(throw new Exception("Should not fail"))
    assert(conversation.containsEvidence.contains(true))
  }

  test("Old data compatibility - missing containsEvidence field") {
    // JSON without containsEvidence field (old data)
    val oldJson = """
    {
      "messages": [
        {
          "speaker": "user",
          "text": "Old format message"
        }
      ],
      "id": "old-format-conv"
    }
    """
    
    val result = decode[Conversation](oldJson)
    assert(result.isRight, "Should parse old data without containsEvidence field")
    
    val conversation = result.getOrElse(throw new Exception("Should not fail"))
    assert(conversation.containsEvidence.isEmpty, "containsEvidence should be None for old data")
  }
}