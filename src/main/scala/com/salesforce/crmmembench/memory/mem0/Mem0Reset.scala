package com.salesforce.crmmembench.memory.mem0

/**
 * Utility object to reset/clear all memories in the mem0 server.
 * 
 * Usage: ./gradlew run -PmainClass=com.salesforce.crmmembench.memory.mem0.Mem0Reset
 * 
 * This is useful for:
 * - Clearing transaction errors
 * - Starting fresh with a clean memory state
 * - Resetting after benchmark runs
 */
object Mem0Reset {
  
  def main(args: Array[String]): Unit = {
    println("=== Mem0 Reset Utility ===")
    println()
    
    val client = new Mem0Client()
    
    try {
      println("Resetting all memories in mem0 server...")
      val response = client.reset()
      
      println("Reset successful!")
      println(s"Response: $response")
      
      // Verify the reset worked by trying to get memories for a test user
      println("\nVerifying reset...")
      try {
        val testResponse = client.getMemories("test-verification-user")
        println(s"Verification response: $testResponse")
        
        if (testResponse.contains("\"results\":[]")) {
          println("\n✓ Reset verified: No memories found")
        } else {
          println("\n⚠ Warning: Some memories may still exist")
        }
      } catch {
        case e: Exception =>
          println(s"\n⚠ Could not verify reset: ${e.getMessage}")
      }
      
    } catch {
      case e: Exception =>
        println(s"Failed to reset mem0 server: ${e.getMessage}")
        println("\nPossible causes:")
        println("- mem0 server is not running")
        println("- Server is at a different address (default: http://localhost:8888)")
        println("- Network connectivity issues")
        System.exit(1)
    }
  }
}