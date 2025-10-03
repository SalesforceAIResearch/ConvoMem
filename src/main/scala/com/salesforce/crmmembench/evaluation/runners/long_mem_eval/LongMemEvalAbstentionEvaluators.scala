package com.salesforce.crmmembench.evaluation.runners.long_mem_eval

import com.salesforce.crmmembench.evaluation._
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}
import com.salesforce.crmmembench.questions.evidence.generators.longmemeval.LongMemEvalAbstentionGenerator
import com.salesforce.crmmembench.LLM_endpoints.Gemini
import com.salesforce.crmmembench.questions.evidence.AbstentionCompositeGenerator

/**
 * Evaluators for LongMemEval Abstention evidence using long context.
 * Only 1 and 2 evidence counts (3 and 4 were consolidated into 2).
 */

object EvaluateLongMemEvalAbstention1LargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(AbstentionCompositeGenerator)
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}