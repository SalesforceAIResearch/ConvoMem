package com.salesforce.crmmembench.questions.evidence.runners.short

import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Test append functionality with a minimal short runner.
 * Generates just 1 use case per person for 1 person.
 */
object TestAppendShortRunner {
  def main(args: Array[String]): Unit = {
    println("=== Testing Append with Short Runner ===")
    println("This will generate evidence twice for the same person to test append\n")
    
    // First run
    println("1. FIRST RUN - Generating initial evidence...")
    val generator1 = new UserFactsEvidenceGenerator(1) {
      override val runShort: Boolean = true
      override lazy val useCasesPerPersonConfig: Int = 1 // Just 1 use case
    }
    generator1.generateEvidence()
    
    // Second run - should append
    println("\n2. SECOND RUN - Generating more evidence (should append)...")
    val generator2 = new UserFactsEvidenceGenerator(1) {
      override val runShort: Boolean = true
      override lazy val useCasesPerPersonConfig: Int = 1 // Another use case
    }
    generator2.generateEvidence()
    
    println("\n✅ Test complete! Check the generated files in short_runs/questions/evidence/user_evidence/1_evidence/default/")
    println("The files should contain 2 evidence items per person (1 from each run)")
  }
}