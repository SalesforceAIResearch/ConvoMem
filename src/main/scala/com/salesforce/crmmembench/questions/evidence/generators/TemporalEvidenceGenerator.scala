package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.Personas

/**
 * Generator for temporal evidence - testing the model's ability to track time-based information.
 * This includes calculating durations, understanding time relationships, and tracking deadlines.
 * 
 * Evidence Count Patterns:
 * - 1: Single temporal fact with duration/age calculation
 * - 2: Two temporal facts requiring comparison or sequencing
 * - 3+: Complex timelines with multiple events
 * 
 * Uses flexible verification since temporal answers can have legitimate variations.
 */
class TemporalEvidenceGenerator(evidenceCount: Int = 1) 
  extends EvidenceGenerator {
  
  override val config: EvidenceConfig = TemporalEvidenceConfig(evidenceCount)
  
  override def getEvidenceTypeName: String = "temporal"
  
  /**
   * No verification for temporal evidence.
   * Temporal answers can have legitimate variations (e.g., off-by-one in day counting,
   * "3 months" vs "about 3 months"), so strict verification would fail too often.
   */
  override def getVerificationChecks(): List[VerificationCheck] = {
    VerificationConstants.NO_VERIFICATION // No verification needed
  }
  
  /**
   * Use temporal evaluation that allows flexibility in time expressions.
   */
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    TemporalAnsweringEvaluation
  }
  
  override def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    val exampleUseCase = EvidenceUseCase(
      id = 1,
      category = "Professional Life",
      scenario_description = evidenceCount match {
        case 1 => "The marketing manager started a new certification program 'three months ago' and needs to know how long they've been studying to update their resume."
        case 2 => "The manager attended a conference 'last month' and completed a related certification 'two weeks ago', and needs to determine the order for their performance review."
        case _ => "The manager launched multiple marketing campaigns over 'the past quarter' and needs to track the timeline and durations for reporting to leadership."
      }
    )
    
    UseCaseSummaryPromptParts(
      evidenceTypeDescription = "temporal reasoning evidence",
      coreTaskDescription = s"""This evidence type tests the AI's ability to understand and reason about time-based relationships. 
The user will share information with temporal context (dates, durations, time markers) across $evidenceCount conversation(s), 
and later ask questions that require temporal reasoning (calculating durations, ordering events, understanding time relationships).""",
      evidenceDistributionDescription = evidenceCount match {
        case 1 => "the user shares one temporal fact and later asks about duration, age, or time elapsed"
        case 2 => "the user shares two temporal facts in separate conversations and later asks about their relationship (ordering, duration between, etc.)"
        case n => s"the user shares $n temporal facts across multiple conversations and later asks complex questions about timelines, aggregations, or sequences"
      },
      exampleUseCase = exampleUseCase,
      additionalRequirements = Some(
        """Temporal Context Requirements:
- Each scenario must involve clear time markers ("last month", "two weeks ago", "in three days")
- The temporal information must be essential to answering the eventual question
- Use realistic time scales appropriate to the context
- Questions should require calculation or reasoning, not just recall of dates""")
    )
  }
  
  override def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    EvidenceCorePromptParts(
      scenarioType = "Temporal Reasoning",
      taskDescription = s"You need to generate the question, answer, and evidence messages for a test where temporal reasoning is required to answer correctly.",
      specificInstructions = "requires understanding time relationships and performing temporal calculations",
      fieldDefinitions = s"""question: A question that requires temporal reasoning (calculating durations, ordering events, or understanding time relationships).
answer: The correct answer based on temporal calculation or reasoning.
message_evidences: An array of exactly $evidenceCount message object(s) containing temporal facts with clear time markers.""",
      additionalGuidance = Some(evidenceCount match {
        case 1 => """Single Evidence Temporal Guidance:
- The evidence should establish one fact with a clear time marker (e.g., "started three months ago")
- The question should ask about duration, age, or time elapsed since that fact
- The answer should be a specific time calculation
- Example: Evidence mentions starting something "two months ago", question asks "How long have I been...", answer is "Two months"""
        case 2 => """Two Evidence Temporal Guidance:
- Each evidence should establish a temporal fact at different times
- The question should ask about the relationship between them (which first, duration between, etc.)
- Use varied time scales for interest
- Example: First evidence "completed certification last month", second evidence "attended conference two weeks ago", question asks which happened first"""
        case n => s"""Multiple Evidence Temporal Guidance:
- Create $n temporal facts that form a timeline or pattern
- The question should require reasoning across multiple time points
- Consider complex patterns like aggregations, sequences, or multi-stage processes
- Example: Multiple events over time, asking for total duration, specific ordering, or time patterns"""
      })
    )
  }
  
  override def getConversationPromptParts(
    person: Personas.Person,
    useCase: EvidenceUseCase,
    evidenceCore: GeneratedEvidenceCore
  ): ConversationPromptParts = {
    ConversationPromptParts(
      evidenceType = "temporal reasoning",
      scenarioDescription = "Tests the AI's ability to track and reason about time-based information",
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = evidenceCore.message_evidences.length
    )
  }
}