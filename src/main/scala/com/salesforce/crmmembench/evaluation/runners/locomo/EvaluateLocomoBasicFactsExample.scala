package com.salesforce.crmmembench.evaluation.runners.locomo

import com.salesforce.crmmembench.evaluation.Evaluator
import com.salesforce.crmmembench.evaluation.generators._
import com.salesforce.crmmembench.evaluation.memory.{MemoryAnswererFactory, LongContextMemoryFactory}
import com.salesforce.crmmembench.questions.evidence.generation._

/**
 * Example evaluator for Locomo Basic Facts category.
 * 
 * This demonstrates how to use the Locomo generators in an actual evaluation.
 * Run this to evaluate how well a model handles basic fact recall questions
 * from the Locomo benchmark.
 */
object EvaluateLocomoBasicFacts extends Evaluator {
  override def testCasesGenerator = LocomoBasicFactsGenerator
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
  override def answeringEvaluation: AnsweringEvaluation = DefaultAnsweringEvaluation
}

/**
 * Example evaluator for Locomo Temporal questions.
 */
object EvaluateLocomoTemporal extends Evaluator {
  override def testCasesGenerator = LocomoTemporalGenerator
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
  override def answeringEvaluation: AnsweringEvaluation = TemporalAnsweringEvaluation
}

/**
 * Example evaluator for Locomo Reasoning questions.
 */
object EvaluateLocomoReasoning extends Evaluator {
  override def testCasesGenerator = LocomoReasoningGenerator
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
  override def answeringEvaluation: AnsweringEvaluation = RubricBasedAnsweringEvaluation
}

/**
 * Example evaluator for Locomo Multi-Session questions.
 */
object EvaluateLocomoMultiSession extends Evaluator {
  override def testCasesGenerator = LocomoMultiSessionGenerator
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
  override def answeringEvaluation: AnsweringEvaluation = DefaultAnsweringEvaluation
}

/**
 * Example evaluator for Locomo Abstention questions.
 */
object EvaluateLocomoAbstention extends Evaluator {
  override def testCasesGenerator = LocomoAbstentionGenerator
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
  override def answeringEvaluation: AnsweringEvaluation = AbstentionAnsweringEvaluation
}

/**
 * Example evaluator for a subset of Locomo Temporal questions.
 * This demonstrates how to evaluate only specific datasets.
 */
object EvaluateLocomoTemporalSubset extends Evaluator {
  // Use only datasets 0, 1, and 2 for faster testing
  override def testCasesGenerator = 
    LocomoTestCasesGenerator(LocomoCategories.Temporal, List(0, 1, 2))
  
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
  override def answeringEvaluation: AnsweringEvaluation = TemporalAnsweringEvaluation
}