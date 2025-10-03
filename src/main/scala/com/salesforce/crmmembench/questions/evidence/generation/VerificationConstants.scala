package com.salesforce.crmmembench.questions.evidence.generation

/**
 * Constants for verification system.
 * Provides pre-instantiated verification checks that can be reused.
 */
object VerificationConstants {
  
  /**
   * Standard verification checks used by most evidence generators.
   * - VerifyWithEvidence: Check that model answers correctly with evidence
   * - VerifyWithoutEvidence: Check that model cannot answer without evidence
   */
  val STANDARD_CHECKS: List[VerificationCheck] = List(
    new VerifyWithEvidence(),
    new VerifyWithoutEvidence()
  )
  
  /**
   * Extended verification checks for multi-evidence scenarios.
   * Includes all standard checks plus:
   * - VerifyWithPartialEvidence: Check that all conversations are necessary
   */
  val MULTI_EVIDENCE_CHECKS: List[VerificationCheck] = STANDARD_CHECKS :+ new VerifyWithPartialEvidence()
  
  /**
   * Empty verification checks for generators that don't need verification.
   */
  val NO_VERIFICATION: List[VerificationCheck] = List.empty
}