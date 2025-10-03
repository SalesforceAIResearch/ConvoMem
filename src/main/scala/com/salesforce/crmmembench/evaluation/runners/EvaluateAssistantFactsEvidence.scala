package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.{BlockBasedEvaluator, Evaluator, Mem0Evaluator, TestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators.AssistantFactsEvidenceGenerator
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}

/**
 * Evaluation entry points for assistant facts evidence in large context.
 * These evaluations test how well models recall their own previous statements/recommendations
 * as the context becomes increasingly diluted with irrelevant conversations.
 * 
 * Assistant facts evidence tests scenarios where users ask to recall what the assistant previously said:
 * - Assistant recommends/states something in earlier conversation
 * - Later, user asks to recall what the assistant said
 * - Model must provide exact recall of its previous statement
 * 
 * Example pattern:
 * - Assistant: "I would recommend Roscioli restaurant for dinner"
 * - User (later): "What restaurant did you recommend?"
 * - Expected answer: "Roscioli"
 */

/**
 * Evaluate assistant facts evidence with 1 statement in large context.
 */
object Evaluate1AssistantFactsEvidenceLargeContext extends Evaluator {
  override def model: LLMModel = Gemini.flash
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AssistantFactsEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate assistant facts evidence with 2 statements in large context.
 */
object Evaluate2AssistantFactsEvidenceLargeContext extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AssistantFactsEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate assistant facts evidence with 3 statements in large context.
 */
object Evaluate3AssistantFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AssistantFactsEvidenceGenerator(3))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate assistant facts evidence with 4 statements in large context.
 */
object Evaluate4AssistantFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AssistantFactsEvidenceGenerator(4))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate assistant facts evidence with 5 statements in large context.
 */
object Evaluate5AssistantFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AssistantFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate assistant facts evidence with 6 statements in large context.
 */
object Evaluate6AssistantFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new AssistantFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate assistant facts evidence with 1 statement using mem0.
 */
object Evaluate1AssistantFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(1)
}

/**
 * Block-based evaluators for assistant facts evidence.
 * These use the block-based memory system that processes conversations in chunks.
 */
object Evaluate1AssistantFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(1)
}

object Evaluate2AssistantFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(2)
}

object Evaluate3AssistantFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(3)
}

object Evaluate4AssistantFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(4)
}

object Evaluate5AssistantFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(5)
}

object Evaluate6AssistantFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(6)
}

