package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.{EvidenceGenerator, EvidenceItem}
import com.salesforce.crmmembench.questions.evidence.generation.AnsweringEvaluation

/**
 * Abstract class for generating test cases.
 * 
 * Test case generators are responsible for:
 * - Loading evidence items and conversations
 * - Creating test cases with appropriate combinations of evidence and irrelevant conversations
 * - Supporting different evaluation strategies (e.g., different context sizes)
 * 
 * Implementations can create test cases for different evaluation scenarios:
 * - Single evidence item with varying amounts of irrelevant context
 * - Multiple evidence items tested together
 * - Complex mixing strategies for future requirements
 * 
 * @param evidenceGenerator The evidence generator to use for loading evidence items
 */
abstract class TestCasesGenerator(val evidenceGenerator: EvidenceGenerator) {
  
  /**
   * Generate all test cases for evaluation.
   * 
   * @return List of test cases ready for evaluation
   */
  def generateTestCases(): List[TestCase]
  
  /**
   * Get a descriptive name for this generator type.
   * Used for logging and reporting.
   * By default, uses the evidence generator's type name.
   */
  def generatorType: String = s"${evidenceGenerator.getEvidenceTypeName} Generator"
  
  /**
   * Get the generator class type (e.g., "standard", "batched", "stitched").
   * Subclasses should override this to provide their specific type.
   */
  def generatorClassType: String = "unknown"
  
  /**
   * Get the answering evaluation strategy for this generator.
   * This determines how answers are evaluated (e.g., exact match, rubric-based, temporal).
   * Delegates to the evidence generator's evaluation strategy.
   */
  final def getAnsweringEvaluation: AnsweringEvaluation = evidenceGenerator.getAnsweringEvaluation()
  
  /**
   * Get the evidence count for this generator.
   * Returns the number of evidence items the underlying evidence generator creates.
   */
  def getEvidenceCount(): Int = evidenceGenerator.getEvidenceCount()
  
  /**
   * Adjust context sizes based on evidence items to ensure:
   * 1. No context size is smaller than the maximum evidence count
   * 2. The maximum evidence count is included as a context size if not already present
   * 
   * @param contextSizes Original list of context sizes
   * @param evidenceItems Evidence items to consider
   * @return Adjusted list of context sizes
   */
  def adjustContextSizesForEvidence(
    contextSizes: List[Int], 
    evidenceItems: List[EvidenceItem]
  ): List[Int] = {
    if (evidenceItems.isEmpty) {
      return contextSizes
    }
    
    // Find the maximum evidence count across all items
    val maxEvidenceCount = evidenceItems.map(_.conversations.length).max
    
    if (maxEvidenceCount == 0) {
      return contextSizes
    }
    
    // Filter out context sizes smaller than the max evidence count
    val validContextSizes = contextSizes.filter(_ >= maxEvidenceCount)
    
    // If max evidence count is not in the list, add it
    val finalContextSizes = if (!validContextSizes.contains(maxEvidenceCount)) {
      (maxEvidenceCount :: validContextSizes).sorted
    } else {
      validContextSizes
    }
    
    // Log the adjustment
    if (finalContextSizes != contextSizes) {
      println(s"Context sizes adjusted for evidence count:")
      println(s"  Original: ${contextSizes.mkString(", ")}")
      println(s"  Max evidence count: $maxEvidenceCount")
      println(s"  Adjusted: ${finalContextSizes.mkString(", ")}")
    }
    
    finalContextSizes
  }
}

/**
 * Factory object for creating test case generators.
 */
object TestCasesGenerator {
  
  /**
   * Create a standard test cases generator for the current evaluation pattern.
   * This generator creates test cases where each evidence item is tested
   * with different amounts of irrelevant context.
   * 
   * @param evidenceGenerator The evidence generator to load evidence items
   * @param contextSizes List of context sizes to test (defaults to Config.Evaluation.CONTEXT_SIZES)
   * @return A test cases generator
   */
  def createStandard(
    evidenceGenerator: EvidenceGenerator,
    contextSizes: List[Int] = com.salesforce.crmmembench.Config.Evaluation.CONTEXT_SIZES
  ): TestCasesGenerator = {
    new StandardTestCasesGenerator(evidenceGenerator, contextSizes)
  }

  
  /**
   * Create a batched test cases generator for efficient evaluation.
   * This generator batches multiple evidence items into single test cases.
   * 
   * @param evidenceGenerator The evidence generator to load evidence items
   * @param contextSizes List of context sizes to test (defaults to Config.Evaluation.CONTEXT_SIZES)
   * @return A batched test cases generator
   */
  def createBatched(
    evidenceGenerator: EvidenceGenerator,
    contextSizes: List[Int] = com.salesforce.crmmembench.Config.Evaluation.CONTEXT_SIZES
  ): TestCasesGenerator = {
    new BatchedTestCasesGenerator(evidenceGenerator, contextSizes)
  }
  
  /**
   * Create a stitching test cases generator that combines standard and batched approaches.
   * Uses standard generation for small contexts and batched for large contexts.
   * 
   * @param evidenceGenerator The evidence generator to load evidence items
   * @param contextSizes List of context sizes to test (defaults to Config.Evaluation.CONTEXT_SIZES)
   * @param threshold Context size threshold (default: 50)
   * @param maxEvidencePerBatch Maximum evidence items per batch for large contexts (default: 100)
   * @return A stitching test cases generator
   */
  def createStitching(
    evidenceGenerator: EvidenceGenerator,
    contextSizes: List[Int] = com.salesforce.crmmembench.Config.Evaluation.CONTEXT_SIZES,
    threshold: Int = 50,
    maxEvidencePerBatch: Int = 100
  ): TestCasesGenerator = {
    StitchingTestCasesGenerator(evidenceGenerator, contextSizes, threshold, maxEvidencePerBatch)
  }
  
  /**
   * Future factory methods can be added here for different generation strategies:
   * - createMultiEvidence(...) for testing multiple evidence items together
   * - createMixedStrategy(...) for complex mixing patterns
   * - createMemoryOptimized(...) for mem0-friendly test cases
   */
}