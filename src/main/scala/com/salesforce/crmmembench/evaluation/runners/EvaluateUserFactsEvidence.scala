package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.LLM_endpoints.{Claude, Gemini, LLMModel, OpenAI}
import com.salesforce.crmmembench.evaluation.memory.{MemoryAnswererFactory, LongContextMemoryFactory, BlockBasedMemoryFactory}
import com.salesforce.crmmembench.evaluation.{BatchedTestCasesGenerator, BlockBasedEvaluator, CachedEvaluator, Evaluator, Mem0Evaluator, TestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Evaluation entry points for user facts evidence in large context.
 * These evaluations test how well models recall user-stated facts/information
 * as the context becomes increasingly diluted with irrelevant conversations.
 *
 * User facts evidence tests scenarios where users ask to recall what they previously stated:
 * - User states facts/information in earlier conversation
 * - Later, user asks to recall what they said
 * - Model must provide exact recall of the user's previous statements
 *
 * Example pattern:
 * - User: "My favorite restaurant is Chez Laurent"
 * - User (later): "What's my favorite restaurant?"
 * - Expected answer: "Chez Laurent"
 */

object Evaluate1UserFactsEvidenceLargeContextBatched extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate user facts evidence with 2 statements in large context.
 */
object Evaluate2UserFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate1UserFactsEvidenceLargeContextFlash extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate2UserFactsEvidenceLargeContextFlash extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate3UserFactsEvidenceLargeContextFlash extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(3))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate4UserFactsEvidenceLargeContextFlash extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(4))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate2UserFactsEvidenceBlockLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = BlockBasedMemoryFactory
}

/**
 * Evaluate user facts evidence with 3 statements in large context.
 */
object Evaluate3UserFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(3))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate user facts evidence with 4 statements in large context.
 */
object Evaluate4UserFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(4))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate user facts evidence with 5 statements in large context.
 */
object Evaluate5UserFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}


/**
 * Evaluate user facts evidence with 6 statements in large context.
 */
object Evaluate6UserFactsEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate6UserFactsEvidenceLargeContextPro extends Evaluator {
  override def model: LLMModel = Gemini.pro
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate6UserFactsEvidenceLargeContextFlashLight extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate5UserFactsEvidenceLargeContextPro extends Evaluator {
  override def model: LLMModel = Gemini.pro
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate5UserFactsEvidenceLargeContextFlashLight extends Evaluator {
  override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate5UserFactsEvidenceLargeContexto4Mini extends Evaluator {
  override def model: LLMModel = OpenAI.gpt4oMini
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate6UserFactsEvidenceLargeContexto4Mini extends Evaluator {
  override def model: LLMModel = OpenAI.gpt4oMini
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate5UserFactsEvidenceLargeContexto4 extends Evaluator {
  override def model: LLMModel = OpenAI.gpt4o
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate6UserFactsEvidenceLargeContexto4 extends Evaluator {
  override def model: LLMModel = OpenAI.gpt4o
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate6UserFactsEvidenceLargeContextsonnet extends Evaluator {
  override def model: LLMModel = Claude.sonnet
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate5UserFactsEvidenceLargeContextsonnet extends Evaluator {
  override def model: LLMModel = Claude.sonnet
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}


/**
 * Evaluate user facts evidence with 2 statements using mem0.
 */
object Evaluate1UserFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(1)
}

object Evaluate2UserFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(2)
}

object Evaluate3UserFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(3)
}

object Evaluate4UserFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(4)
}

/**
 * Block-based evaluators for user facts evidence.
 * These use the block-based memory system that processes conversations in chunks.
 */
object Evaluate1UserFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(1)
}

object Evaluate2UserFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(2)
}

object Evaluate3UserFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(3)
}

object Evaluate4UserFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(4)
}

object Evaluate5UserFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(5)
}

object Evaluate6UserFactsEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(6)
}

object Evaluate5UserFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(5)
}

object Evaluate6UserFactsEvidenceMem0 extends Mem0Evaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(6)
}

