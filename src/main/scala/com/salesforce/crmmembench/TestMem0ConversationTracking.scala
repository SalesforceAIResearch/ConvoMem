package com.salesforce.crmmembench

import com.salesforce.crmmembench.memory.mem0.Mem0MemoryAnswerer
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message
import java.util.UUID

/**
 * Standalone test to verify mem0 conversation ID tracking works correctly
 */
object TestMem0ConversationTracking {
  def main(args: Array[String]): Unit = {
    println("🧪 Testing Mem0 Conversation ID Tracking...")
    
    try {
      // Initialize mem0
      val answerer = new Mem0MemoryAnswerer()
      answerer.initialize()
      
      // Create test conversations with specific IDs
      val relevantId = s"test-relevant-${UUID.randomUUID()}"
      val irrelevantId = s"test-irrelevant-${UUID.randomUUID()}"
      
      val relevantConv = Conversation(
        id = Some(relevantId),
        messages = List(
          Message("user", "My favorite programming language is Scala."),
          Message("assistant", "I'll remember that your favorite programming language is Scala.")
        )
      )
      
      val irrelevantConv = Conversation(
        id = Some(irrelevantId),
        messages = List(
          Message("user", "What's the weather like?"),
          Message("assistant", "I don't have access to weather data.")
        )
      )
      
      println(s"📝 Adding conversations:")
      println(s"   - Relevant: $relevantId")
      println(s"   - Irrelevant: $irrelevantId")
      
      // Add conversations
      answerer.addConversations(List(relevantConv, irrelevantConv))
      
      // Wait a bit for indexing
      Thread.sleep(3000)
      
      // Query about programming language
      println("\n🔍 Querying: 'What is my favorite programming language?'")
      val answerResult = answerer.answerQuestion("What is my favorite programming language?", "test-tracking-1")
      val answerOpt = answerResult.answer
      val retrievedIds = answerResult.retrievedConversationIds
      
      println(s"\n📊 Results:")
      println(s"   Answer: ${answerOpt.getOrElse("No answer")}")
      println(s"   Retrieved IDs: ${retrievedIds.mkString(", ")}")
      println(s"   Contains relevant: ${retrievedIds.contains(relevantId)}")
      println(s"   Contains irrelevant: ${retrievedIds.contains(irrelevantId)}")
      
      if (retrievedIds.contains(relevantId) && !retrievedIds.contains(irrelevantId)) {
        println("\n✅ SUCCESS: Retrieved the relevant conversation and not the irrelevant one!")
      } else if (retrievedIds.isEmpty) {
        println("\n⚠️  WARNING: No conversation IDs were retrieved. This might mean:")
        println("   - The mem0 API doesn't return run_id in search results")
        println("   - Or the search didn't find any relevant memories")
      } else {
        println("\n❌ ISSUE: Unexpected retrieval pattern")
      }
      
      // Test error handling
      println("\n🧪 Testing error handling for conversations without IDs...")
      val convWithoutId = Conversation(
        id = None,
        messages = List(Message("user", "Test"))
      )
      
      try {
        answerer.addConversation(convWithoutId)
        println("❌ ERROR: Should have thrown exception for conversation without ID")
      } catch {
        case _: IllegalArgumentException =>
          println("✅ SUCCESS: Correctly threw exception for conversation without ID")
        case e: Exception =>
          println(s"❌ ERROR: Wrong exception type: ${e.getClass.getName}")
      }
      
      // Cleanup
      answerer.cleanup()
      println("\n🧹 Cleanup complete")
      
    } catch {
      case e: Exception =>
        println(s"\n❌ Test failed with exception: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}