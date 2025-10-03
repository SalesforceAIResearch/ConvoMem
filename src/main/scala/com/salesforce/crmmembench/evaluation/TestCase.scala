package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Conversation}

/**
 * Represents a test case for evaluation.
 * 
 * A test case contains evidence items to be tested and conversations that provide context.
 * In the current implementation, conversations come from either:
 * - Evidence items themselves (containing evidence)
 * - Irrelevant conversations (not containing evidence)
 * 
 * In future implementations, this structure will support:
 * - Multiple evidence items being tested together
 * - Conversations from evidence items being distributed among other conversations
 * - Testing multiple questions within a single test case
 * 
 * @param evidenceItems List of evidence items to be tested in this case
 * @param conversations All conversations to be loaded for this test case
 * @param contextSize Optional context size to avoid recalculating from conversations list
 */
case class TestCase(
  evidenceItems: List[EvidenceItem],
  conversations: List[Conversation],
  contextSize: Option[Int] = None
) {

  /**
   * Get conversations that contain evidence.
   * Uses the containsEvidence field to filter.
   */
  def getEvidenceConversations: List[Conversation] = 
    conversations.filter(_.containsEvidence.getOrElse(false))

  /**
   * Get the total number of conversations in this test case.
   * Uses the provided contextSize if available, otherwise calculates from conversations list.
   */
  def conversationCount: Int = contextSize.getOrElse(conversations.size)
  
  /**
   * Get a unique identifier for this test case.
   * Combines evidence item hash codes with context size to ensure uniqueness
   * across different context sizes for the same evidence items.
   */
  lazy val id: String = {
    val evidenceId = evidenceItems.map(_.hashCode).mkString("_")
    val contextId = conversationCount
    s"${evidenceId}_ctx${contextId}"
  }
}