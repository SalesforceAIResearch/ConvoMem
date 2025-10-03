package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Test runner to verify model statistics tracking
 */
object TestModelStats {
  def main(args: Array[String]): Unit = {
    println("Testing model statistics tracking in short mode...")
    println("=" * 80)
    
    // Create generator with runShort = true
    val generator = new UserFactsEvidenceGenerator(1) {
      override val runShort: Boolean = true
    }
    
    // Run generation
    generator.generateEvidence()
    
    println("\n" + "=" * 80)
    println("Test completed. Check the statistics above for model breakdowns.")
  }
}