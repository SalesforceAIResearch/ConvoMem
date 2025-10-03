package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.evaluation.ExtractedContextEvaluator
import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.generators._

/**
 * Extracted context evaluators for various evidence types.
 * These use the extracted context memory system that processes conversations in two stages:
 * 1. Extract relevant information from all conversations
 * 2. Answer questions based on the extracted context
 */

// =============================================================================
// User Facts Evidence Evaluators
// =============================================================================

/**
 * Evaluate user facts evidence with extracted context memory.
 * Tests how well the system can extract and recall user-stated facts.
 */
object Evaluate1UserFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(1)
}

object Evaluate2UserFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(2)
}

object Evaluate3UserFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(3)
}

object Evaluate4UserFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(4)
}

object Evaluate5UserFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(5)
}

object Evaluate6UserFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new UserFactsEvidenceGenerator(6)
}

// =============================================================================
// Assistant Facts Evidence Evaluators
// =============================================================================

/**
 * Evaluate assistant facts evidence with extracted context memory.
 * Tests how well the system can extract and recall assistant-stated facts.
 */
object Evaluate1AssistantFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(1)
}

object Evaluate2AssistantFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(2)
}

object Evaluate3AssistantFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(3)
}

object Evaluate4AssistantFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(4)
}

object Evaluate5AssistantFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(5)
}

object Evaluate6AssistantFactsEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AssistantFactsEvidenceGenerator(6)
}

// =============================================================================
// Changing Evidence Evaluators
// =============================================================================

/**
 * Evaluate changing evidence with extracted context memory.
 * Tests how well the system can extract and track information that changes over time.
 */
object Evaluate1ChangingEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(1)
}

object Evaluate2ChangingEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(2)
}

object Evaluate3ChangingEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(3)
}

object Evaluate4ChangingEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(4)
}

object Evaluate5ChangingEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(5)
}

object Evaluate6ChangingEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ChangingEvidenceGenerator(6)
}

// =============================================================================
// Implicit Connection Evidence Evaluators
// =============================================================================

/**
 * Evaluate implicit connection evidence with extracted context memory.
 * Tests how well the system can extract and connect related pieces of information.
 */
object Evaluate1ImplicitConnectionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(1)
}

object Evaluate2ImplicitConnectionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(2)
}

object Evaluate3ImplicitConnectionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(3)
}

object Evaluate4ImplicitConnectionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(4)
}

object Evaluate5ImplicitConnectionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(5)
}

object Evaluate6ImplicitConnectionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new ImplicitConnectionEvidenceGenerator(6)
}

// =============================================================================
// Abstention Evidence Evaluators
// =============================================================================

/**
 * Evaluate abstention evidence with extracted context memory.
 * Tests how well the system can recognize when information is not available.
 */
object Evaluate1AbstentionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AbstentionEvidenceGenerator(1)
}

object Evaluate2AbstentionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AbstentionEvidenceGenerator(2)
}

object Evaluate3AbstentionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AbstentionEvidenceGenerator(3)
}

object Evaluate4AbstentionEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new AbstentionEvidenceGenerator(4)
}

// =============================================================================
// Preference Evidence Evaluators
// =============================================================================

/**
 * Evaluate preference evidence with extracted context memory.
 * Tests how well the system can extract and apply user preferences.
 */
object Evaluate1PreferenceEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(1)
}

object Evaluate2PreferenceEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(2)
}

object Evaluate3PreferenceEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(3)
}

object Evaluate4PreferenceEvidenceExtractedContext extends ExtractedContextEvaluator {
  override def evidenceGenerator: EvidenceGenerator = new PreferenceEvidenceGenerator(4)
}