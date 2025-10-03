package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.evaluation.EvaluationLogger.EvaluationLogEntry
import com.salesforce.crmmembench.questions.evidence.Conversation

/**
 * Memory answerer that returns cached model answers from evaluation logs.
 * Used by ReJudge to re-evaluate existing model responses without re-running inference.
 * 
 * @param testCaseAnswerMap Map from test case ID to (model answer, answer result)
 * @param originalMemoryType The name of the original memory system (to preserve in stats)
 */
class CachedLogMemoryAnswerer(
  testCaseAnswerMap: Map[String, (String, Option[AnswerResult])],
  originalMemoryType: String
) extends MemoryAnswerer {
  
  override def addConversation(conversation: Conversation): Unit = {
    // No-op: We don't need to load conversations since we have cached answers
  }
  
  override def addConversations(conversations: List[Conversation]): Unit = {
    // No-op: We don't need to load conversations since we have cached answers
  }
  
  override def answerQuestion(question: String, testCaseId: String): AnswerResult = {
    // Look up answer by test case ID
    testCaseAnswerMap.get(testCaseId) match {
      case Some((modelAnswer, originalAnswerResult)) =>
        // Return the original answer result if available, otherwise create a minimal one
        originalAnswerResult.getOrElse(
          AnswerResult(
            answer = Some(modelAnswer),
            retrievedConversationIds = List.empty
          )
        )
      case None =>
        // Critical error - test case ID not found in cache
        System.err.println(s"\n❌ FATAL ERROR: Test case ID not found in cached answers!")
        System.err.println(s"Test case ID: '$testCaseId'")
        System.err.println(s"Question being asked: '$question'")
        System.err.println(s"Total cached test cases: ${testCaseAnswerMap.size}")
        System.err.println(s"First 5 cached test case IDs:")
        testCaseAnswerMap.keys.take(5).foreach { id =>
          System.err.println(s"  - '$id'")
        }
        System.err.println("\nThis indicates a mismatch between log entries and the test cases being evaluated.")
        System.err.println("Terminating to prevent incorrect evaluation results.")
        System.exit(1)
        throw new IllegalStateException(s"No cached answer found for test case: $testCaseId")
    }
  }
  
  override def getMemoryType: String = originalMemoryType
  
  override def clearMemory(): Unit = {
    // No-op: Cached answers are immutable
  }
}