package com.salesforce.crmmembench.memory.mem0

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.questions.evidence.Message

class Mem0ErrorDiagnosticsTest extends AnyFunSuite with Mem0TestHelper {
  
  test("Diagnose connection errors with detailed logging") {
    runIfMem0Enabled {
      println("\n=== Mem0 Error Diagnostics Test ===")
      
      // First, ensure containers are stopped to simulate startup scenario
      println("🛑 Ensuring clean state...")
      try {
        Mem0DockerManager.shutdown()
        Thread.sleep(3000)
      } catch {
        case e: Exception => println(s"Shutdown failed: ${e.getMessage}")
      }
    
      // Create a client - this should trigger Docker startup
      println("\n📝 Creating Mem0 client (should start Docker)...")
      val client = new Mem0Client()
      
      // Initialize - this should start containers
      println("\n🚀 Initializing client...")
      client.initialize()
      
      // Immediately try to use it (containers might still be starting)
      println("\n📤 Attempting to post memory immediately...")
      val conversation = Conversation(
        messages = List(
          Message("user", "Test message for diagnostics"),
          Message("assistant", "Test response")
        ),
        id = Some("test-diagnostics")
      )
      
      try {
        val result = client.postMemory(conversation, "test-user-diagnostics")
        println(s"✅ Success: $result")
      } catch {
        case e: Exception =>
          println(s"❌ Failed as expected. Full error details:")
          println(s"   Exception type: ${e.getClass.getName}")
          println(s"   Message: ${e.getMessage}")
          if (e.getCause != null) {
            println(s"   Cause: ${e.getCause}")
          }
          e.printStackTrace()
      }
      
      // Check health status
      println("\n📊 Service health check:")
      println(s"Service healthy: ${Mem0DockerManager.isHealthy()}")
      
      // Wait a bit and try again
      println("\n⏳ Waiting 10 seconds for containers to stabilize...")
      Thread.sleep(10000)
      
      println("\n📤 Attempting again after wait...")
      try {
        val result = client.postMemory(conversation, "test-user-diagnostics-2")
        println(s"✅ Success on retry: $result")
        
        // Clean up
        client.deleteAllMemories("test-user-diagnostics-2")
      } catch {
        case e: Exception =>
          println(s"❌ Still failing after wait:")
          println(s"   ${e.getClass.getSimpleName}: ${e.getMessage}")
      }
    }
  }
  
  test("Test connection timeout vs connection refused") {
    runIfMem0Enabled {
      println("\n=== Connection Error Types Test ===")
      
      // Ensure containers are running first
      Mem0DockerManager.initialize()
      Thread.sleep(5000)
      
      // Test 1: Normal operation (should work)
      println("\n1️⃣ Testing normal operation...")
      val client1 = new Mem0Client("http://localhost:8888")
      try {
        client1.reset()
        println("   ✅ Normal operation works")
      } catch {
        case e: Exception =>
          println(s"   ❌ Normal operation failed: ${e.getMessage}")
      }
      
      // Test 2: Wrong port (connection refused)
      println("\n2️⃣ Testing wrong port (should get connection refused)...")
      val client2 = new Mem0Client("http://localhost:9999")
      try {
        client2.reset()
        println("   ❌ Should have failed!")
      } catch {
        case e: Exception =>
          println(s"   ✅ Got expected error:")
          println(s"      Type: ${e.getClass.getSimpleName}")
          println(s"      Message: ${e.getMessage}")
          if (e.getCause != null) {
            println(s"      Cause: ${e.getCause.getClass.getSimpleName} - ${e.getCause.getMessage}")
          }
      }
      
      // Test 3: Non-existent host (timeout)
      println("\n3️⃣ Testing non-existent host (should timeout)...")
      val client3 = new Mem0Client("http://192.168.99.99:8888")
      try {
        client3.reset()
        println("   ❌ Should have failed!")
      } catch {
        case e: Exception =>
          println(s"   ✅ Got expected error:")
          println(s"      Type: ${e.getClass.getSimpleName}")
          println(s"      Message: ${e.getMessage}")
          if (e.getCause != null) {
            println(s"      Cause: ${e.getCause.getClass.getSimpleName} - ${e.getCause.getMessage}")
          }
      }
    }
  }
}