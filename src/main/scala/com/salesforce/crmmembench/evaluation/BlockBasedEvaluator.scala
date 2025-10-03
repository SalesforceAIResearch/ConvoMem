package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.evaluation.memory.{MemoryAnswererFactory, BlockBasedMemoryFactory}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator

/**
 * Base class for block-based memory evaluations.
 * 
 * This evaluator is configured for block-based memory system with:
 * - Standard context sizes for evaluating block processing
 * - Block-based memory type pre-configured
 * - Parallel execution for efficient processing
 * 
 * The block-based approach:
 * - Processes conversations in blocks of 10
 * - Extracts relevant information from each block independently
 * - Aggregates extracted information for final answer generation
 * - Better handles very long conversation lists
 * 
 * Extend this class for any block-based evaluation scenarios.
 */
abstract class BlockBasedEvaluator extends Evaluator {
  
  // Abstract evidence generator that concrete implementations must provide
  def evidenceGenerator: EvidenceGenerator
  
  // Use standard context sizes for block-based processing
  def contextSizes: List[Int] = Config.Evaluation.CONTEXT_SIZES
  
  // Create test cases generator using the evidence generator and context sizes
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(evidenceGenerator, contextSizes)
  
  // Pre-configured for block-based memory system
  override def memoryFactory: MemoryAnswererFactory = BlockBasedMemoryFactory
  
  // Use standard thread count for parallel block processing
  override def testCaseThreads: Int = 40
  
  // Override main method to indicate block-based evaluation
  override def main(args: Array[String]): Unit = {
    println("═" * 80)
    println("BLOCK-BASED EVALUATION")
    println("═" * 80)
    println("Using block-based memory processing (blocks of 10 conversations)")
    println("This approach processes conversations in chunks for better scalability")
    println()
    
    super.main(args)
  }
}