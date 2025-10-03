package com.salesforce.crmmembench.questions.evidence.generators.longmemeval

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Generator for LongMemEval knowledge update evidence.
 * These test the model's ability to handle changing information over time.
 * 
 * @param evidenceCount The number of evidence items (only 2 available)
 */
class LongMemEvalKnowledgeUpdateGenerator(evidenceCount: Int = 2) extends LongMemEvalReadOnlyGenerator {
  
  override def config: EvidenceConfig = LongMemEvalKnowledgeUpdateConfig(evidenceCount)
  
  override def getEvidenceTypeName: String = "LongMemEval Knowledge Updates"
  
  override def getVerificationChecks(): List[VerificationCheck] = {
    // Knowledge updates are similar to changing evidence
    VerificationConstants.STANDARD_CHECKS
  }
  
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    // Use default factual evaluation
    DefaultAnsweringEvaluation
  }
  
  override def printAdditionalStatistics(items: List[EvidenceItem]): Unit = {
    // Show how many have multiple conversations (indicating updates)
    val multiConv = items.filter(_.conversations.length > 1)
    println(s"📊 Items with updates: ${multiConv.length}/${items.length}")
  }
}