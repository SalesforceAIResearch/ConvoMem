package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence.Message
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.{Config, Personas}

/**
 * Generator for user facts evidence conversations with configurable evidence count.
 * Handles both single fact recall (1 evidence) and multi-fact aggregation (2+ evidence).
 * 
 * QUALITY ASSURANCE: This generator includes immediate verification of generated evidence.
 * Each evidence item is automatically verified using the same logic as the verification
 * system. If verification fails, the generator retries with a new generation attempt.
 * This ensures that only high-quality, verifiable evidence is produced.
 * 
 * Retry behavior:
 * - Up to 20 retry attempts for each evidence item
 * - Verification failures trigger automatic retry
 * - Logging shows verification attempts when > 10 attempts are needed
 * - Throws exception after 20 failed attempts
 */
class UserFactsEvidenceGenerator(val evidenceCount: Int) extends EvidenceGenerator with StandardVerificationMixin {
  
  override val config: EvidenceConfig = UserFactsEvidenceConfig(evidenceCount)
  
  /**
   * Override the getEvidenceCount method to return the actual evidence count.
   */
  override def getEvidenceCount(): Int = evidenceCount

  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "User facts"

  /**
   * Get implementation-specific prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    if (evidenceCount == 1) {
      // Single fact recall scenario
      EvidenceCorePromptParts(
        scenarioType = "User Fact Recall",
        taskDescription = "You need to generate the question, answer, and evidence message for a test where the user needs to recall a specific fact they previously shared.",
        specificInstructions = "tests the AI's ability to recall a specific user-provided fact",
        fieldDefinitions = s"""question: The question that asks about a specific fact the user previously mentioned.
answer: The correct answer that directly recalls the user's fact.
message_evidences: An array of exactly 1 message object containing the user's fact.""",
        additionalGuidance = Some("Focus on straightforward recall of specific facts, details, preferences, or information the user shared.")
      )
    } else {
      // Multi-fact aggregation scenario
      EvidenceCorePromptParts(
        scenarioType = "User Facts Aggregation",
        taskDescription = s"You need to generate the question, answer, and evidence messages for a test where multiple user facts must be combined or synthesized.",
        specificInstructions = "requires combining or synthesizing multiple user-provided facts",
        fieldDefinitions = s"""question: The question that requires combining information from all $evidenceCount user facts.
answer: The correct answer that synthesizes information from multiple user facts.
message_evidences: An array of exactly $evidenceCount message objects, where each contains a different user fact.""",
        additionalGuidance = Some("Focus on questions that require combining, comparing, or reasoning across multiple facts the user provided.")
      )
    }
  }

  /**
   * Get conversation prompt parts for user facts scenarios.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    if (evidenceCount == 1) {
      ConversationPromptParts(
        evidenceType = "user fact recall",
        scenarioDescription = "This tests the AI's ability to recall specific facts, details, or information that the user previously shared. Unlike complex reasoning tasks, this focuses on pure memory recall - the AI must remember and accurately retrieve specific user-provided information when asked. This tests the AI's fundamental memory capabilities for factual information shared by users, which is essential for maintaining context and providing personalized assistance. The challenge is in accurate storage and retrieval of user-specific details without confusion or fabrication.",
        useCaseScenario = Some(useCase.scenario_description),
        evidenceMessages = evidenceCore.message_evidences,
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        evidenceCount = evidenceCount
      )
    } else {
      ConversationPromptParts(
        evidenceType = "user facts aggregation",
        scenarioDescription = "This tests the AI's ability to combine and synthesize multiple user-provided facts to answer questions that require reasoning across different pieces of information. Unlike single-fact recall where one piece of information suffices, this requires the AI to connect and integrate disparate user facts that may appear in different conversations at different times. This is cognitively complex because it demands working memory to hold multiple pieces of information simultaneously while reasoning about their relationships. The AI must demonstrate it can perform information fusion - not just recall individual facts, but integrate them to derive new insights or comprehensive answers about the user.",
        useCaseScenario = Some(useCase.scenario_description),
        evidenceMessages = evidenceCore.message_evidences,
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        evidenceCount = evidenceCount
      )
    }
  }

  // Use standard verification from mixin - explicit override to resolve conflict
  override def getVerificationChecks(): List[VerificationCheck] = 
    super[StandardVerificationMixin].getVerificationChecks()

  /**
   * Get the answering evaluation strategy for user facts evidence.
   * Uses custom evaluation when evidenceCount > 1 to verify information
   * from all evidence messages is reflected in the answer.
   */
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    new UserFactsAnsweringEvaluation(evidenceCount)
  }

  /**
   * Get implementation-specific prompt parts for use case summary generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    if (evidenceCount == 1) {
      // Single fact recall scenarios
      val evidenceDistribution = "In each test, the user provides one specific fact, detail, or piece of information."
      
      val coreTaskDescription = s"""These scenarios specifically test "user fact recall" - situations where the AI must remember and accurately retrieve specific information that the user previously shared. This tests fundamental memory capabilities for user-provided facts.

Key Pattern: User Shares Fact → User Asks About Fact
Each scenario follows this pattern:
1. User shares a specific fact, detail, preference, or piece of information
2. User later asks a question about that information
3. The correct answer directly recalls the user's fact"""

      val exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Professional Life", 
        scenario_description = "The project manager shares the specific budget allocation for Project Phoenix's Q3 phase with the AI during a planning discussion. Later, when preparing a quarterly report, they ask 'What was the budget I mentioned for Project Phoenix's Q3 phase?' The scenario tests whether the AI can accurately remember and retrieve this specific user-provided financial detail."
      )

      UseCaseSummaryPromptParts(
        evidenceTypeDescription = "an AI's ability to recall specific user-provided facts",
        coreTaskDescription = coreTaskDescription,
        evidenceDistributionDescription = evidenceDistribution,
        exampleUseCase = exampleUseCase,
        additionalRequirements = Some("Types of User Facts: Include various types of factual information such as:\n- Personal preferences and choices\n- Specific details and specifications\n- Names, dates, numbers, and identifiers\n- Decisions and plans\n- Experiences and events")
      )
    } else {
      // Multi-fact aggregation scenarios
      val evidenceDistribution = evidenceCount match {
        case 2 => "In each test, the user provides two related facts or pieces of information at different times."
        case 3 => "In each test, the user provides three related facts or pieces of information at different times."
        case 4 => "In each test, the user provides four related facts or pieces of information at different times."
        case n => s"In each test, the user provides $n related facts or pieces of information at different times."
      }
      
      val coreTaskDescription = s"""These scenarios are designed to test an AI's ability to combine and synthesize user-provided facts across $evidenceCount distinct interactions. Each scenario will form the basis for a "$evidenceCount-evidence aggregation" test. $evidenceDistribution Later, the user asks a question that requires combining, comparing, or reasoning over all $evidenceCount user facts."""

      val exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Professional Life",
        scenario_description = s"The project manager shares various details about Project Phoenix with the AI across multiple conversations - the budget in one discussion, the timeline in another, and team assignments in a third. Later, when preparing for a stakeholder meeting, they ask 'Based on what I've told you about Project Phoenix, what's the total resource allocation across all phases?' The scenario requires the AI to combine budget, timeline, and staffing information from different discussions to provide a comprehensive answer."
      )

      UseCaseSummaryPromptParts(
        evidenceTypeDescription = "an AI's ability to combine and synthesize multiple user-provided facts",
        coreTaskDescription = coreTaskDescription,
        evidenceDistributionDescription = evidenceDistribution,
        exampleUseCase = exampleUseCase,
        additionalRequirements = Some("Types of User Fact Combinations: Include various combinations such as:\n- Comparing preferences or choices\n- Calculating totals or aggregates from user data\n- Timeline reconstruction from multiple events\n- Decision analysis based on multiple criteria\n- Pattern recognition across user-provided information")
      )
    }
  }

}