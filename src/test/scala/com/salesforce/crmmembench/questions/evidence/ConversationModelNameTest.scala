package com.salesforce.crmmembench.questions.evidence

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.funsuite.AnyFunSuite
import java.util.UUID

class ConversationModelNameTest extends AnyFunSuite {
  
  test("Conversation should serialize and deserialize with model_name field") {
    val conversation = Conversation(
      messages = List(
        Message("User", "Hello, how are you?"),
        Message("Assistant", "I'm doing well, thank you! How can I help you today?")
      ),
      id = Some(UUID.randomUUID().toString),
      containsEvidence = Some(false),
      model_name = Some("gemini-1.5-pro")
    )
    
    val json = conversation.asJson.noSpaces
    assert(json.contains("\"model_name\":\"gemini-1.5-pro\""))
    
    val decoded = decode[Conversation](json)
    assert(decoded.isRight)
    assert(decoded.toOption.get == conversation)
  }
  
  test("Conversation should handle missing model_name field for backwards compatibility") {
    val jsonWithoutModelName = """{
      "messages":[
        {"speaker":"User","text":"Hello"},
        {"speaker":"Assistant","text":"Hi there!"}
      ],
      "id":"123e4567-e89b-12d3-a456-426614174000",
      "containsEvidence":true
    }"""
    
    val decoded = decode[Conversation](jsonWithoutModelName)
    assert(decoded.isRight)
    
    val conversation = decoded.toOption.get
    assert(conversation.messages.length == 2)
    assert(conversation.id == Some("123e4567-e89b-12d3-a456-426614174000"))
    assert(conversation.containsEvidence == Some(true))
    assert(conversation.model_name.isEmpty)
  }
  
  test("Conversation should preserve model_name through copy operation") {
    val original = Conversation(
      messages = List(Message("User", "Test")),
      id = Some("test-id"),
      containsEvidence = Some(true)
    )
    
    val withModelName = original.copy(model_name = Some("claude-3-opus"))
    
    assert(withModelName.messages == List(Message("User", "Test")))
    assert(withModelName.id == Some("test-id"))
    assert(withModelName.containsEvidence == Some(true))
    assert(withModelName.model_name == Some("claude-3-opus"))
  }
  
  test("GeneratedConversations with Conversations containing model_name") {
    val generatedConversations = GeneratedConversations(
      conversations = List(
        Conversation(
          messages = List(Message("User", "Message 1")),
          id = None,
          containsEvidence = None,
          model_name = Some("gpt-4o")
        ),
        Conversation(
          messages = List(Message("User", "Message 2")),
          id = None,
          containsEvidence = None,
          model_name = Some("gemini-1.5-flash")
        )
      )
    )
    
    val json = generatedConversations.asJson.noSpaces
    val decoded = decode[GeneratedConversations](json)
    
    assert(decoded.isRight)
    val decodedConvs = decoded.toOption.get
    assert(decodedConvs.conversations.length == 2)
    assert(decodedConvs.conversations(0).model_name == Some("gpt-4o"))
    assert(decodedConvs.conversations(1).model_name == Some("gemini-1.5-flash"))
  }
  
  test("MessageNormalizer should preserve model_name when normalizing conversations") {
    import com.salesforce.crmmembench.MessageNormalizer
    
    val conversation = Conversation(
      messages = List(
        Message("USER", "Hello"),
        Message("ASSISTANT", "Hi")
      ),
      id = Some("test-id"),
      containsEvidence = Some(true),
      model_name = Some("test-model")
    )
    
    val normalized = MessageNormalizer.normalizeConversation(conversation)
    
    // Messages should be normalized to lowercase
    assert(normalized.messages(0).speaker == "user")
    assert(normalized.messages(1).speaker == "assistant")
    
    // Other fields should be preserved
    assert(normalized.id == Some("test-id"))
    assert(normalized.containsEvidence == Some(true))
    assert(normalized.model_name == Some("test-model"))
  }
}