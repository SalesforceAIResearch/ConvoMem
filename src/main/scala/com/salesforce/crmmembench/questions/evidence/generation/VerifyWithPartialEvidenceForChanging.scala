package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.EvidenceItem
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

/**
 * Verification that the model cannot answer correctly without the latest conversation.
 * This is specifically for ChangingEvidenceGenerator to ensure the latest update is necessary.
 * 
 * Unlike the standard VerifyWithPartialEvidence which checks that ALL conversations are necessary,
 * this check specifically verifies that the LATEST conversation (containing the final update)
 * is required to answer the question correctly.
 */
class VerifyWithPartialEvidenceForChanging extends VerificationCheck {
  override def name: String = "partial_evidence_latest"
  
  override def verify(
    evidenceItem: EvidenceItem,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): VerificationCheckResult = {
    
    if (evidenceItem.conversations.isEmpty) {
      trackCheck(false, stats)
      return VerificationCheckResult(
        checkName = name,
        passed = false,
        details = "No conversations to verify",
        modelAnswer = None
      )
    }
    
    // For changing evidence, we specifically test without the latest conversation
    val conversationsWithoutLatest = evidenceItem.conversations.dropRight(1)
    
    if (conversationsWithoutLatest.isEmpty) {
      // If there's only one conversation, we can't test partial evidence
      trackCheck(true, stats)
      return VerificationCheckResult(
        checkName = name,
        passed = true,
        details = "Only one conversation present, skipping partial evidence check",
        modelAnswer = None
      )
    }
    
    // Create a modified evidence item without the latest conversation
    val modifiedItem = evidenceItem.copy(conversations = conversationsWithoutLatest)
    
    // Check if the model can still answer correctly without the latest update
    val result = FilteringVerification.verifyWithEvidence(
      modifiedItem, 
      requiredPasses = 1, 
      stats = None, 
      answeringEvaluation = answeringEvaluation, 
      extensive = false
    )
    
    result match {
      case Some(verificationResult) if verificationResult.passed =>
        // Model answered correctly without the latest update - this is a failure
        trackCheck(false, stats)
        VerificationCheckResult(
          checkName = name,
          passed = false,
          details = s"Model answered correctly without the latest conversation (final update): ${verificationResult.lastModelAnswer}",
          modelAnswer = Some(verificationResult.lastModelAnswer)
        )
      case Some(verificationResult) =>
        // Model couldn't answer correctly without the latest update - this is what we want
        trackCheck(true, stats)
        VerificationCheckResult(
          checkName = name,
          passed = true,
          details = "Latest conversation is necessary - model cannot answer correctly without the final update",
          modelAnswer = Some(verificationResult.lastModelAnswer)
        )
      case None =>
        // Failed to get a response
        trackCheck(false, stats)
        VerificationCheckResult(
          checkName = name,
          passed = false,
          details = "Failed to verify answer without latest conversation",
          modelAnswer = None
        )
    }
  }
}