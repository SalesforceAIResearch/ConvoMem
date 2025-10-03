package com.salesforce.crmmembench.questions.evidence.generators.longmemeval

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Generator for LongMemEval multi-session evidence (includes single-session-user).
 * This combines questions that require tracking information across multiple sessions.
 * 
 * @param evidenceCount The number of evidence items (1-5 available)
 */
class LongMemEvalMultiSessionGenerator(evidenceCount: Int = 2) extends LongMemEvalReadOnlyGenerator {
  
  override def config: EvidenceConfig = LongMemEvalMultiSessionConfig(evidenceCount)
  
  override def getEvidenceTypeName: String = "LongMemEval Multi-Session"
  
  override def getVerificationChecks(): List[VerificationCheck] = {
    // Use standard checks for multi-session data
    VerificationConstants.MULTI_EVIDENCE_CHECKS
  }
  
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    // Use default factual evaluation
    DefaultAnsweringEvaluation
  }
  
  override def printAdditionalStatistics(items: List[EvidenceItem]): Unit = {
    // Group by conversation count
    val byConvCount = items.groupBy(_.conversations.length)
    println(s"📊 By conversation count:")
    byConvCount.toSeq.sortBy(_._1).foreach { case (count, items) =>
      println(s"   - $count conversations: ${items.length} items")
    }
  }
}