package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.EvidenceItem

/**
 * Result of a context-based evaluation test.
 * This case class is used throughout the evaluation framework to track results.
 */
case class ContextTestResult(
  evidenceItem: EvidenceItem,
  contextType: String,
  contextSize: Int,
  modelAnswer: String,
  isCorrect: Boolean,
  retrievedRelevantConversations: Int = 0
)