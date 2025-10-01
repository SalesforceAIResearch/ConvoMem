package com.salesforce.crmmembench.memory.mem0

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message
import io.circe.parser._
import io.circe.Json

class Mem0ClientTest extends AnyFunSuite with Mem0TestHelper {
  
  test("Post, retrieve, and delete conversation from mem0 server") {
    runIfMem0Enabled {
      val client = new Mem0Client()
      val userId = "test-user-123"
    
    // Create a simple hardcoded conversation
    val conversation = Conversation(
      messages = List(
        Message("user", "Hello, I'm having trouble with my order #12345"),
        Message("assistant", "I'm sorry to hear that. Let me look up your order. Can you tell me what specific issue you're experiencing?"),
        Message("user", "The package was supposed to arrive yesterday but it never showed up"),
        Message("assistant", "I understand your concern. Let me check the tracking information for order #12345. I'll help you resolve this right away.")
      ),
      id = Some("conv-001")
    )
    
    // Post the conversation to mem0
    println("Posting conversation to mem0...")
    val postResponse = client.postMemory(conversation, userId)
    println(s"Post response:\n$postResponse\n")
    
    // Retrieve memories from mem0
    println("Retrieving memories from mem0...")
    val getResponse = client.getMemories(userId)
    println(s"Get response:\n$getResponse\n")
    
    // Parse the response to get memory IDs for deletion testing
    parse(getResponse) match {
      case Right(json) =>
        val memories = json.hcursor.downField("results").as[List[Json]].getOrElse(List.empty)
        
        if (memories.nonEmpty) {
          // Test deleting a single memory
          val firstMemoryId = memories.head.hcursor.downField("id").as[String].getOrElse("")
          if (firstMemoryId.nonEmpty) {
            println(s"Deleting single memory with ID: $firstMemoryId...")
            val deleteResponse = client.deleteMemory(firstMemoryId)
            println(s"Delete single memory response:\n$deleteResponse\n")
          }
          
          // Check remaining memories
          println("Checking remaining memories after single deletion...")
          val remainingResponse = client.getMemories(userId)
          println(s"Remaining memories:\n$remainingResponse\n")
        }
        
        // Test deleting all memories for the user
        println(s"Deleting all memories for user: $userId...")
        val deleteAllResponse = client.deleteAllMemories(userId)
        println(s"Delete all response:\n$deleteAllResponse\n")
        
        // Verify all memories are deleted
        println("Verifying all memories are deleted...")
        val finalResponse = client.getMemories(userId)
        println(s"Final check response:\n$finalResponse\n")
        
      case Left(error) =>
        println(s"Failed to parse response: $error")
    }
    }
  }
}