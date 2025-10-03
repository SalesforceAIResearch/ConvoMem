package com.salesforce.crmmembench.questions.evidence.runners.short

import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Short runners for UserFactsEvidenceGenerator.
 * These runners are designed for quick testing and validation of the generator.
 * They process only 3 personas with 20 use cases each for diversity validation.
 * Per README guidelines: only evidence counts 1 and 2 for common sense checking.
 */

object GenerateUserFacts1EvidenceShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running UserFactsEvidenceGenerator in SHORT mode (1 evidence)")
    println("Output: short_runs/questions/evidence/user_evidence/1_evidence/")
    println("Processing: 3 personas × 20 use cases = ~60 evidence items")
    println("IMPORTANT: Review all 20 use cases per person for diversity!")
    println("="*80)
    
    val generator = new UserFactsEvidenceGenerator(1) {
      override val runShort: Boolean = true
    }
    
    generator.generateEvidence()
  }
}

object GenerateUserFacts2EvidenceShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running UserFactsEvidenceGenerator in SHORT mode (2 evidence)")
    println("Output: short_runs/questions/evidence/user_evidence/2_evidence/")
    println("Processing: 3 personas × 20 use cases = ~60 evidence items")
    println("IMPORTANT: Review all 20 use cases per person for diversity!")
    println("="*80)
    
    val generator = new UserFactsEvidenceGenerator(2) {
      override val runShort: Boolean = true
    }
    
    generator.generateEvidence()
  }
}