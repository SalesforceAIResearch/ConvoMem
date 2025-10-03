package com.salesforce.crmmembench.questions.evidence.generators.longmemeval

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Generator for LongMemEval assistant facts evidence.
 * These test the model's ability to recall facts stated by the assistant.
 * 
 * @param evidenceCount The number of evidence items (only 1 available)
 */
class LongMemEvalAssistantFactsGenerator(evidenceCount: Int = 1) extends LongMemEvalReadOnlyGenerator {
  
  override def config: EvidenceConfig = LongMemEvalAssistantFactsConfig(evidenceCount)
  
  override def getEvidenceTypeName: String = "LongMemEval Assistant Facts"
  
  override def getVerificationChecks(): List[VerificationCheck] = {
    // Use standard checks for assistant facts
    VerificationConstants.STANDARD_CHECKS
  }
  
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    // Use default factual evaluation
    DefaultAnsweringEvaluation
  }
}