package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.{Evaluator, ExtractedContextEvaluator, Mem0Evaluator, TestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators.{ImplicitConnectionEvidenceGenerator, PreferenceEvidenceGenerator}
import com.salesforce.crmmembench.evaluation.memory.{LongContextMemoryFactory, MemoryAnswererFactory}

/**
 * Evaluation entry points for preference evidence in large context.
 * These evaluations test how well models remember and apply user preferences
 * when making recommendations or providing personalized responses.
 * 
 * Preference evidence tests scenarios where users express preferences and later ask for recommendations:
 * - User expresses preferences naturally in conversation
 * - Later, user asks for recommendations in related areas
 * - Model must provide responses that align with the user's preferences
 * 
 * Key difference from factual evidence: Answers are rubrics describing appropriate responses,
 * not specific facts. The evaluation framework will need to be updated to handle rubric-based
 * evaluation in the future.
 * 
 * Example pattern:
 * - User: "I've been really enjoying working with React for my frontend projects"
 * - User (later): "Can you recommend some resources for learning frontend development?"
 * - Expected answer (rubric): "The user would prefer responses that suggest React-specific resources..."
 */

/**
 * Evaluate preference evidence with 1 preference in large context.
 */
object Evaluate1PreferenceEvidenceLargeContext extends Evaluator {
  override def testCasesGenerator: TestCasesGenerator = 
    TestCasesGenerator.createBatched(new PreferenceEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate1PreferenceEvidenceLargeContextflashLite extends Evaluator {
  override def model: LLMModel = Gemini.flash
  override def testCasesGenerator: TestCasesGenerator =
    TestCasesGenerator.createBatched(new PreferenceEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate1PreferenceEvidenceLargeContextPro extends Evaluator {
  override def model: LLMModel = Gemini.pro
  override def testCasesGenerator: TestCasesGenerator =
    TestCasesGenerator.createBatched(new PreferenceEvidenceGenerator(1))
  override def memoryFactory: MemoryAnswererFactory = LongContextMemoryFactory
}

object Evaluate1PreferenceEvidenceMem0 extends Mem0Evaluator {
  override def model: LLMModel = Gemini.flash
  override def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(1)
}

object Evaluate1PreferenceEvidenceExtracted extends ExtractedContextEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flash)
  override def model: LLMModel = Gemini.flash
  def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(1)
}

object Evaluate1PreferenceEvidenceExtractedflashLite extends ExtractedContextEvaluator {
  override def helperModel: Option[LLMModel] = Some(Gemini.flashLite)
  override def model: LLMModel = Gemini.flash
  def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(1)
}