package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.{Config, Personas}

/**
 * Evidence generator for cases where the connection between question and evidence is implicit.
 * 
 * Example: User mentions they broke their leg, later asks "what should I do this weekend?"
 * The model needs to infer that physical activities are inappropriate.
 * 
 * The answer is a rubric describing what makes a good response, not a specific answer.
 */
class ImplicitConnectionEvidenceGenerator(evidenceCount: Int) 
  extends EvidenceGenerator {
  
  override val config: EvidenceConfig = ImplicitConnectionEvidenceConfig(evidenceCount)

  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "implicit_connection"

  // Override verification to use 1 pass for withEvidence check
  override def getVerificationChecks(): List[VerificationCheck] = {
    List(
      new VerifyWithEvidence(requiredPasses = 1),  // Only 1 pass needed for rubric-based answers
      new VerifyWithoutEvidence()
    )
  }
  
  /**
   * Use rubric-based evaluation since answers are evaluation criteria, not specific facts.
   */
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    RubricBasedAnsweringEvaluation
  }

  /**
   * Get prompt parts for use case generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    UseCaseSummaryPromptParts(
      evidenceTypeDescription = "implicit connection scenarios where context influences future recommendations",
      
      coreTaskDescription = """This tests the AI's ability to make non-obvious connections between earlier context and later questions.
The user will share important context (health issues, life changes, constraints) that should implicitly influence future recommendations,
even when the later question doesn't reference this context.""",
      
      evidenceDistributionDescription = if (evidenceCount == 1) {
        "a single piece of important context is shared that should influence later recommendations"
      } else {
        s"$evidenceCount different pieces of context are shared across conversations that should all influence later recommendations"
      },
      
      exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Health Constraints",
        scenario_description = s"${person.role_name} casually mentions having a knee surgery scheduled next month during a discussion about quarterly planning. Weeks later, they ask for ideas for the company retreat without mentioning their surgery. The AI should suggest activities that don't require physical exertion."
      ),
      
      additionalRequirements = Some(
        s"""Focus on scenarios where:
        - The context is shared naturally, not as a major announcement
        - There's a realistic time gap between sharing context and asking the question
        - The connection requires inference and understanding implications
        - Forgetting the context would lead to inappropriate suggestions
        - The scenarios are realistic for ${person.role_name}'s professional and personal life
        
        Categories to consider:
        - Health/Physical Constraints (injuries, conditions, pregnancies)
        - Life Changes (new baby, moving, family situations)
        - Financial Constraints (budget limits, job changes)
        - Dietary/Religious Restrictions
        - Fears/Phobias (heights, flying, crowds)
        - Time Constraints (other commitments, schedules)
        - Technical Limitations (equipment, skills)"""
      )
    )
  }

  /**
   * Get prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    val specificInstructions = if (evidenceCount == 1) {
      """- The question should be asked naturally without referencing the context
- The answer should be a rubric describing what makes a good response
- The evidence message should contain the important context mentioned casually"""
    } else {
      s"""- The question should require considering ALL $evidenceCount pieces of context
- The answer rubric should address all contextual constraints
- Each evidence message should contain different relevant context"""
    }
    
    EvidenceCorePromptParts(
      scenarioType = "Implicit Connection",
      
      taskDescription = "You need to create a scenario where important context shared earlier should influence how to respond to a later question, even though the question doesn't reference the context.",
      
      specificInstructions = specificInstructions,
      
      fieldDefinitions = """Field Definitions:
- question: A natural question that doesn't reference the earlier context but where that context is relevant
- answer: A RUBRIC (not a direct answer) starting with "A good response should..." that describes how to appropriately respond given the implicit context
- message_evidences: Message(s) containing important context that should influence the response""",
      
      additionalGuidance = Some(s"""Examples of good implicit connections:
- Context: "I broke my leg" → Question: "What should I do this weekend?" → Rubric: Suggest sedentary activities
- Context: "My baby was born last week" → Question: "Any vacation recommendations?" → Rubric: Suggest baby-friendly destinations
- Context: "I'm on a tight budget" → Question: "What gift should I get my team?" → Rubric: Suggest affordable options

For ${person.role_name}, create realistic context and questions that fit their role.

The answer must be a rubric that evaluates whether a response appropriately considers the implicit context.""")
    )
  }

  /**
   * Get conversation prompt parts.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    ConversationPromptParts(
      evidenceType = "implicit connection memory test",
      scenarioDescription = s"""This tests whether the AI can make implicit connections between earlier context and later questions.
        
The user will share important context (${useCase.category}) that should influence future recommendations, even when not explicitly referenced.

Key aspects:
- The context is shared naturally in conversation, not as a major announcement
- Time passes between sharing context and asking the question  
- The question doesn't reference the context
- A good AI response considers the context without being reminded""",
      
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = evidenceCount
    )
  }
}