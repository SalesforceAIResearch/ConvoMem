package com.salesforce.crmmembench.memory.mem0

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message

class Mem0SingleThreadTest extends AnyFunSuite with BeforeAndAfterAll with Mem0TestHelper {
  
  override def beforeAll(): Unit = {
    super.beforeAll()
    if (Config.RUN_MEM0_TESTS) {
      println("\n=== Setting up Mem0 Test Environment ===")
      
      // Ensure clean start
      println("1. Shutting down any existing containers...")
      Mem0DockerManager.shutdown()
      Thread.sleep(3000)
      
      println("2. Starting fresh containers...")
      Mem0DockerManager.initialize()
      
      println("3. Waiting for services to be fully ready...")
      Thread.sleep(15000) // Give extra time for all services to stabilize
      
      println("4. Checking if service is healthy...")
      println(s"Service healthy: ${Mem0DockerManager.isHealthy()}")
      
      println("=== Setup Complete ===\n")
    }
  }
  
  override def afterAll(): Unit = {
    if (Config.RUN_MEM0_TESTS) {
      println("\n=== Cleaning up ===")
      Mem0DockerManager.shutdown()
    }
    super.afterAll()
  }
  
  test("Single-threaded basic mem0 operations") {
    runIfMem0Enabled {
      println("\n=== Test: Single-threaded Mem0 Operations ===")
      
      // Create client
      println("1. Creating Mem0 client...")
      val client = new Mem0Client()
    
      // Test 1: Simple post memory
      println("\n2. Testing simple memory post...")
      val userId = s"single-thread-test-${System.currentTimeMillis()}"
      val conversation1 = Conversation(
        messages = List(
          Message("user", "My name is SingleThreadTestUser"),
          Message("assistant", "Nice to meet you, SingleThreadTestUser!")
        ),
        id = Some("single-test-1")
      )
      
      try {
        val result = client.postMemory(conversation1, userId)
        println(s"✅ Successfully posted memory: ${result.take(100)}...")
      } catch {
        case e: Exception =>
          println(s"❌ Failed to post memory:")
          e.printStackTrace()
          fail(s"Single memory post failed: ${e.getMessage}")
      }
      
      // Give mem0 time to index
      Thread.sleep(2000)
      
      // Test 2: Retrieve memories
      println("\n3. Testing memory retrieval...")
      try {
        val memories = client.getMemories(userId)
        println(s"✅ Retrieved memories: ${memories.take(200)}...")
        assert(memories.nonEmpty, "Should have retrieved some memories")
      } catch {
        case e: Exception =>
          println(s"❌ Failed to retrieve memories:")
          e.printStackTrace()
          fail(s"Memory retrieval failed: ${e.getMessage}")
      }
      
      // Test 3: Search memories
      println("\n4. Testing memory search...")
      try {
        val searchResult = client.searchMemories("SingleThreadTestUser", Some(userId))
        println(s"✅ Search result: ${searchResult.take(200)}...")
        assert(searchResult.contains("SingleThreadTestUser"), "Search should find the user name")
      } catch {
        case e: Exception =>
          println(s"❌ Failed to search memories:")
          e.printStackTrace()
          fail(s"Memory search failed: ${e.getMessage}")
      }
      
      // Test 4: Delete memories
      println("\n5. Testing memory deletion...")
      try {
        val deleteResult = client.deleteAllMemories(userId)
        println(s"✅ Deleted memories: $deleteResult")
      } catch {
        case e: Exception =>
          println(s"❌ Failed to delete memories:")
          e.printStackTrace()
          fail(s"Memory deletion failed: ${e.getMessage}")
      }
    
      println("\n✅ All single-threaded tests passed!")
    }
  }
  
  test("Sequential memory operations") {
    runIfMem0Enabled {
      println("\n=== Test: Sequential Memory Operations ===")
      
      val client = new Mem0Client()
      val userId = s"sequential-test-${System.currentTimeMillis()}"
      
      // Add multiple conversations sequentially
      val conversations = (1 to 5).map { i =>
        Conversation(
          messages = List(
            Message("user", s"This is message $i"),
            Message("assistant", s"Response to message $i")
          ),
          id = Some(s"seq-test-$i")
        )
      }
      
      println("Adding 5 conversations sequentially...")
      conversations.zipWithIndex.foreach { case (conv, idx) =>
        try {
          print(s"  ${idx + 1}. Adding conversation...")
          client.postMemory(conv, userId)
          println(" ✅")
          Thread.sleep(500) // Small delay between posts
        } catch {
          case e: Exception =>
            println(s" ❌")
            println(s"     Error: ${e.getMessage}")
            if (e.getCause != null) {
              println(s"     Cause: ${e.getCause.getMessage}")
            }
            fail(s"Failed on conversation ${idx + 1}")
        }
      }
      
      println("\n✅ Sequential operations completed successfully!")
      
      // Cleanup
      Thread.sleep(1000)
      client.deleteAllMemories(userId)
    }
  }
  
  test("Test with MemoryAnswerer") {
    runIfMem0Enabled {
      println("\n=== Test: MemoryAnswerer Single Thread ===")
      
      // Create answerer
      println("1. Creating Mem0MemoryAnswerer...")
      val answerer = new Mem0MemoryAnswerer()
      
      println(s"   User ID: ${answerer.getUserId}")
      
      try {
        // Add conversation
        println("\n2. Adding conversation...")
        val conversation = Conversation(
          messages = List(
            Message("user", "I'm a software engineer working on distributed systems"),
            Message("assistant", "That's interesting! Distributed systems are complex but fascinating.")
          ),
          id = Some("answerer-test-1")
        )
        
        answerer.addConversation(conversation)
        println("✅ Conversation added")
        
        // Wait for indexing
        Thread.sleep(2000)
        
        // Ask question
        println("\n3. Asking question...")
        val answerResult = answerer.answerQuestion("What does the user do for work?", "single-thread-test-1")
        val answer = answerResult.answer
        
        answer match {
          case Some(ans) =>
            println(s"✅ Got answer: $ans")
            assert(ans.toLowerCase.contains("software") || ans.toLowerCase.contains("engineer"), 
                   "Answer should mention the user's profession")
          case None =>
            fail("No answer received")
        }
        
        // Cleanup
        println("\n4. Cleaning up...")
        answerer.cleanup()
        println("✅ Cleanup complete")
        
      } catch {
        case e: Exception =>
          println(s"❌ Test failed:")
          e.printStackTrace()
          fail(s"MemoryAnswerer test failed: ${e.getMessage}")
      }
    }
  }
}