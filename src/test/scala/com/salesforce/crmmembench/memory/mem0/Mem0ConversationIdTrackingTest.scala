package com.salesforce.crmmembench.memory.mem0

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message
import java.util.UUID

class Mem0ConversationIdTrackingTest extends AnyFunSuite with Mem0TestHelper {
  
  test("Mem0 should store and retrieve conversations with run_id") {
    runIfMem0Enabled {
      // Initialize mem0
      val answerer = new Mem0MemoryAnswerer()
      answerer.initialize()
      
      // Create test conversations with specific IDs
      val relevantId1 = s"test-relevant-${UUID.randomUUID()}"
      val relevantId2 = s"test-relevant-${UUID.randomUUID()}"
      val irrelevantId = s"test-irrelevant-${UUID.randomUUID()}"
      
      val relevantConv1 = Conversation(
        id = Some(relevantId1),
        messages = List(
          Message("user", "My favorite color is blue."),
          Message("assistant", "I'll remember that your favorite color is blue.")
        )
      )
      
      val relevantConv2 = Conversation(
        id = Some(relevantId2),
        messages = List(
          Message("user", "I also like the color green."),
          Message("assistant", "Noted! You like both blue and green.")
        )
      )
      
      val irrelevantConv = Conversation(
        id = Some(irrelevantId),
        messages = List(
          Message("user", "What's the weather like today?"),
          Message("assistant", "I don't have access to current weather data.")
        )
      )
      
      try {
        // Add all conversations
        answerer.addConversations(List(relevantConv1, irrelevantConv, relevantConv2))
        
        // Query about colors (should retrieve relevant conversations)
        val answerResult = answerer.answerQuestion("What are my favorite colors?", "tracking-test-1")
        val answerOpt = answerResult.answer
        val retrievedIds = answerResult.retrievedConversationIds
        
        assert(answerOpt.isDefined, "Should get an answer")
        println(s"Answer: ${answerOpt.get}")
        println(s"Retrieved conversation IDs: $retrievedIds")
        
        // Check that we retrieved at least one relevant conversation
        val relevantSet = Set(relevantId1, relevantId2)
        val relevantRetrieved = retrievedIds.filter(relevantSet.contains)
        assert(relevantRetrieved.nonEmpty, s"Should retrieve at least one relevant conversation, but got: $retrievedIds")
        
        // The irrelevant conversation should not be retrieved
        assert(!retrievedIds.contains(irrelevantId), s"Should not retrieve irrelevant conversation")
        
        println(s"✅ Test passed! Retrieved ${relevantRetrieved.size} relevant conversations out of ${retrievedIds.size} total")
        
      } finally {
        // Clean up
        answerer.cleanup()
      }
    }
  }
  
  test("Mem0 should handle conversations without IDs by throwing exception") {
    runIfMem0Enabled {
      val answerer = new Mem0MemoryAnswerer()
      answerer.initialize()
      
      val convWithoutId = Conversation(
        id = None,
        messages = List(Message("user", "Hello"))
      )
      
      try {
        assertThrows[IllegalArgumentException] {
          answerer.addConversation(convWithoutId)
        }
      } finally {
        answerer.cleanup()
      }
    }
  }
}