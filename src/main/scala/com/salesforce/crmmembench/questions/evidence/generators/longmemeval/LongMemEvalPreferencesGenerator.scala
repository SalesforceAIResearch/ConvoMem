package com.salesforce.crmmembench.questions.evidence.generators.longmemeval

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Generator for LongMemEval preferences evidence.
 * These test the model's ability to remember and apply user preferences.
 * 
 * @param evidenceCount The number of evidence items (only 1 available)
 */
class LongMemEvalPreferencesGenerator(evidenceCount: Int = 1) extends LongMemEvalReadOnlyGenerator {
  
  override def config: EvidenceConfig = LongMemEvalPreferencesConfig(evidenceCount)
  
  override def getEvidenceTypeName: String = "LongMemEval Preferences"
  
  override def getVerificationChecks(): List[VerificationCheck] = {
    // Preferences use rubric-based evaluation, so standard checks apply
    VerificationConstants.STANDARD_CHECKS
  }
  
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    // Use rubric-based evaluation for preferences
    RubricBasedAnsweringEvaluation
  }
}