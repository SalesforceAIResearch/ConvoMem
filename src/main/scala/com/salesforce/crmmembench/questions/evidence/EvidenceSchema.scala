package com.salesforce.crmmembench.questions.evidence

/**
 * Unified evidence conversation schema that supports:
 * - Variable number of evidence pieces (0, 1, 2, or more)
 * - Evidence from both User and Assistant speakers
 * - Multiple conversation contexts
 * - Abstinence case (no evidence -> "I do not know")
 */

/**
 * Represents a single message in a conversation.
 * @param speaker The speaker of the message (e.g., "User", "Assistant")
 * @param text The text content of the message
 */
case class Message(speaker: String, text: String)

/**
 * Represents a conversation as a structured entity.
 * Contains a list of messages and optional ID for tracking.
 */
case class Conversation(
  messages: List[Message],
  id: Option[String] = None, // Optional ID for conversation tracking, generated after LLM conversation creation
  containsEvidence: Option[Boolean] = None, // Optional flag indicating if this conversation contains evidence
  model_name: Option[String] = None // Optional - tracks which model was used to generate this conversation
)

/**
 * Intermediate case class for parsing LLM-generated evidence responses (step 1).
 * Contains only the core evidence without conversations.
 */
case class GeneratedEvidenceCore(
  question: String,
  answer: String,
  message_evidences: List[Message],
  model_name: Option[String] = None // Optional - tracks which model was used to generate this evidence core
)

/**
 * Response from conversation generation (step 2).
 * Contains the conversations generated based on evidence core.
 */
case class GeneratedConversations(
  conversations: List[Conversation]
)

/**
 * Complete evidence item that includes both LLM-generated content and usecase metadata.
 * This is what gets saved to evidence files and used throughout the system.
 */
case class EvidenceItem(
  question: String,
  answer: String,
  message_evidences: List[Message],
  conversations: List[Conversation], // Multiple conversation contexts
  category: String, // From the original usecase
  scenario_description: Option[String], // From the original usecase
  personId: Option[String] = None, // Optional person ID for linking evidence to specific personas
  use_case_model_name: Option[String] = None, // Optional - tracks which model was used for use case generation
  core_model_name: Option[String] = None // Optional - tracks which model was used for evidence core generation
)

/**
 * Payload for evidence collections.
 */
case class EvidencePayload(
  evidence_items: List[EvidenceItem],
  checkpoint: Option[String] = None // Git commit hash when this evidence was generated
)

/**
 * Use case definition for evidence generation.
 * Contains only the scenario description - evidence core generation will create the actual evidence, question, and answer.
 */
case class EvidenceUseCase(
  id: Int,
  category: String,
  scenario_description: String, // Required - describes the scenario for evidence core generation
  model_name: Option[String] = None // Optional - tracks which model was used to generate this use case
)

/**
 * Collection of use cases for evidence generation.
 */
case class EvidenceUseCases(
  use_cases: List[EvidenceUseCase]
)

/**
 * Evidence configuration that defines the behavior for different evidence counts.
 */
sealed trait EvidenceConfig {
  def evidenceCount: Int
  def resourcePath: String
}




/**
 * Configuration for user facts evidence cases (recalling and aggregating user-provided facts).
 * Handles both single fact recall (1 evidence) and multi-fact aggregation (2+ evidence).
 */
case class UserFactsEvidenceConfig(count: Int) extends EvidenceConfig {
  def evidenceCount: Int = count
  def resourcePath: String = s"src/main/resources/questions/evidence/user_evidence/${count}_evidence"
}

/**
 * Flexible configuration for multi-evidence cases (1, 2, 3, 4+).
 * Now unified for all evidence counts including 1.
 * @deprecated Use UserFactsEvidenceConfig instead for better naming consistency
 */
@deprecated("Use UserFactsEvidenceConfig instead", "current")
case class MultiEvidenceConfig(count: Int) extends EvidenceConfig {
  def evidenceCount: Int = count
  def resourcePath: String = s"src/main/resources/questions/evidence/user_evidence/${count}_evidence"
}

/**
 * Configuration for changing evidence cases (facts that change over time).
 */
case class ChangingEvidenceConfig(
  evidenceCount: Int,
  changeCount: Int
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/questions/evidence/changing_evidence/${evidenceCount}_evidence"
}

/**
 * Configuration for abstention evidence cases (no answer available in context).
 */
case class AbstentionEvidenceConfig(
  evidenceCount: Int
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/questions/evidence/abstention_evidence/${evidenceCount}_evidence"
}

/**
 * Configuration for assistant facts evidence cases (recalling assistant statements/recommendations).
 */
case class AssistantFactsEvidenceConfig(
  evidenceCount: Int
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/questions/evidence/assistant_facts_evidence/${evidenceCount}_evidence"
}

/**
 * Configuration for preference evidence cases (testing model's ability to remember and apply user preferences).
 * Unlike factual recall, preferences test whether the model can make personalized recommendations
 * based on remembered user preferences. The answer is a rubric describing appropriate responses.
 */
case class PreferenceEvidenceConfig(
  evidenceCount: Int
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/questions/evidence/preference_evidence/${evidenceCount}_evidence"
}

/**
 * Configuration for implicit connection evidence cases (testing model's ability to make non-obvious connections).
 * The user shares important context (health issues, life changes, constraints) that should implicitly 
 * influence future recommendations, even when later questions don't reference this context.
 * Like preferences, uses rubric-based answers to evaluate response quality.
 */
case class ImplicitConnectionEvidenceConfig(
  evidenceCount: Int
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/questions/evidence/implicit_connection_evidence/${evidenceCount}_evidence"
}

/**
 * Configuration for temporal evidence cases (testing model's ability to track time-based information).
 * This configuration supports scenarios where timing and temporal relationships are important.
 */
case class TemporalEvidenceConfig(
  evidenceCount: Int,
  evidenceGenStartYear: Int = 2023,
  evidenceGenEndYear: Int = 2024
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/questions/evidence/temporal_evidence/${evidenceCount}_evidence"
}

/**
 * Configuration for conversation generation.
 * This is used to generate general conversations that don't follow the typical evidence patterns.
 */
case class ConversationEvidenceConfig(
  evidenceCount: Int = 0
) extends EvidenceConfig {
  def resourcePath: String = s"src/main/resources/conversations/irrelevant"
}

/**
 * Configuration for LongMemEval multi-session evidence (includes single-session-user).
 */
case class LongMemEvalMultiSessionConfig(
  override val evidenceCount: Int = 2
) extends EvidenceConfig {
  override def resourcePath: String = s"src/main/resources/questions/longmemeval/multi_session/${evidenceCount}_evidence"
}

/**
 * Configuration for LongMemEval preferences evidence.
 */
case class LongMemEvalPreferencesConfig(
  override val evidenceCount: Int = 1
) extends EvidenceConfig {
  override def resourcePath: String = s"src/main/resources/questions/longmemeval/preferences/${evidenceCount}_evidence"
}

/**
 * Configuration for LongMemEval assistant facts evidence.
 */
case class LongMemEvalAssistantFactsConfig(
  override val evidenceCount: Int = 1
) extends EvidenceConfig {
  override def resourcePath: String = s"src/main/resources/questions/longmemeval/assistant_facts/${evidenceCount}_evidence"
}

/**
 * Configuration for LongMemEval knowledge update evidence.
 */
case class LongMemEvalKnowledgeUpdateConfig(
  override val evidenceCount: Int = 2
) extends EvidenceConfig {
  override def resourcePath: String = s"src/main/resources/questions/longmemeval/knowledge_updates/${evidenceCount}_evidence"
}

/**
 * Configuration for LongMemEval abstention evidence.
 */
case class LongMemEvalAbstentionConfig(
  override val evidenceCount: Int = 2
) extends EvidenceConfig {
  override def resourcePath: String = s"src/main/resources/questions/longmemeval/abstention/${evidenceCount}_evidence"
}

/**
 * Configuration for composite evidence generators.
 * This config is used when combining multiple generators.
 */
case class CompositeEvidenceConfig(
  override val evidenceCount: Int
) extends EvidenceConfig {
  override def resourcePath: String = "" // Not used for composite generators
}
