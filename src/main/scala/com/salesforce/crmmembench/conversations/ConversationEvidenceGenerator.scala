package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._


object GenerateConversationEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating irrelevant conversations...")
    val generator = new ConversationEvidenceGenerator()
    generator.generateEvidence()
  }
}

/**
 * Generator for general conversation use cases.
 * Unlike other evidence generators, this focuses on creating diverse conversation scenarios
 * that don't necessarily follow the question-answer-evidence pattern.
 */

class ConversationEvidenceGenerator extends EvidenceGenerator {
  
  override val config: EvidenceConfig = ConversationEvidenceConfig()
  
  /**
   * Override the number of use cases per person for conversation generation.
   * We need many more conversations to create a diverse dataset.
   */
  override lazy val useCasesPerPersonConfig: Int = 400
  
  /**
   * Override to use Gemini Pro for conversation generation.
   * Conversations need higher quality generation due to their complexity.
   */
  override val llmModel: LLMModel = Gemini.proJson
  
  /**
   * No verification for conversation generation.
   * Irrelevant conversations don't contain facts to verify.
   */
  override def getVerificationChecks(): List[VerificationCheck] = {
    VerificationConstants.NO_VERIFICATION // No verification needed
  }
  
  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "Conversation"
  
  /**
   * Get implementation-specific prompt parts for use case summary generation.
   * This is the main method we're implementing for generating conversation use cases.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    val evidenceDistribution = s"Each conversation scenario should be a natural, engaging interaction between ${person.role_name} and an AI assistant."
    
    val coreTaskDescription = s"""These scenarios are designed to create diverse, realistic conversations that a ${person.role_name} might have with an AI assistant. Unlike evidence-based conversations, these should cover a wide range of topics and interaction styles.

Key Patterns to Include:
1. Problem-solving discussions where the ${person.role_name} seeks help with professional challenges
2. Casual conversations about hobbies, interests, or daily life
3. Planning and brainstorming sessions for projects or goals  
4. Learning conversations where the ${person.role_name} asks about new topics
5. Reflection or venting conversations about work or life situations
6. Creative discussions about ideas or innovations
7. Technical support or troubleshooting conversations
8. Decision-making conversations where the ${person.role_name} weighs options

Each scenario should:
- Feel natural and authentic to the ${person.role_name}'s background and interests
- Have a clear conversational purpose or goal
- Allow for meaningful back-and-forth dialogue
- Cover diverse emotional tones (professional, casual, frustrated, excited, curious, etc.)
- Represent different times of day, contexts, and urgency levels"""
    
    val exampleUseCase = EvidenceUseCase(
      id = 1,
      category = "Professional Development",
      scenario_description = s"The ${person.role_name} is preparing for an important presentation next week and wants to brainstorm with the AI about effective presentation techniques, dealing with nervousness, and structuring their content for maximum impact. They're feeling both excited and anxious about the opportunity."
    )
    
    UseCaseSummaryPromptParts(
      evidenceTypeDescription = "diverse, natural conversations between a professional and an AI assistant",
      coreTaskDescription = coreTaskDescription,
      evidenceDistributionDescription = evidenceDistribution,
      exampleUseCase = exampleUseCase,
      additionalRequirements = Some(s"""Conversation Diversity Requirements:
- Mix professional and personal topics (70% professional, 30% personal as a guideline)
- Include various conversation lengths (some quick exchanges, some deep discussions)
- Cover different emotional states and urgency levels
- Include both goal-oriented and exploratory conversations
- Ensure scenarios are specific to ${person.role_name}'s domain and interests
- Avoid repetitive patterns or overly similar scenarios
- Make scenarios rich enough to generate 15-20 message exchanges""")
    )
  }
  
  /**
   * Get implementation-specific prompt parts for evidence core generation.
   * For conversations, we adapt this to generate conversation starters instead of Q&A pairs.
   * Since conversations don't have evidence to recall, we generate opening messages that kickstart the conversation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    EvidenceCorePromptParts(
      scenarioType = "Conversation Starter",
      taskDescription = s"You need to generate conversation starter messages that will naturally lead to the type of conversation described in the scenario. Unlike evidence-based conversations, these are general conversations without specific facts to recall.",
      specificInstructions = s"creates natural conversation starters for a ${person.role_name}",
      fieldDefinitions = s"""question: A natural conversation starter or initial query from the ${person.role_name} that fits the scenario. This should feel like a genuine way they would begin this type of conversation.
answer: A brief description of the expected conversation flow and key topics that should be covered. This is NOT a specific answer but rather guidance for how the conversation should develop.
message_evidences: An array with exactly 1 message object containing the opening message from the User that starts the conversation.""",
      additionalGuidance = Some(s"""CRITICAL: For conversation generation, the fields have different meanings:
- The "question" is the conversation starter (what the ${person.role_name} would naturally say to begin)
- The "answer" is a META description of the conversation flow (not an actual answer)
- The "message_evidences" contains the opening message that kicks off the conversation

Example:
- question: "Hey, I've been struggling with my presentation for next week. Do you have time to help me brainstorm?"
- answer: "A conversation about presentation preparation including structure, dealing with nerves, audience engagement techniques, and practicing delivery. The conversation should be supportive and practical."
- message_evidences: [{"speaker": "User", "text": "Hey, I've been struggling with my presentation for next week. Do you have time to help me brainstorm?"}]

The opening message should:
- Feel natural and authentic to the ${person.role_name}'s communication style
- Clearly establish the conversation's context and purpose
- Invite meaningful dialogue
- Match the emotional tone described in the scenario""")
    )
  }
  
  /**
   * Get conversation prompt parts for the scenario-specific conversation generation.
   * For irrelevant conversations, we adapt this to generate natural conversations without evidence.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    ConversationPromptParts(
      evidenceType = "general conversation",
      scenarioDescription = s"""This is a natural conversation between ${person.role_name} and an AI assistant. 
Unlike other evidence types, this conversation doesn't contain specific facts to recall later. 
Instead, it should be a realistic, helpful interaction about the topic: ${evidenceCore.answer}
The conversation should feel authentic and provide genuine value to the ${person.role_name}.""",
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = 1
    )
  }
}