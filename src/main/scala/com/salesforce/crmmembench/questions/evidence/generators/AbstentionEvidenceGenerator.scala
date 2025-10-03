package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation.{UseCaseSummaryPromptParts, EvidenceCorePromptParts, VerificationCheck, AnsweringEvaluation, AbstentionAnsweringEvaluation}
import com.salesforce.crmmembench.{Config, Personas}

/**
 * Generator for abstention evidence conversations where no answer is available in context.
 * 
 * This generator creates scenarios where:
 * - Evidence is related to the question but doesn't actually answer it
 * - The correct answer is "There is no information in prior conversations to answer this question"
 * - The model should learn to say "I don't know" when information is insufficient
 * 
 * Examples:
 * - Question: "What's John's phone number?" 
 * - Evidence: "John works in marketing", "John joined last month"
 * - Answer: "There is no information in prior conversations to answer this question"
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
class AbstentionEvidenceGenerator(evidenceCount: Int) 
  extends EvidenceGenerator {
  
  override val config: EvidenceConfig = AbstentionEvidenceConfig(evidenceCount)
  
  /**
   * Override to use the specialized AbstentionAnsweringEvaluation for judging responses.
   * This evaluator properly recognizes abstention/refusal as correct behavior.
   */
  override def getAnsweringEvaluation(): AnsweringEvaluation = AbstentionAnsweringEvaluation


  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "Abstention"

  /**
   * Get implementation-specific prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    EvidenceCorePromptParts(
      scenarioType = "Abstention",
      taskDescription = "You need to generate the question, answer, and evidence messages for a test where insufficient information is provided.",
      specificInstructions = "cannot be answered from the provided information",
      fieldDefinitions = s"""question: The question that cannot be answered from the provided information.
answer: Must be exactly "There is no information in prior conversations to answer this question".
message_evidences: An array of exactly $evidenceCount message objects containing related but insufficient information.""",
      additionalGuidance = Some("The evidence should be realistic for the person's background and the scenario described, but should deliberately lack the specific information needed to answer the question.")
    )
  }

  /**
   * Get conversation prompt parts for abstention scenarios.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    ConversationPromptParts(
      evidenceType = "abstention",
      scenarioDescription = "This tests the AI's ability to recognize when sufficient information is NOT available to answer a question and should respond with 'I don't know' rather than hallucinating. Unlike scenarios where the AI has access to complete information, abstention tests present related but insufficient evidence that might tempt the AI to make educated guesses or fabricate answers. This is cognitively demanding because it requires the AI to perform meta-reasoning about its own knowledge state - assessing what it knows versus what it needs to know. The AI must resist the pressure to provide an answer when doing so would require speculation, demonstrating intellectual humility and proper uncertainty handling.",
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = evidenceCount
    )
  }

  /**
   * Override verification checks for abstention evidence.
   * Only verify with evidence - without evidence check doesn't make sense for abstention.
   */
  override def getVerificationChecks(): List[VerificationCheck] = {
    import com.salesforce.crmmembench.questions.evidence.generation.VerifyWithEvidence
    List(new VerifyWithEvidence())
  }

  /**
   * Get implementation-specific prompt parts for use case summary generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    val evidenceDescription = evidenceCount match {
      case 1 => "one piece of related but insufficient information"
      case 2 => "two pieces of related but insufficient information"
      case 3 => "three pieces of related but insufficient information"
      case 4 => "four pieces of related but insufficient information"
      case n => s"$n pieces of related but insufficient information"
    }
    
    val evidenceDistribution = evidenceCount match {
      case 1 => "In each test, the user provides one piece of information that is related to the question but does not contain the answer."
      case 2 => "In each test, the user provides two pieces of information that are related to the question but neither contains the answer."
      case 3 => "In each test, the user provides three pieces of information that are related to the question but none contains the answer."
      case 4 => "In each test, the user provides four pieces of information that are related to the question but none contains the answer."
      case n => s"In each test, the user provides $n pieces of information that are related to the question but none contains the answer."
    }
    
    val coreTaskDescription = s"""These scenarios specifically test "abstention" - situations where the AI should say "I don't know" or acknowledge that the information is insufficient. This is a critical capability to prevent AI from hallucinating or making up answers.

Key Pattern: Related Information → Insufficient for Answer → "I Don't Know"
Each scenario follows this pattern:
1. User provides $evidenceDescription that is topically related to the eventual question
2. The evidence creates a realistic context but deliberately omits the specific answer
3. User asks a question that requires information NOT provided in the evidence
4. The correct answer is "There is no information in prior conversations to answer this question"

Evidence Quality: The evidence should be:
- Topically related to the question (same general subject area)
- Realistic and plausible for the persona
- Genuinely insufficient to answer the specific question
- Not containing any hints or clues that could lead to the answer"""

    val additionalRequirements = s"""Types of Missing Information: Include various types of missing critical details such as:
- Specific numbers (phone numbers, amounts, dates, quantities)
- Names of people not mentioned 
- Locations not specified
- Times/schedules not provided
- Technical details omitted
- Specific preferences not shared
- Private information not disclosed"""

    val exampleUseCase = EvidenceUseCase(
      id = 1,
      category = "Professional Life",
      scenario_description = s"The project manager discusses the Project Phoenix team structure and mentions having frontend and backend developers, but never specifies their names or contact information. Later, when needing to schedule a code review, they ask 'What's the email address of our lead frontend developer on Project Phoenix?' The AI should recognize that while it knows about the team structure, specific contact details were never provided."
    )

    UseCaseSummaryPromptParts(
      evidenceTypeDescription = "an AI's ability to recognize when sufficient information is NOT available to answer a question",
      coreTaskDescription = coreTaskDescription,
      evidenceDistributionDescription = evidenceDistribution,
      exampleUseCase = exampleUseCase,
      additionalRequirements = Some(additionalRequirements)
    )
  }

}