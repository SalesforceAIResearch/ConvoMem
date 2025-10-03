package com.salesforce.crmmembench.questions.evidence.runners.short

import com.salesforce.crmmembench.questions.evidence.generators.PreferenceEvidenceGenerator

/**
 * Short runner for PreferenceEvidenceGenerator.
 * This runner is designed for quick testing and validation of the generator.
 * It processes only 3 personas with 20 use cases each for diversity validation.
 */
object GeneratePreference1EvidenceShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running PreferenceEvidenceGenerator in SHORT mode (1 preference)")
    println("Output: short_runs/questions/evidence/preference_evidence/1_evidence/")
    println("Processing: 3 personas × 20 use cases = ~60 evidence items")
    println("IMPORTANT: Checking that questions are natural and don't reference memory!")
    println("="*80)
    
    val generator = new PreferenceEvidenceGenerator(1) {
      override val runShort: Boolean = true
    }
    
    generator.generateEvidence()
  }
}