package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator

/**
 * A test cases generator that combines Standard and Batched generators based on context size.
 * 
 * This generator uses:
 * - StandardTestCasesGenerator for context sizes below the threshold (better for small contexts)
 * - BatchedTestCasesGenerator for context sizes at or above the threshold (better for large contexts)
 * 
 * This provides optimal test case generation for both small and large context evaluations.
 * 
 * @param evidenceGenerator The evidence generator to use
 * @param contextSizes List of context sizes to test
 * @param threshold Context size threshold - sizes below this use Standard, at/above use Batched
 * @param maxEvidencePerBatch Maximum evidence items per batch for the batched generator
 */
class StitchingTestCasesGenerator(
  evidenceGenerator: EvidenceGenerator,
  contextSizes: List[Int],
  threshold: Int,
  maxEvidencePerBatch: Int = 20
) extends TestCasesGenerator(evidenceGenerator) {

  // Note: Context size adjustment is handled by the Standard and Batched generators themselves
  // when they load evidence items, so we don't need to adjust here
  
  // Split context sizes based on threshold
  val (smallContextSizes, largeContextSizes) = contextSizes.partition(_ < threshold)
  
  // Create generators for each size range
  val standardGenerator = if (smallContextSizes.nonEmpty) {
    Some(new StandardTestCasesGenerator(evidenceGenerator, smallContextSizes))
  } else {
    None
  }
  
  val batchedGenerator = if (largeContextSizes.nonEmpty) {
    Some(new BatchedTestCasesGenerator(evidenceGenerator, largeContextSizes, maxEvidencePerBatch))
  } else {
    None
  }
  
  override def generatorType: String = 
    s"Stitching ${evidenceGenerator.getEvidenceTypeName} Generator (threshold=$threshold)"
  
  override def generatorClassType: String = "stitched"
  
  override def generateTestCases(): List[TestCase] = {
    println(s"\nGenerating test cases with StitchingTestCasesGenerator")
    println(s"Threshold: $threshold")
    println(s"Context sizes < $threshold (using Standard): ${smallContextSizes.mkString(", ")}")
    println(s"Context sizes >= $threshold (using Batched): ${largeContextSizes.mkString(", ")}")
    
    // Generate test cases from both generators
    val standardTestCases = standardGenerator match {
      case Some(gen) =>
        println(s"\nGenerating standard test cases for small contexts...")
        val cases = gen.generateTestCases()
        println(s"Generated ${cases.length} standard test cases")
        cases
      case None =>
        println(s"No small context sizes, skipping standard generation")
        List.empty
    }
    
    val batchedTestCases = batchedGenerator match {
      case Some(gen) =>
        println(s"\nGenerating batched test cases for large contexts...")
        val cases = gen.generateTestCases()
        println(s"Generated ${cases.length} batched test cases")
        cases
      case None =>
        println(s"No large context sizes, skipping batched generation")
        List.empty
    }
    
    // Combine all test cases
    val allTestCases = standardTestCases ++ batchedTestCases
    
    println(s"\nTotal test cases generated: ${allTestCases.length}")
    println(s"  Standard: ${standardTestCases.length}")
    println(s"  Batched: ${batchedTestCases.length}")
    
    allTestCases
  }
}

/**
 * Factory methods for StitchingTestCasesGenerator
 */
object StitchingTestCasesGenerator {
  
  /**
   * Create a stitching test cases generator with default threshold of 50.
   * This threshold is chosen because:
   * - Below 50: Individual evidence testing is efficient
   * - 50 and above: Batching provides better memory system performance
   */
  def apply(
    evidenceGenerator: EvidenceGenerator,
    contextSizes: List[Int] = com.salesforce.crmmembench.Config.Evaluation.CONTEXT_SIZES,
    threshold: Int = 50,
    maxEvidencePerBatch: Int = 20
  ): StitchingTestCasesGenerator = {
    new StitchingTestCasesGenerator(evidenceGenerator, contextSizes, threshold, maxEvidencePerBatch)
  }
}