package com.salesforce.crmmembench.questions.evidence

/**
 * Categories of validation failures for evidence cores
 */
object CoreValidationFailureCategory {
  val EVIDENCE_COUNT_MISMATCH = "evidence_count_mismatch"
}

/**
 * Result of core validation
 */
case class CoreValidationResult(
  isValid: Boolean,
  errors: List[String],
  failureCategories: Set[String] = Set.empty
)

/**
 * Validates that generated evidence cores meet expected requirements.
 * Similar structure to ConversationValidator but for evidence cores.
 */
object CoreValidator {
  
  /**
   * Validates that the evidence core has the expected number of evidence messages
   * based on the generator's configuration.
   * 
   * @param evidenceCore The evidence core to validate
   * @param expectedEvidenceCount The expected number of evidence messages from the generator
   * @return CoreValidationResult with isValid flag and any error messages
   */
  def validateCore(
    evidenceCore: GeneratedEvidenceCore,
    expectedEvidenceCount: Int
  ): CoreValidationResult = {
    val errors = scala.collection.mutable.ListBuffer[String]()
    val failureCategories = scala.collection.mutable.Set[String]()
    
    // Check: Number of evidence messages must match expected count
    val actualCount = evidenceCore.message_evidences.length
    if (actualCount != expectedEvidenceCount) {
      errors += s"Number of evidence messages ($actualCount) doesn't match expected count ($expectedEvidenceCount)"
      failureCategories += CoreValidationFailureCategory.EVIDENCE_COUNT_MISMATCH
    }
    
    CoreValidationResult(errors.isEmpty, errors.toList, failureCategories.toSet)
  }
}