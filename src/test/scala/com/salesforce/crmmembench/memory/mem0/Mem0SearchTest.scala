package com.salesforce.crmmembench.memory.mem0

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message

class Mem0SearchTest extends AnyFunSuite with Mem0TestHelper {
  
  test("Search memories functionality") {
    runIfMem0Enabled {
      val client = new Mem0Client()
      val userId = "search-test-user"
      
      // First, clean up any existing memories
      println("Cleaning up existing memories...")
      try {
        client.deleteAllMemories(userId)
      } catch {
        case _: Exception => // Ignore if no memories exist
      }
      
      // Create conversations with different topics
      val conversation1 = Conversation(
        messages = List(
          Message("user", "I need to update my shipping address"),
          Message("assistant", "I can help you update your shipping address. What's your new address?"),
          Message("user", "123 Main Street, San Francisco, CA 94105"),
          Message("assistant", "I've updated your shipping address to 123 Main Street, San Francisco, CA 94105")
        ),
        id = Some("conv-addr-001")
      )
      
      val conversation2 = Conversation(
        messages = List(
          Message("user", "What's the status of my refund for order #98765?"),
          Message("assistant", "Let me check the refund status for order #98765"),
          Message("user", "I returned it two weeks ago"),
          Message("assistant", "Your refund for order #98765 was processed 3 days ago. It should appear in your account within 5-7 business days")
        ),
        id = Some("conv-refund-001")
      )
      
      val conversation3 = Conversation(
        messages = List(
          Message("user", "Can you help me track my package for order #55555?"),
          Message("assistant", "I'll track your package for order #55555 right away"),
          Message("user", "It's been in transit for 5 days"),
          Message("assistant", "Your package for order #55555 is currently in Memphis and will be delivered tomorrow by 5 PM")
        ),
        id = Some("conv-track-001")
      )
      
      // Post all conversations
      println("Posting conversations to mem0...")
      client.postMemory(conversation1, userId)
      client.postMemory(conversation2, userId)
      client.postMemory(conversation3, userId)
      
      // Wait a bit for indexing
      Thread.sleep(2000)
      
      // Test search functionality
      println("\n=== Testing search for 'shipping address' ===")
      val searchResult1 = client.searchMemories("shipping address", Some(userId))
      println(s"Search result:\n$searchResult1\n")
      
      println("\n=== Testing search for 'refund' ===")
      val searchResult2 = client.searchMemories("refund", Some(userId))
      println(s"Search result:\n$searchResult2\n")
      
      println("\n=== Testing search for 'order' (should find multiple) ===")
      val searchResult3 = client.searchMemories("order", Some(userId))
      println(s"Search result:\n$searchResult3\n")
      
      println("\n=== Testing search for 'Memphis' (specific detail) ===")
      val searchResult4 = client.searchMemories("Memphis", Some(userId))
      println(s"Search result:\n$searchResult4\n")
      
      // Clean up
      println("\nCleaning up test data...")
      client.deleteAllMemories(userId)
    }
  }
}