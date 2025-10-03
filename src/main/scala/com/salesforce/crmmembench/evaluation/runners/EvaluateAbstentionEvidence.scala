package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.{Evaluator, TestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators.AbstentionEvidenceGenerator
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}

/**
 * Evaluation entry points for abstention evidence in large context.
 * These evaluations test how well models recognize when they lack sufficient
 * information to answer questions as the context becomes diluted with irrelevant conversations.
 */

/**
 * Evaluate 1-message abstention evidence in large context.
 * Pattern: One piece of related but insufficient information
 * Example: "John works in marketing" → Question: "What's John's phone number?" → "I don't know"
 */
object Evaluate1AbstentionEvidenceLargeContext extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AbstentionEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate 2-message abstention evidence in large context.
 * Pattern: Two pieces of related but insufficient information
 * Example: "John works in marketing", "John joined last month" → Question: "What's John's phone number?" → "I don't know"
 */
object Evaluate2AbstentionEvidenceLargeContext extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AbstentionEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate 3-message abstention evidence in large context.
 * Pattern: Three pieces of related but insufficient information
 */
object Evaluate3AbstentionEvidenceLargeContext extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AbstentionEvidenceGenerator(3))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate 4-message abstention evidence in large context.
 * Pattern: Four pieces of related but insufficient information
 */
object Evaluate4AbstentionEvidenceLargeContext extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AbstentionEvidenceGenerator(4))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

