package com.salesforce.crmmembench.evaluation.runners.long_mem_eval

import com.salesforce.crmmembench.evaluation._
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}
import com.salesforce.crmmembench.questions.evidence.generators.longmemeval.LongMemEvalMultiSessionGenerator

/**
 * Evaluators for LongMemEval Multi-Session evidence using long context.
 * Each evaluator targets a specific evidence count (1-5).
 */

object EvaluateLongMemEvalMultiSession1LargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalMultiSessionGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object EvaluateLongMemEvalMultiSession2LargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalMultiSessionGenerator(2))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object EvaluateLongMemEvalMultiSession3LargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalMultiSessionGenerator(3))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object EvaluateLongMemEvalMultiSession4LargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalMultiSessionGenerator(4))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object EvaluateLongMemEvalMultiSession5LargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createStandard(new LongMemEvalMultiSessionGenerator(5))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}