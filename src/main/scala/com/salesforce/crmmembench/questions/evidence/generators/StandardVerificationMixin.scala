package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Mixin trait providing standard verification checks for evidence generators.
 * 
 * This trait implements the common pattern where:
 * - Single evidence items use standard checks (with evidence, without evidence)
 * - Multiple evidence items add an additional check for partial evidence
 * 
 * Mix this into evidence generators that follow this verification pattern.
 */
trait StandardVerificationMixin {
  this: EvidenceGenerator =>
  /**
   * The number of evidence items this generator creates.
   * Must be implemented by the mixing class.
   */
  def evidenceCount: Int
  
  /**
   * Provides standard verification checks based on evidence count.
   * 
   * For single evidence (evidenceCount == 1):
   * - VerifyWithEvidence: Check that model answers correctly with evidence
   * - VerifyWithoutEvidence: Check that model cannot answer without evidence
   * 
   * For multiple evidence (evidenceCount > 1):
   * - All of the above, plus:
   * - VerifyWithPartialEvidence: Check that all conversations are necessary
   * 
   * @return List of verification checks to perform
   */
  def getVerificationChecks(): List[VerificationCheck] = {
    if (evidenceCount > 1) {
      // For multiple facts, ensure all conversations are necessary
      VerificationConstants.MULTI_EVIDENCE_CHECKS
    } else {
      // For single fact, use standard checks
      VerificationConstants.STANDARD_CHECKS
    }
  }
}