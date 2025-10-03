package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.{BatchedTestCasesGenerator, BlockBasedEvaluator, Evaluator, Mem0Evaluator, TestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators.ChangingEvidenceGenerator
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, Mem0MemoryFactory, MemoryAnswererFactory}

/**
 * Evaluation entry points for changing evidence in large context.
 * These evaluations test how well models track information that changes over time
 * as the context becomes increasingly diluted with irrelevant conversations.
 */

/**
 * Evaluate 2-message changing evidence (1 change) in large context.
 * Pattern: Initial state → Final state
 * Example: "Meeting at 3pm" → "Meeting cancelled"
 */
object Evaluate2ChangingEvidenceLargeContext extends Evaluator {
   override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

//object Evaluate2ChangingEvidenceLargeContexMem0 extends Mem0Evaluator {
//  override def testCasesGenerator: TestCasesGenerator =
//    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(2))
//  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
//}

object Evaluate2ChangingEvidenceMem0 extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = Mem0MemoryFactory
}

/**
 * Evaluate 3-message changing evidence (2 changes) in large context.
 * Pattern: Initial state → Intermediate change → Final state
 * Example: "Meeting at 3pm" → "Moved to 4pm" → "Meeting cancelled"
 */
object Evaluate3ChangingEvidenceLargeContext extends Evaluator {
   override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(3))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate 4-message changing evidence (3 changes) in large context.
 * Pattern: Initial state → First change → Second change → Final state
 * Example: "Trip to Paris" → "Changed to London" → "Moved to next month" → "Trip cancelled"
 */
object Evaluate4ChangingEvidenceLargeContext extends Evaluator {
   override def model: LLMModel = Gemini.flashLite
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(4))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate 4-message changing evidence (3 changes) in large context.
 * Pattern: Initial state → First change → Second change → Final state
 * Example: "Trip to Paris" → "Changed to London" → "Moved to next month" → "Trip cancelled"
 */
object Evaluate5ChangingEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Evaluate 4-message changing evidence (3 changes) in large context.
 * Pattern: Initial state → First change → Second change → Final state
 * Example: "Trip to Paris" → "Changed to London" → "Moved to next month" → "Trip cancelled"
 */
object Evaluate6ChangingEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new ChangingEvidenceGenerator(6))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

/**
 * Block-based evaluators for changing evidence.
 * These use the block-based memory system that processes conversations in chunks.
 */
object Evaluate2ChangingEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(2)
}

object Evaluate3ChangingEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(3)
}

object Evaluate4ChangingEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(4)
}

object Evaluate5ChangingEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(5)
}

object Evaluate6ChangingEvidenceBlockBased extends BlockBasedEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(6)
}

