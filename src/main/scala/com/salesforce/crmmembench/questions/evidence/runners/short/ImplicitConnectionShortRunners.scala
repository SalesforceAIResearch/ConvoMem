package com.salesforce.crmmembench.questions.evidence.runners.short

import com.salesforce.crmmembench.questions.evidence.generators.ImplicitConnectionEvidenceGenerator

/**
 * Short runners for ImplicitConnectionEvidenceGenerator.
 * These runners are designed for quick testing and validation of the generator.
 * They process only 3 personas with 2 use cases each, outputting to short_runs directory.
 * 
 * Only runners for 1 and 2 evidence are provided for common sense checking.
 */

object GenerateImplicitConnection1EvidenceShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running ImplicitConnectionEvidenceGenerator in SHORT mode (1 evidence)")
    println("Output: short_runs/questions/evidence/preference_evidence/1_evidence/")
    println("Processing: 3 personas × 2 use cases = ~6 evidence items")
    println("="*80)
    
    val generator = new ImplicitConnectionEvidenceGenerator(1) {
      override val runShort: Boolean = true
    }
    
    generator.generateEvidence()
  }
}

object GenerateImplicitConnection2EvidenceShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running ImplicitConnectionEvidenceGenerator in SHORT mode (2 evidence)")
    println("Output: short_runs/questions/evidence/preference_evidence/2_evidence/")
    println("Processing: 3 personas × 2 use cases = ~6 evidence items")
    println("="*80)
    
    val generator = new ImplicitConnectionEvidenceGenerator(2) {
      override val runShort: Boolean = true
    }
    
    generator.generateEvidence()
  }
}