package com.salesforce.crmmembench.memory.mem0

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message

class Mem0DetailedDiagnosticTest extends AnyFunSuite with Mem0TestHelper {
  
  test("Detailed mem0 operation diagnostics") {
    runIfMem0Enabled {
      println("\n=== Detailed Mem0 Diagnostics ===")
      
      // 1. Ensure fresh start
      println("\n1. Starting fresh...")
      Mem0DockerManager.emergencyRestart()
      Thread.sleep(20000) // Extra time for clean start
    
      // 2. Check service health
      println("\n2. Checking service health...")
      println(s"Service healthy: ${Mem0DockerManager.isHealthy()}")
    
      // Test direct HTTP connection
      println("\n3. Testing direct HTTP connection...")
      try {
        val healthCheck = scala.io.Source.fromURL("http://localhost:8888/health").mkString
        println(s"✅ Health check response: $healthCheck")
      } catch {
        case e: Exception =>
          println(s"❌ Health check failed: ${e.getMessage}")
      }
    
      // 3. Create client and test step by step
      println("\n4. Creating client...")
      val client = new Mem0Client()
      val userId = "diagnostic-test-user"
    
      // 4. Post a simple memory
      println("\n5. Posting memory...")
      val conversation = Conversation(
        messages = List(
          Message("user", "My name is DiagnosticTestUser and I work at TestCompany"),
          Message("assistant", "Hello DiagnosticTestUser! How can I help you at TestCompany?")
        ),
        id = Some("diag-conv-1")
      )
    
      try {
        val postResult = client.postMemory(conversation, userId)
        println(s"✅ Post result: $postResult")
        
        // Parse the response to get memory ID if available
        val memoryIdPattern = """"id":\s*"([^"]+)"""".r
        val memoryId = memoryIdPattern.findFirstMatchIn(postResult).map(_.group(1))
        println(s"   Memory ID: ${memoryId.getOrElse("not found")}")
        
      } catch {
        case e: Exception =>
          println(s"❌ Post failed:")
          e.printStackTrace()
          fail("Post memory failed")
      }
    
      // 5. Wait and retrieve
      println("\n6. Waiting for indexing...")
      Thread.sleep(3000)
    
      println("\n7. Retrieving memories...")
      try {
        val memories = client.getMemories(userId)
        println(s"✅ Memories retrieved (${memories.length} chars):")
        println(s"   ${memories.take(500)}")
        
        // Check if memories contain our data
        if (memories.contains("DiagnosticTestUser")) {
          println("   ✅ Found user name in memories")
        } else {
          println("   ⚠️  User name not found in memories")
        }
        
      } catch {
        case e: Exception =>
          println(s"❌ Retrieval failed:")
          e.printStackTrace()
      }
    
      // 6. Test search
      println("\n8. Testing search...")
      try {
        val searchResult = client.searchMemories("DiagnosticTestUser", Some(userId))
        println(s"✅ Search result (${searchResult.length} chars):")
        println(s"   ${searchResult.take(500)}")
        
        // Parse search results
        if (searchResult.contains("DiagnosticTestUser") || searchResult.contains("results")) {
          println("   ✅ Search appears to be working")
        } else {
          println("   ⚠️  Search returned unexpected results")
        }
        
      } catch {
        case e: Exception =>
          println(s"❌ Search failed:")
          e.printStackTrace()
      }
    
      // 7. Test with MemoryAnswerer
      println("\n9. Testing with MemoryAnswerer...")
      val answerer = new Mem0MemoryAnswerer()
      
      try {
        // Add conversation
        answerer.addConversation(conversation)
        println("✅ Conversation added to answerer")
        
        Thread.sleep(3000)
        
        // Try to answer
        val answerResult = answerer.answerQuestion("What is the user's name?", "diagnostic-test-1")
        val answer = answerResult.answer
        println(s"Answer: ${answer.getOrElse("None")}")
        
        // Also try searching directly
        println("\n10. Direct search through answerer's client...")
        val answererUserId = answerer.getUserId
        val directSearch = answerer.mem0Client.searchMemories("name", Some(answererUserId))
        println(s"Direct search result: ${directSearch.take(300)}")
        
      } catch {
        case e: Exception =>
          println(s"❌ Answerer test failed:")
          e.printStackTrace()
      } finally {
        answerer.cleanup()
      }
      
      println("\n=== Diagnostics Complete ===")
    }
  }
}