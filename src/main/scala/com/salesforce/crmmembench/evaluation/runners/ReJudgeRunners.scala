package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.evaluation.{ReJudge, Evaluator}
import com.salesforce.crmmembench.questions.evidence.generation.{
  AbstentionAnsweringEvaluation,
  DefaultAnsweringEvaluation,
  TemporalAnsweringEvaluation,
  UserFactsAnsweringEvaluation
}
import com.salesforce.crmmembench.LLM_endpoints.{Claude, Gemini, LLMModel, OpenAI}

/**
 * Combined ReJudge runners for all evidence types.
 * These re-evaluate existing evaluation logs with different criteria.
 */

// ============================================================================
// User Facts Evidence ReJudge Runners
// ============================================================================

/**
 * Re-judge 1-message user facts with default criteria.
 */
object ReJudge1UserFactsEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1UserFactsEvidenceLargeContextBatched
}

/**
 * Re-judge 2-message user facts with default criteria.
 */
object ReJudge2UserFactsEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate2UserFactsEvidenceLargeContext
}

/**
 * Re-judge 3-message user facts with default criteria.
 */
object ReJudge3UserFactsEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate3UserFactsEvidenceLargeContext
}

/**
 * Re-judge 5-message user facts with more lenient criteria (require only 4).
 */
object ReJudge5UserFactsEvidenceLenient extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate5UserFactsEvidenceLargeContext
  override def answeringEvaluation = new UserFactsAnsweringEvaluation(evidenceCount = 4)
}

/**
 * Re-judge 6-message user facts with default answering evaluation.
 */
object ReJudge6UserFactsEvidenceDefault extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate6UserFactsEvidenceLargeContext
  override def answeringEvaluation = DefaultAnsweringEvaluation
}

/**
 * Re-judge 2-message user facts with block-based memory.
 */
object ReJudge2UserFactsEvidenceBlock extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate2UserFactsEvidenceBlockLargeContext
}

// ============================================================================
// Assistant Facts Evidence ReJudge Runners
// ============================================================================

/**
 * Re-judge 1-message assistant facts.
 */
object ReJudge1AssistantFactsEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1AssistantFactsEvidenceLargeContext
}

/**
 * Re-judge 2-message assistant facts.
 */
object ReJudge2AssistantFactsEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate2AssistantFactsEvidenceLargeContext
}

/**
 * Re-judge 3-message assistant facts in SHORT mode.
 */
object ReJudge3AssistantFactsEvidenceShort extends ReJudge {
  override val runShort = true
  override def originalEvaluator: Evaluator = Evaluate3AssistantFactsEvidenceLargeContext
}

/**
 * Re-judge 4-message assistant facts with Claude Sonnet as judge.
 */
object ReJudge4AssistantFactsEvidenceClaude extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate4AssistantFactsEvidenceLargeContext
  override def judgeModel = Claude.sonnet
}

/**
 * Re-judge assistant facts with different evaluation criteria.
 */
object ReJudge1AssistantFactsEvidenceDefault extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1AssistantFactsEvidenceLargeContext
  override def answeringEvaluation = DefaultAnsweringEvaluation
}

// ============================================================================
// Changing Evidence ReJudge Runners
// ============================================================================

/**
 * Re-judge 2-message changing evidence with temporal evaluation.
 */
object ReJudge2ChangingEvidenceTemporal extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate2ChangingEvidenceLargeContext
  override def answeringEvaluation = TemporalAnsweringEvaluation
}

/**
 * Re-judge 3-message changing evidence with temporal evaluation.
 */
object ReJudge3ChangingEvidenceTemporal extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate3ChangingEvidenceLargeContext
  override def answeringEvaluation = TemporalAnsweringEvaluation
}

/**
 * Re-judge 2-message changing evidence in SHORT mode.
 */
object ReJudge2ChangingEvidenceShort extends ReJudge {
  override val runShort = true
  override def originalEvaluator: Evaluator = Evaluate2ChangingEvidenceLargeContext
}

/**
 * Re-judge 4-message changing evidence.
 */
object ReJudge4ChangingEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate4ChangingEvidenceLargeContext
}

/**
 * Re-judge changing evidence with default (non-temporal) evaluation.
 */
object ReJudge2ChangingEvidenceDefault extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate2ChangingEvidenceLargeContext
  override def answeringEvaluation = DefaultAnsweringEvaluation
}

// ============================================================================
// Abstention Evidence ReJudge Runners
// ============================================================================

/**
 * Re-judge 1-message abstention evidence with same criteria.
 * Useful for testing ReJudge functionality.
 */
object ReJudge1AbstentionEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1AbstentionEvidenceLargeContext
}

/**
 * Re-judge 1-message abstention evidence in SHORT mode.
 */
object ReJudge1AbstentionEvidenceShort extends ReJudge {
  override val runShort = true
  override def originalEvaluator: Evaluator = Evaluate1AbstentionEvidenceLargeContext
}

/**
 * Re-judge 2-message abstention evidence.
 */
object ReJudge2AbstentionEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate2AbstentionEvidenceLargeContext
}

/**
 * Re-judge 3-message abstention evidence.
 */
object ReJudge3AbstentionEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate3AbstentionEvidenceLargeContext
}

/**
 * Re-judge abstention evidence with GPT-4o as judge.
 */
object ReJudge1AbstentionEvidenceGPT4 extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1AbstentionEvidenceLargeContext
  override def judgeModel = OpenAI.gpt4o
}

/**
 * Re-judge abstention evidence with Gemini Pro as judge.
 */
object ReJudge1AbstentionEvidenceGeminiPro extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1AbstentionEvidenceLargeContext
  override def judgeModel = Gemini.pro
}

// ============================================================================
// Preference Evidence ReJudge Runners
// ============================================================================

/**
 * Re-judge 1-message preference evidence.
 */
object ReJudge1PreferenceEvidence extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1PreferenceEvidenceLargeContext
}


/**
 * Re-judge preference evidence with different evaluation criteria.
 */
object ReJudge1PreferenceEvidenceDefault extends ReJudge {
  override def originalEvaluator: Evaluator = Evaluate1PreferenceEvidenceLargeContext
  override def answeringEvaluation = DefaultAnsweringEvaluation
}