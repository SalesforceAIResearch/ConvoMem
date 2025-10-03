package com.salesforce.crmmembench.questions.evidence.generators.longmemeval

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Generator for LongMemEval abstention evidence.
 * These test the model's ability to say "I don't know" when information is not available.
 * 
 * @param evidenceCount The number of evidence items (1-4 available)
 */
class LongMemEvalAbstentionGenerator(evidenceCount: Int = 2) extends LongMemEvalReadOnlyGenerator {
  
  override def config: EvidenceConfig = LongMemEvalAbstentionConfig(evidenceCount)
  
  override def getEvidenceTypeName: String = "LongMemEval Abstention"
  
  override def getVerificationChecks(): List[VerificationCheck] = {
    // For abstention, we might want different checks
    // The model should NOT be able to answer with evidence
    VerificationConstants.NO_VERIFICATION  // Or create custom checks
  }
  
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    // Abstention uses special evaluation
    AbstentionAnsweringEvaluation
  }
}