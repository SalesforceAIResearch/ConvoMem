package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.EvidenceItem
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

/**
 * Result of a single verification check.
 */
case class VerificationCheckResult(
  checkName: String,
  passed: Boolean,
  details: String,
  modelAnswer: Option[String] = None
)

/**
 * Base trait for individual verification checks.
 * Each implementation represents a specific type of verification.
 */
trait VerificationCheck {
  /**
   * The name of this verification check for tracking purposes.
   */
  def name: String
  
  /**
   * Execute this verification check.
   * 
   * @param evidenceItem The evidence item to verify
   * @param stats Optional stats tracking
   * @param answeringEvaluation The evaluation strategy to use
   * @return The result of this specific check
   */
  def verify(
    evidenceItem: EvidenceItem,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): VerificationCheckResult
  
  /**
   * Helper to track check stats.
   */
  def trackCheck(
    passed: Boolean,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]]
  ): Unit = {
    stats.foreach { s =>
      val (attempts, passes) = s.getOrElseUpdate(name, 
        (new AtomicInteger(0), new AtomicInteger(0)))
      attempts.incrementAndGet()
      if (passed) passes.incrementAndGet()
    }
  }
}

/**
 * Verification that the model answers correctly when given the evidence.
 * This is the core verification - can the model use the evidence to answer?
 */
class VerifyWithEvidence(requiredPasses: Int = 2) extends VerificationCheck {
  override def name: String = "with_evidence"
  
  override def verify(
    evidenceItem: EvidenceItem,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): VerificationCheckResult = {
    // Track that we're attempting this check
    stats.foreach { statsMap =>
      val (attempts, _) = statsMap.getOrElseUpdate(name, (new AtomicInteger(0), new AtomicInteger(0)))
      attempts.incrementAndGet()
    }
    
    FilteringVerification.verifyWithEvidence(evidenceItem, requiredPasses = requiredPasses, stats = stats, answeringEvaluation = answeringEvaluation) match {
      case Some(result) =>
        // Update passes count if successful
        if (result.passed) {
          stats.foreach { statsMap =>
            val (_, passes) = statsMap(name)
            passes.incrementAndGet()
          }
        }
        VerificationCheckResult(
          checkName = name,
          passed = result.passed,
          details = result.failureReason.getOrElse(s"Model answered correctly $requiredPasses times in a row"),
          modelAnswer = Some(result.lastModelAnswer)
        )
      case None =>
        // Attempt already tracked, just return failure
        VerificationCheckResult(
          checkName = name,
          passed = false,
          details = "Failed to get response from model",
          modelAnswer = None
        )
    }
  }
}

/**
 * Verification that the model cannot answer correctly without the evidence.
 * This ensures the evidence is actually needed.
 */
class VerifyWithoutEvidence extends VerificationCheck {
  override def name: String = "without_evidence"
  
  override def verify(
    evidenceItem: EvidenceItem,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): VerificationCheckResult = {
    FilteringVerification.verifyWithoutEvidence(evidenceItem, answeringEvaluation) match {
      case Some(result) =>
        // For this check, we want the model to fail (not answer correctly)
        val passed = result.passed // passed here means "answered incorrectly" which is what we want
        trackCheck(passed, stats)
        
        if (passed) {
          VerificationCheckResult(
            checkName = name,
            passed = true,
            details = "Model correctly failed to answer without evidence",
            modelAnswer = Some(result.lastModelAnswer)
          )
        } else {
          VerificationCheckResult(
            checkName = name,
            passed = false,
            details = s"Model answered correctly without evidence: ${result.lastModelAnswer}",
            modelAnswer = Some(result.lastModelAnswer)
          )
        }
      case None =>
        trackCheck(false, stats)
        VerificationCheckResult(
          checkName = name,
          passed = false,
          details = "Failed to verify answer without evidence",
          modelAnswer = None
        )
    }
  }
}

/**
 * Verification that removing any single conversation makes the answer incorrect.
 * This ensures all conversations are necessary (no redundancy).
 */
class VerifyWithPartialEvidence extends VerificationCheck {
  override def name: String = "partial_evidence"
  
  override def verify(
    evidenceItem: EvidenceItem,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): VerificationCheckResult = {
    // Test with each conversation removed, stopping early if one still works
    var anyStillCorrect = false
    
    for (i <- evidenceItem.conversations.indices if !anyStillCorrect) {
      val conversationsWithoutOne = 
        evidenceItem.conversations.take(i) ++ evidenceItem.conversations.drop(i + 1)
      
      if (conversationsWithoutOne.nonEmpty) {
        val modifiedItem = evidenceItem.copy(conversations = conversationsWithoutOne)
        val result = FilteringVerification.verifyWithEvidence(
          modifiedItem, 
          requiredPasses = 1, 
          stats = None, 
          answeringEvaluation = answeringEvaluation, 
          extensive = false
        )
        
        result match {
          case Some(verificationResult) if verificationResult.passed =>
            // Found a configuration that still works - we can stop
            anyStillCorrect = true
          case _ =>
            // This configuration doesn't work, continue checking
        }
      }
    }
    
    val passed = !anyStillCorrect // We pass if NONE of the partial configs work
    
    // Track the result atomically
    trackCheck(passed, stats)
    
    if (passed) {
      VerificationCheckResult(
        checkName = name,
        passed = true,
        details = "All conversations are necessary - removing any one prevents correct answer",
        modelAnswer = None
      )
    } else {
      VerificationCheckResult(
        checkName = name,
        passed = false,
        details = "Evidence is too redundant - model can still answer correctly with missing conversations",
        modelAnswer = None
      )
    }
  }
}

/**
 * Composite verification result combining multiple checks.
 */
case class CompositeVerificationResult(
  passed: Boolean,
  checks: List[VerificationCheckResult],
  lastModelAnswer: String,
  failureReason: Option[String] = None
) {
  def toVerificationResult: VerificationResult = {
    VerificationResult(
      passed = passed,
      lastModelAnswer = lastModelAnswer,
      failureReason = failureReason
    )
  }
}

/**
 * Executes a list of verification checks and combines the results.
 */
object VerificationExecutor {
  
  /**
   * Execute all verification checks for an evidence item.
   * 
   * @param evidenceItem The evidence to verify
   * @param checks The list of checks to perform
   * @param stats Optional stats tracking
   * @param answeringEvaluation The evaluation strategy to use
   * @return Combined verification result
   */
  def execute(
    evidenceItem: EvidenceItem,
    checks: List[VerificationCheck],
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]] = None,
    answeringEvaluation: AnsweringEvaluation
  ): CompositeVerificationResult = {
    
    // If no checks, always pass
    if (checks.isEmpty) {
      return CompositeVerificationResult(
        passed = true,
        checks = Nil,
        lastModelAnswer = "No verification required",
        failureReason = None
      )
    }
    
    // Execute checks sequentially, stopping at first failure
    val results = scala.collection.mutable.ListBuffer[VerificationCheckResult]()
    var lastModelAnswer = ""
    
    for (check <- checks) {
      val result = check.verify(evidenceItem, stats, answeringEvaluation)
      results += result
      
      if (result.modelAnswer.isDefined) {
        lastModelAnswer = result.modelAnswer.get
      }
      
      // Stop at first failure
      if (!result.passed) {
        return CompositeVerificationResult(
          passed = false,
          checks = results.toList,
          lastModelAnswer = lastModelAnswer,
          failureReason = Some(s"${result.checkName}: ${result.details}")
        )
      }
    }
    
    // All checks passed
    CompositeVerificationResult(
      passed = true,
      checks = results.toList,
      lastModelAnswer = lastModelAnswer,
      failureReason = None
    )
  }
}