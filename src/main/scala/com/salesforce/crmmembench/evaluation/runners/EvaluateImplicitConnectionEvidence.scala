package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.{BatchedTestCasesGenerator, BlockBasedEvaluator, Evaluator, ExtractedContextEvaluator, Mem0Evaluator, TestCasesGenerator}
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators.ImplicitConnectionEvidenceGenerator

/**
 * Evaluation entry points for implicit connection evidence.
 * These evaluations test how well models can identify implicit connections
 * between pieces of information stated across different conversations.
 *
 * Implicit connection evidence tests scenarios where:
 * - Information is provided in separate contexts/conversations
 * - User asks a question that requires connecting these separate pieces
 * - Model must infer the connection to provide the correct answer
 *
 * Example pattern:
 * - Conversation 1: "Sarah manages the marketing team"
 * - Conversation 2: "The marketing team uses Slack for communication"
 * - User: "What tool does Sarah's team use for communication?"
 * - Expected answer: "Slack"
 */

/**
 * Evaluate implicit connection evidence with 1 connection in large context.
 */
object Evaluate2ImplicitConnectionEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new ImplicitConnectionEvidenceGenerator(2))

  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate2ImplicitConnectionEvidenceLargeContextPro extends Evaluator {
  override def model: LLMModel = Gemini.pro
  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new ImplicitConnectionEvidenceGenerator(2))

  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate2ImplicitConnectionEvidenceLargeContextFlash extends Evaluator {
  override def model: LLMModel = Gemini.flashLite

  override def testCasesGenerator: TestCasesGenerator =
    new BatchedTestCasesGenerator(new ImplicitConnectionEvidenceGenerator(2))

  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate2ImplicitConnectionEvidenceBlockBasedFlashLite extends BlockBasedEvaluator {
  override def model: LLMModel = Gemini.flashLite
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate2ImplicitConnectionEvidenceBlockBasedFlash extends BlockBasedEvaluator {
  override def model: LLMModel = Gemini.flash
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate1ImplicitConnectionEvidenceBlockBasedProWithHelperModel extends BlockBasedEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flashLite)
  override def model: LLMModel = Gemini.pro
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate1ImplicitConnectionEvidenceBlockBasedProWithHelperModelFlash extends BlockBasedEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flashLite)
  override def model: LLMModel = Gemini.flash
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate1ImplicitConnectionEvidenceExtracted extends ExtractedContextEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flash)
  override def model: LLMModel = Gemini.flash
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate1ImplicitConnectionEvidenceExtractedflashLite extends ExtractedContextEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flashLite)
  override def model: LLMModel = Gemini.flash
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate1ImplicitConnectionEvidenceExtractedPro extends ExtractedContextEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flashLite)
  override def model: LLMModel = Gemini.pro
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate1ImplicitConnectionEvidenceExtractedProFlashHelperModel extends ExtractedContextEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flash)
  override def model: LLMModel = Gemini.pro
  def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

/**
 * Evaluate implicit connection evidence with 1 connection using mem0.
 */
object Evaluate1ImplicitConnectionEvidenceMem0 extends Mem0Evaluator {
  override def model: LLMModel = Gemini.pro
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}