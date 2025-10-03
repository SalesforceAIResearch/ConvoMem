package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.evaluation.memory.{MemoryAnswererFactory, Mem0MemoryFactory}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator

/**
 * Base class for mem0-specific evaluations.
 * 
 * This evaluator is configured for mem0 memory system with:
 * - Smaller context sizes (CONTEXT_SIZES_SMALL) to handle mem0's limitations
 * - Single-threaded execution to avoid overwhelming mem0/PostgreSQL
 * - Mem0 memory type pre-configured
 * 
 * Extend this class for any mem0-based evaluation scenarios.
 */
abstract class Mem0Evaluator extends Evaluator {
  
  // Abstract evidence generator that concrete implementations must provide
  def evidenceGenerator: EvidenceGenerator
  
  // Use smaller context sizes for mem0
  def contextSizes: List[Int] = Config.Evaluation.CONTEXT_SIZES
  
  // Create test cases generator using the evidence generator and context sizes
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(evidenceGenerator, contextSizes)
  
  // Pre-configured for mem0 memory system
  override def memoryFactory: MemoryAnswererFactory = Mem0MemoryFactory
  
  // Reduced threads to avoid overwhelming mem0
  override def testCaseThreads: Int = 20
  
  // Override main method to check if mem0 is available
  override def main(args: Array[String]): Unit = {
    // For production runs, check if mem0 is actually available
    println("═" * 80)
    println("MEM0 EVALUATION")
    println("═" * 80)
    println("Checking mem0 availability...")
    
    try {
      // Initialize mem0 (this starts Docker containers if needed)
      import com.salesforce.crmmembench.memory.mem0.Mem0Client
      val testClient = new Mem0Client()
      println("Initializing mem0 (starting Docker containers if needed)...")
      testClient.initialize()
      
      // Now verify it's working
      println("Verifying mem0 is operational...")
      testClient.getMemories("test-user-health-check")
      println("✅ Mem0 is available and healthy")
    } catch {
      case e: Exception =>
        println("❌ Failed to initialize or connect to mem0!")
        println(s"Error: ${e.getMessage}")
        throw new RuntimeException("Mem0 is not available for production evaluation", e)
    }
    
    super.main(args)
  }
}