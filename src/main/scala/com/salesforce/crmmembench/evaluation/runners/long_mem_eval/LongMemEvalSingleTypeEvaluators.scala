package com.salesforce.crmmembench.evaluation.runners.long_mem_eval

import com.salesforce.crmmembench.evaluation._
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}
import com.salesforce.crmmembench.questions.evidence.generators.longmemeval._

/**
 * Evaluators for LongMemEval single evidence count types using long context.
 * These types only have one evidence count configuration.
 */

object EvaluateLongMemEvalAssistantFactsLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalAssistantFactsGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object EvaluateLongMemEvalKnowledgeUpdatesLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalKnowledgeUpdateGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object EvaluateLongMemEvalPreferencesLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalPreferencesGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}