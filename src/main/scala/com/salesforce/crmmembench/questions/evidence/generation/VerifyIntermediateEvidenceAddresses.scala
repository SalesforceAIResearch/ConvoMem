package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.EvidenceItem
import com.salesforce.crmmembench.evaluation.EvaluationUtils
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

/**
 * Verification that each evidence message (except the last one) actually tries to address the question.
 * This prevents a scenario where all evidence messages except the last one are gibberish.
 * 
 * This check is specifically designed for ChangingEvidenceGenerator to ensure that each
 * intermediate update is relevant to the question, not just the final state.
 */
class VerifyIntermediateEvidenceAddresses extends VerificationCheck {
  override def name: String = "intermediate_evidence_addresses_question"
  
  override def verify(
    evidenceItem: EvidenceItem,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): VerificationCheckResult = {
    
    if (evidenceItem.message_evidences.size <= 1) {
      // Not enough evidence messages to check intermediate evidence
      trackCheck(true, stats)
      return VerificationCheckResult(
        checkName = name,
        passed = true,
        details = "Not enough evidence messages to check intermediate evidence",
        modelAnswer = None
      )
    }
    
    // Get all evidence messages except the last one
    val intermediateEvidenceMessages = evidenceItem.message_evidences.dropRight(1)
    
    if (intermediateEvidenceMessages.isEmpty) {
      trackCheck(false, stats)
      return VerificationCheckResult(
        checkName = name,
        passed = false,
        details = "No intermediate evidence messages found",
        modelAnswer = None
      )
    }
    
    // Check each message to see if it addresses the question
    var anyMessageFailed = false
    var failedMessageDetails: Option[String] = None
    
    for ((message, idx) <- intermediateEvidenceMessages.zipWithIndex if !anyMessageFailed) {
      val judgePrompt = s"""I will provide you with a question and a message. Your task is to determine if the message DIRECTLY addresses the specific topic that the question is asking about.

STRICT CRITERIA:
1. The message must be about the EXACT same subject as the question (not just vaguely related)
2. The message must provide specific information that would be needed to answer the question
3. General statements or tangentially related topics should be marked as WRONG
4. The message should contain concrete details about what the question asks

Examples:
- Question: "What time is the meeting?" 
  Message: "Let's schedule the meeting for 2 PM" → RIGHT (directly states meeting time)
  Message: "We need to have a meeting soon" → WRONG (mentions meeting but no time info)
  Message: "I'm busy at 2 PM" → WRONG (mentions time but not about the meeting)
  Message: "The project is going well" → WRONG (completely unrelated)

- Question: "What are my travel plans?"
  Message: "I'm flying to Paris on March 15th" → RIGHT (specific travel information)
  Message: "I love traveling" → WRONG (general statement, no specific plans)
  Message: "Paris is beautiful this time of year" → WRONG (about Paris but not about travel plans)
  Message: "My trip got cancelled" → RIGHT (directly addresses travel plans status)

- Question: "What's the project deadline?"
  Message: "The project is due on Friday, April 10th" → RIGHT (directly states deadline)
  Message: "We're working hard on the project" → WRONG (about project but not deadline)
  Message: "Friday is going to be busy" → WRONG (mentions Friday but not as deadline)
  Message: "The deadline was moved to next Monday" → RIGHT (directly about deadline change)

- Question: "Where is the conference room?"
  Message: "The meeting is in Conference Room B on the 3rd floor" → RIGHT (directly states location)
  Message: "We have a nice conference room" → WRONG (mentions conference room but no location)
  Message: "I'll be on the 3rd floor" → WRONG (mentions floor but not about conference room)

Be STRICT: If the message doesn't contain specific information that directly helps answer the question, mark it as WRONG.

**Answer only "RIGHT" or "WRONG". Do not provide any additional text, explanations, or reasoning.**

Question: ${evidenceItem.question}
Message: ${message.text}

Answer (RIGHT/WRONG):"""
      
      EvaluationUtils.verifyAnswerCorrectnessWithPrompt(judgePrompt) match {
        case Some(true) =>
          // Message addresses the question - continue to next
          ()
        case Some(false) =>
          // Message doesn't address the question - fail the check
          anyMessageFailed = true
          failedMessageDetails = Some(s"Message ${idx + 1} doesn't address the question: '${message.text.take(100)}...'")
        case None =>
          // Failed to verify - consider as failure
          anyMessageFailed = true
          failedMessageDetails = Some(s"Failed to verify if message ${idx + 1} addresses the question")
      }
    }
    
    val passed = !anyMessageFailed
    trackCheck(passed, stats)
    
    if (passed) {
      VerificationCheckResult(
        checkName = name,
        passed = true,
        details = s"All ${intermediateEvidenceMessages.size} intermediate evidence messages address the question",
        modelAnswer = None
      )
    } else {
      VerificationCheckResult(
        checkName = name,
        passed = false,
        details = failedMessageDetails.getOrElse("Some intermediate evidence messages don't address the question"),
        modelAnswer = None
      )
    }
  }
}