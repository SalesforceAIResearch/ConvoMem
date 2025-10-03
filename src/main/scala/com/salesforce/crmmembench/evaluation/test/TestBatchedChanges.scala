package com.salesforce.crmmembench.evaluation.test

import com.salesforce.crmmembench.evaluation.BatchedTestCasesGenerator
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Simple test to demonstrate the BatchedTestCasesGenerator changes:
 * - Batch size increased from 20 to 100
 * - Minimum 10 test cases per context size
 */
object TestBatchedChanges {
  def main(args: Array[String]): Unit = {
    println("=== Testing BatchedTestCasesGenerator Changes ===")
    println("This test demonstrates:")
    println("1. Default batch size is now 100 (was 20)")
    println("2. Minimum 10 test cases per context size")
    println()
    
    // Create generator with minimal evidence to test the minimum test cases feature
    val evidenceGenerator = new UserFactsEvidenceGenerator(1)
    
    // Test with a large context size that would previously result in very few test cases
    val contextSizes = List(300)  // Large context that could fit many evidence items in one batch
    
    println(s"Creating BatchedTestCasesGenerator with:")
    println(s"  - Context sizes: $contextSizes")
    println(s"  - Default maxEvidencePerBatch: 100")
    println(s"  - Default minTestCasesPerContext: 10")
    println()
    
    val generator = new BatchedTestCasesGenerator(
      evidenceGenerator,
      contextSizes = contextSizes
    )
    
    // Print configuration via reflection
    val batchSizeField = generator.getClass.getDeclaredField("maxEvidencePerBatch")
    batchSizeField.setAccessible(true)
    val batchSize = batchSizeField.get(generator)
    
    val minTestCasesField = generator.getClass.getDeclaredField("minTestCasesPerContext")
    minTestCasesField.setAccessible(true)
    val minTestCases = minTestCasesField.get(generator)
    
    println(s"Configuration verified:")
    println(s"  - maxEvidencePerBatch: $batchSize")
    println(s"  - minTestCasesPerContext: $minTestCases")
    println()
    
    println("Note: The actual test case generation would load evidence from files.")
    println("In production, if there are fewer test cases than the minimum,")
    println("additional test cases are created by reusing evidence items with")
    println("different irrelevant conversations.")
  }
}