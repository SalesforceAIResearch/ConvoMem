package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.evaluation.memory.{MemoryAnswererFactory, ExtractedContextMemoryFactory}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator

/**
 * Base class for extracted context memory evaluations.
 * 
 * This evaluator is configured for extracted context memory system with:
 * - Standard context sizes for evaluating context extraction
 * - Extracted context memory type pre-configured
 * - Parallel execution for efficient processing
 * 
 * The extracted context approach:
 * - Stage 1: Extracts all relevant information from conversations using a helper model
 * - Stage 2: Answers questions based on the extracted context
 * - Better handles targeted information retrieval
 * - Reduces context size for final answer generation
 * 
 * Extend this class for any extracted context evaluation scenarios.
 */
abstract class ExtractedContextEvaluator extends Evaluator {
  
  // Abstract evidence generator that concrete implementations must provide
  def evidenceGenerator: EvidenceGenerator
  
  // Use standard context sizes for extracted context processing
  def contextSizes: List[Int] = Config.Evaluation.CONTEXT_SIZES
  
  // Create test cases generator using the evidence generator and context sizes
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(evidenceGenerator, contextSizes)
  
  // Pre-configured for extracted context memory system
  override def memoryFactory: MemoryAnswererFactory = ExtractedContextMemoryFactory
  
  // Use standard thread count for parallel processing
  override def testCaseThreads: Int = 40
  
  // Override main method to indicate extracted context evaluation
  override def main(args: Array[String]): Unit = {
    println("═" * 80)
    println("EXTRACTED CONTEXT EVALUATION")
    println("═" * 80)
    println("Using extracted context memory processing (two-stage approach)")
    println("Stage 1: Extract relevant information from all conversations")
    println("Stage 2: Answer questions based on extracted context")
    println()
    
    super.main(args)
  }
}