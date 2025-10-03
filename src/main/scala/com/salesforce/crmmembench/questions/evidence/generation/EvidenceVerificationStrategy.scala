package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.EvidenceItem
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

/**
 * Result of evidence verification.
 * 
 * @param passed Whether the verification passed all checks
 * @param lastModelAnswer The last answer provided by the model during verification
 * @param failureReason Optional reason for failure
 */
case class VerificationResult(
  passed: Boolean,
  lastModelAnswer: String,
  failureReason: Option[String] = None
)

// NOTE: The old verification strategy architecture has been replaced with
// the new composable VerificationCheck system in VerificationSystem.scala
// 
// Old classes removed:
// - EvidenceVerificationStrategy (trait)
// - StandardVerificationStrategy
// - MissingConversationCheckStrategy
// - AlwaysPassVerificationStrategy
//
// These have been replaced by:
// - VerificationCheck (trait)
// - VerifyWithEvidence
// - VerifyWithoutEvidence  
// - VerifyWithPartialEvidence
//
// See VerificationSystem.scala for the new architecture