package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.questions.evidence.{Conversation, GeneratedEvidenceCore, Message}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ConversationValidatorTest extends AnyFunSpec with Matchers {

  describe("ConversationValidator") {
    
    describe("validateConversations") {
      
      it("should pass when all evidence messages are found exactly once in correct order") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "My favorite color is blue."),
            Message("User", "I really love the color blue.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hi there!"),
            Message("Assistant", "Hello! How can I help you?"),
            Message("User", "My favorite color is blue."),
            Message("Assistant", "Blue is a great color!")
          )),
          Conversation(messages = List(
            Message("User", "Let's talk about colors."),
            Message("Assistant", "Sure, what about colors?"),
            Message("User", "I really love the color blue."),
            Message("Assistant", "That's nice!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should fail when number of conversations doesn't match evidence messages") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "My favorite color is blue."),
            Message("User", "I really love the color blue.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hi there!"),
            Message("User", "My favorite color is blue.")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        result.errors should contain("Number of conversations (1) doesn't match number of evidence messages (2)")
        result.failureCategories should contain(ValidationFailureCategory.CONVERSATION_COUNT_MISMATCH)
      }
      
      it("should fail when evidence message is not found in any conversation") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "My favorite color is blue.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hi there!"),
            Message("Assistant", "Hello!"),
            Message("User", "I like colors."),
            Message("Assistant", "That's nice!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        result.errors should contain("Evidence message 0 not found in any conversation: My favorite color is blue.")
        result.failureCategories should contain(ValidationFailureCategory.EVIDENCE_NOT_FOUND)
      }
      
      it("should pass when evidence message is found with minor differences using Levenshtein distance") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "My favorite color is blue.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hi there!"),
            Message("Assistant", "Hello!"),
            Message("User", "My favorite color is blue!"), // Minor difference: exclamation mark
            Message("Assistant", "Blue is nice!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should fail when evidence message differs too much (exceeds 15% threshold)") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "My favorite color is blue.")  // 26 characters
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hi there!"),
            Message("Assistant", "Hello!"),
            Message("User", "I absolutely adore the color blue more than anything."), // Very different (>15% threshold)
            Message("Assistant", "Blue is nice!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        result.errors should contain("Evidence message 0 not found in any conversation: My favorite color is blue.")
      }
      
      it("should fail when evidence message appears in multiple conversations") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "My favorite color is blue."),
            Message("User", "I really love blue.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "My favorite color is blue."),
            Message("Assistant", "Nice!")
          )),
          Conversation(messages = List(
            Message("User", "My favorite color is blue."), // Duplicate
            Message("User", "I really love blue."),
            Message("Assistant", "Great!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        result.errors should contain("Evidence message 0 found in multiple conversations: [0, 1]")
        result.failureCategories should contain(ValidationFailureCategory.EVIDENCE_IN_MULTIPLE_CONVERSATIONS)
      }
      
      it("should match evidence with different speakers correctly") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What did I recommend?",
          answer = "Python",
          message_evidences = List(
            Message("Assistant", "I recommend learning Python.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "What programming language should I learn?"),
            Message("Assistant", "I recommend learning Python."),
            Message("User", "Thanks!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should fail when speaker doesn't match") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What did I say?",
          answer = "Python",
          message_evidences = List(
            Message("User", "I recommend learning Python.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "What should I learn?"),
            Message("Assistant", "I recommend learning Python."), // Wrong speaker
            Message("User", "Thanks!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        result.errors should contain("Evidence message 0 not found in any conversation: I recommend learning Python.")
      }
      
      it("should handle empty evidence messages correctly") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your favorite color?",
          answer = "I don't know",
          message_evidences = List()
        )
        
        val conversations = List()
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should fail when invalid speakers are used in conversations") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your name?",
          answer = "John",
          message_evidences = List(
            Message("User", "My name is John.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("Human", "Hello!"), // Invalid speaker
            Message("AI", "Hi there!"), // Invalid speaker
            Message("User", "My name is John."),
            Message("Assistant", "Nice to meet you!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        result.errors should contain("Invalid speakers found in conversation 0: Human, AI. Only 'User' and 'Assistant' are valid speakers.")
        result.failureCategories should contain(ValidationFailureCategory.INVALID_SPEAKERS)
      }
      
      it("should pass when only valid speakers (User and Assistant) are used") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is your name?",
          answer = "John",
          message_evidences = List(
            Message("User", "My name is John.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hello!"),
            Message("Assistant", "Hi there!"),
            Message("User", "My name is John."),
            Message("Assistant", "Nice to meet you, John!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should pass when evidence is embedded in a longer conversation message") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is my favorite color?",
          answer = "Blue",
          message_evidences = List(
            Message("User", "my favorite color is blue.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Let me tell you about my preferences."),
            Message("Assistant", "I'd love to hear about them!"),
            Message("User", "Well, for starters, my favorite color is blue. I've always loved how calming it is."),
            Message("Assistant", "Blue is a wonderful color!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should pass with reverse partial match when conversation message is substantial part of evidence") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What project am I working on?",
          answer = "The new mobile app",
          message_evidences = List(
            Message("User", "I'm currently working on the new mobile app for our company.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "How's your day going?"),
            Message("Assistant", "Great! What are you up to?"),
            Message("User", "I'm currently working on the new mobile app for our"), // Truncated but >80%
            Message("Assistant", "That sounds exciting!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should pass with dynamic Levenshtein threshold based on message length") {
        val longMessage = "I've been learning Python for the past six months and really enjoying the language's simplicity and powerful libraries."
        val evidenceCore = GeneratedEvidenceCore(
          question = "What programming language are you learning?",
          answer = "Python",
          message_evidences = List(
            Message("User", longMessage)
          )
        )
        
        // Message with ~12% difference (within 15% threshold)
        val similarMessage = "I've been studying Python for the last six months and really enjoying the language's simplicity and powerful libraries."
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Let me tell you about my coding journey."),
            Message("Assistant", "Please do!"),
            Message("User", similarMessage),
            Message("Assistant", "Python is great for beginners!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should handle single evidence message with exact match") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What is my name?",
          answer = "John",
          message_evidences = List(
            Message("User", "My name is John.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "Hello!"),
            Message("Assistant", "Hi there!"),
            Message("User", "My name is John."),
            Message("Assistant", "Nice to meet you, John!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe true
        result.errors shouldBe empty
      }
      
      it("should track which evidence messages were not found") {
        val evidenceCore = GeneratedEvidenceCore(
          question = "What are my hobbies?",
          answer = "Reading and swimming",
          message_evidences = List(
            Message("User", "I love reading books."),
            Message("User", "Swimming is my favorite sport."),
            Message("User", "I also enjoy hiking.")
          )
        )
        
        val conversations = List(
          Conversation(messages = List(
            Message("User", "I love reading books."),
            Message("Assistant", "That's great!")
          )),
          Conversation(messages = List(
            Message("User", "I enjoy walking."), // Wrong - should be swimming
            Message("Assistant", "Exercise is good!")
          )),
          Conversation(messages = List(
            Message("User", "I also enjoy hiking."),
            Message("Assistant", "Hiking is fun!")
          ))
        )
        
        val result = ConversationValidator.validateConversations(evidenceCore, conversations)
        result.isValid shouldBe false
        // We expect 2 errors:
        // 1. Evidence message 1 not found in any conversation
        // 2. Evidence message 2 found in multiple conversations (due to fuzzy matching with conversation 1)
        result.errors.size shouldBe 2
        result.errors should contain("Evidence message 1 not found in any conversation: Swimming is my favorite sport.")
        result.errors should contain("Evidence message 2 found in multiple conversations: [1, 2]")
      }
    }
    
    describe("levenshteinDistance") {
      it("should return 0 for identical strings") {
        ConversationValidator.levenshteinDistance("hello", "hello") shouldBe 0
      }
      
      it("should return correct distance for single character change") {
        ConversationValidator.levenshteinDistance("hello", "hallo") shouldBe 1
      }
      
      it("should return correct distance for insertion") {
        ConversationValidator.levenshteinDistance("hello", "helllo") shouldBe 1
      }
      
      it("should return correct distance for deletion") {
        ConversationValidator.levenshteinDistance("hello", "helo") shouldBe 1
      }
      
      it("should return correct distance for multiple changes") {
        ConversationValidator.levenshteinDistance("kitten", "sitting") shouldBe 3
      }
      
      it("should handle empty strings") {
        ConversationValidator.levenshteinDistance("", "") shouldBe 0
        ConversationValidator.levenshteinDistance("hello", "") shouldBe 5
        ConversationValidator.levenshteinDistance("", "hello") shouldBe 5
      }
      
      it("should be case sensitive") {
        ConversationValidator.levenshteinDistance("Hello", "hello") shouldBe 1
      }
    }
  }
}