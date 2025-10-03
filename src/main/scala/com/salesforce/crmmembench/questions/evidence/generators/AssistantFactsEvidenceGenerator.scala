package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence.Message
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.{Config, Personas}

/**
 * Generator for assistant facts evidence conversations where users need to recall what the assistant said/recommended.
 * Examples: Assistant says "I would recommend Roscioli restaurant" -> Question: "What restaurant did you recommend?" -> "Roscioli"
 * 
 * Supports configurable evidence counts:
 * - evidenceCount: Number of assistant statements to embed (e.g., 2 for multiple recommendations)
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
class AssistantFactsEvidenceGenerator(val evidenceCount: Int) 
  extends EvidenceGenerator 
  with StandardVerificationMixin {
  
  override val config: EvidenceConfig = AssistantFactsEvidenceConfig(evidenceCount)


  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "Assistant facts"

  /**
   * Get implementation-specific prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    if (evidenceCount == 1) {
      // Single assistant fact recall scenario
      EvidenceCorePromptParts(
        scenarioType = "Assistant Fact Recall",
        taskDescription = "You need to generate the question, answer, and evidence message for a test where the user needs to recall a specific statement the assistant made.",
        specificInstructions = "tests the AI's ability to recall a specific statement it previously made",
        fieldDefinitions = s"""question: The question that asks the user to recall what the assistant previously said.
answer: The correct answer that exactly matches the assistant's previous statement.
message_evidences: An array of exactly 1 message object containing the assistant's statement to be recalled.""",
        additionalGuidance = Some("""Focus on straightforward recall of specific assistant statements, recommendations, or advice.

IMPORTANT: Evidence messages should be written as if they are being spoken in real-time during a conversation. Use present tense or conversational language (e.g., "I recommend...", "You should try...", "The best approach is...") rather than past tense (avoid "I recommended...", "I suggested..."). The messages should feel natural and immediate, as if the assistant is speaking them right now.

AVOID: Simply copying the answer text verbatim into the message. The message should feel like natural conversation, not a robotic repetition of the answer. Add conversational context, transitions, or framing to make it feel authentic.""")
      )
    } else {
      // Multi-assistant fact aggregation scenario
      EvidenceCorePromptParts(
        scenarioType = "Assistant Facts Aggregation",
        taskDescription = s"You need to generate the question, answer, and evidence messages for a test where multiple assistant statements must be combined or synthesized.",
        specificInstructions = "requires combining or synthesizing multiple assistant statements",
        fieldDefinitions = s"""question: The question that requires combining information from all $evidenceCount assistant statements.
answer: The correct answer that synthesizes information from multiple assistant statements.
message_evidences: An array of exactly $evidenceCount message objects containing the assistant's statements to be combined.""",
        additionalGuidance = Some("""Focus on questions that require combining, comparing, or reasoning across multiple assistant statements.

IMPORTANT: Evidence messages should be written as if they are being spoken in real-time during a conversation. Use present tense or conversational language (e.g., "I recommend...", "You should try...", "The best approach is...") rather than past tense (avoid "I recommended...", "I suggested..."). The messages should feel natural and immediate, as if the assistant is speaking them right now.

AVOID: Simply copying the answer text verbatim into the message. The message should feel like natural conversation, not a robotic repetition of the answer. Add conversational context, transitions, or framing to make it feel authentic.""")
      )
    }
  }

  /**
   * Get conversation prompt parts for assistant facts scenarios.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    if (evidenceCount == 1) {
      ConversationPromptParts(
        evidenceType = "assistant fact recall",
        scenarioDescription = "This tests the AI's ability to recall a specific statement, recommendation, or piece of advice that it previously gave to the user. Unlike user-fact scenarios where the AI recalls what others said, this tests self-awareness and memory of the AI's own contributions to conversations. This is uniquely challenging because it requires the AI to maintain a consistent identity and accurately remember its own past statements. The AI must demonstrate autobiographical memory - the ability to remember not just what was discussed, but specifically what role it played in those discussions. This tests whether the AI can maintain coherent self-knowledge and recall its own previous behavior.",
        useCaseScenario = Some(useCase.scenario_description),
        evidenceMessages = evidenceCore.message_evidences,
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        evidenceCount = evidenceCount
      )
    } else {
      ConversationPromptParts(
        evidenceType = "assistant facts aggregation",
        scenarioDescription = "This tests the AI's ability to combine and synthesize multiple statements, recommendations, and advice that it previously gave to the user. Unlike single-fact recall or user-fact scenarios, this requires the AI to remember multiple of its own contributions and reason across them. This is particularly challenging because it requires the AI to maintain a consistent identity across interactions while accurately remembering and integrating its own past behavior, recommendations, and commitments. The AI must demonstrate advanced autobiographical memory - not just remembering individual statements, but understanding relationships between its own contributions and synthesizing them coherently.",
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
   * Override to expect Assistant messages for this evidence type.
   */
  override def getExpectedSpeaker(): String = "Assistant"

  /**
   * Get implementation-specific prompt parts for use case summary generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    if (evidenceCount == 1) {
      // Single assistant fact recall scenarios
      val evidenceDescription = "In each test, the assistant provides one specific recommendation, statement, or piece of advice that the user later needs to recall."
      
      val coreTaskDescription = s"""These scenarios specifically test "assistant fact recall" - situations where the AI must remember and accurately retrieve a specific statement it previously made. This tests self-awareness and autobiographical memory of the AI's own contributions.

Key Pattern: Assistant Statement → User Recall Question
Each scenario follows this pattern:
1. Assistant makes a specific statement, recommendation, or gives advice
2. User later asks to recall what the assistant said
3. The correct answer is exactly what the assistant stated"""

      val exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Professional Life",
        scenario_description = "The project manager seeks advice from the AI about team collaboration tools for remote work. The AI recommends a specific tool with detailed reasoning. Later, when setting up the team workspace, the manager asks 'What collaboration tool did you recommend for our remote team and why?' The scenario tests whether the AI can accurately recall its own previous recommendation and reasoning."
      )

      UseCaseSummaryPromptParts(
        evidenceTypeDescription = "an AI's ability to recall its own previous statements",
        coreTaskDescription = coreTaskDescription,
        evidenceDistributionDescription = evidenceDescription,
        exampleUseCase = exampleUseCase,
        additionalRequirements = Some("Types of Assistant Statements: Include various types of assistant statements such as:\n- Specific recommendations (restaurants, books, tools, approaches)\n- Advice (strategies, solutions, best practices)\n- Factual statements (information, explanations, definitions)\n- Opinions (preferences, assessments, evaluations)\n- Instructions (steps, procedures, guidelines)")
      )
    } else {
      // Multi-assistant fact aggregation scenarios
      val evidenceDescription = evidenceCount match {
        case 2 => "In each test, the assistant provides two different recommendations, statements, or pieces of advice that the user later needs to combine or compare."
        case 3 => "In each test, the assistant provides three different recommendations, statements, or pieces of advice that the user later needs to synthesize."
        case n => s"In each test, the assistant provides $n different recommendations, statements, or pieces of advice that the user later needs to combine."
      }
      
      val coreTaskDescription = s"""These scenarios specifically test "assistant facts aggregation" - situations where the user asks the AI to combine or synthesize multiple statements the assistant previously made. The AI must remember multiple of its own statements and reason across them.

Key Pattern: Multiple Assistant Statements → User Aggregation Question
Each scenario follows this pattern:
1. Assistant makes multiple statements, recommendations, or gives advice across interactions
2. User later asks a question requiring combination or synthesis of assistant statements
3. The correct answer combines or compares what the assistant stated"""

      val exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Professional Life",
        scenario_description = s"The project manager seeks advice from the AI about improving team productivity. Over several conversations, the AI recommends different tools for task management, communication, and documentation. Later, when preparing a team improvement plan, the manager asks 'What were all the productivity tools you recommended and how do they work together?' The scenario tests whether the AI can combine and synthesize its multiple recommendations into a cohesive answer."
      )

      UseCaseSummaryPromptParts(
        evidenceTypeDescription = "an AI's ability to combine and synthesize its own previous statements",
        coreTaskDescription = coreTaskDescription,
        evidenceDistributionDescription = evidenceDescription,
        exampleUseCase = exampleUseCase,
        additionalRequirements = Some("Types of Assistant Statement Combinations: Include various combinations such as:\n- Comparing multiple recommendations\n- Synthesizing advice across different areas\n- Aggregating factual information provided\n- Connecting related opinions or assessments\n- Combining instructions or procedures")
      )
    }
  }

}