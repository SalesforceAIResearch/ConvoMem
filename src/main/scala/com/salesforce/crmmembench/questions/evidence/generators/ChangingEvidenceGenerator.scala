package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation.{UseCaseSummaryPromptParts, EvidenceCorePromptParts, VerificationCheck, VerifyWithEvidence, VerifyWithoutEvidence, VerifyWithPartialEvidenceForChanging, VerifyIntermediateEvidenceAddresses}
import com.salesforce.crmmembench.{Config, Personas}

/**
 * Generator for changing evidence conversations where facts evolve over time.
 * Examples: "I'm going to Paris" -> "My trip got cancelled" -> Question: "What travel plans do I have?" -> "No travel plans"
 * 
 * Takes evidenceCount as parameter, changeCount is automatically calculated as evidenceCount - 1.
 * For example, with evidenceCount=2, you get initial state + 1 change = 2 evidence messages.
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
class ChangingEvidenceGenerator(evidenceCount: Int) 
  extends EvidenceGenerator {
  
  override val config: EvidenceConfig = ChangingEvidenceConfig(evidenceCount, evidenceCount - 1)

  // Derived property: changeCount is always evidenceCount - 1
  val changeCount = evidenceCount - 1

  /**
   * Override verification checks to include the special partial evidence check for changing evidence.
   * This ensures that the latest update is necessary to answer the question correctly.
   * Also includes a check to ensure intermediate evidence messages address the question.
   */
  override def getVerificationChecks(): List[VerificationCheck] = {
    super.getVerificationChecks() ++ List(
      new VerifyWithPartialEvidenceForChanging(),
      new VerifyIntermediateEvidenceAddresses()
    )
  }

  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "Changing"

  /**
   * Get implementation-specific prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    EvidenceCorePromptParts(
      scenarioType = "Changing Evidence",
      taskDescription = "You need to generate the question, answer, and evidence messages for a test where information changes over time.",
      specificInstructions = "naturally flows from the scenario context",
      fieldDefinitions = s"""question: A natural question the user would ask about the topic discussed in the evidence.
answer: The correct answer based on the information provided in all evidence messages.
message_evidences: An array of exactly $evidenceCount message objects that contain the information needed to answer the question.""",
      additionalGuidance = Some("""CRITICAL - Avoid Revealing Changes in Questions:
- Questions MUST NOT use words like "current", "now", "latest", "updated", "new", "still", or any other temporal indicators
- Questions MUST NOT reference that something has changed or been updated
- Questions should be phrased neutrally, as if asking for information without knowing it has changed
- BAD examples: "What is the current launch date?", "What time is the meeting now?", "What's the latest budget?"
- GOOD examples: "What is the launch date?", "What time is the meeting?", "What's the budget?"

The evidence messages should tell a coherent story where information naturally evolves, but the question must not reveal this evolution has occurred.""")
    )
  }

  /**
   * Get conversation prompt parts for changing evidence scenarios.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    ConversationPromptParts(
      evidenceType = "changing evidence",
      scenarioDescription = "This tests the AI's ability to track how information changes over time, with evidence showing temporal progression from initial state through changes to final state. Unlike static memory tests where facts remain constant, changing evidence scenarios require the AI to maintain temporal awareness and update its understanding as new information contradicts or modifies previous statements. This is particularly challenging because it tests not just memory retention, but also memory updating - the ability to override outdated information with newer, conflicting facts. The AI must demonstrate temporal reasoning to understand which information is most current and relevant when answering questions.",
      useCaseScenario = Some(useCase.scenario_description),
      evidenceMessages = evidenceCore.message_evidences,
      question = evidenceCore.question,
      answer = evidenceCore.answer,
      evidenceCount = evidenceCount
    )
  }


  /**
   * Get implementation-specific prompt parts for use case summary generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    
    val changeDescription = changeCount match {
      case 1 => "one change occurs"
      case 2 => "two changes occur"
      case 3 => "three changes occur"
      case n => s"$n changes occur"
    }
    
    val evidenceDistribution = evidenceCount match {
      case 2 => "In each test, the user provides information about something that later changes. The user shares the initial state in one conversation, then shares the updated state in a second conversation."
      case 3 => "In each test, the user provides information about something that changes twice. The user shares the initial state in one conversation, an intermediate update in a second conversation, and the final state in a third conversation."
      case 4 => "In each test, the user provides information about something that changes three times. The user shares the initial state, then provides three subsequent updates across four separate conversations."
      case n => s"In each test, the user provides information about something that changes $changeCount times across $n separate conversations."
    }
    
    val coreTaskDescription = s"""These scenarios specifically test "changing evidence" - situations where facts evolve, get updated, cancelled, or modified. The AI must remember not just the latest state, but understand that previous information is no longer valid.

Key Pattern: Initial State → Change(s) → Final Question
Each scenario follows this pattern:
1. User establishes an initial fact/plan/state
2. $changeDescription that modify or override the initial state
3. User asks a neutral question without revealing they know something has changed
4. The correct answer reflects the final state after all changes

CRITICAL: Questions must NEVER use temporal words like "current", "now", "latest", "updated", "new", "still" or any phrases that reveal the information has changed."""

    val additionalRequirements = s"""Types of Changes: Include various types of changes such as:
- Cancellations (trip cancelled, meeting cancelled, order cancelled)
- Rescheduling (moved to different time/date/location)
- Replacements (changed to different option/person/item)
- Updates (new information overwrites old)
- Status changes (approved → rejected, planned → completed)"""

    val exampleUseCase = EvidenceUseCase(
      id = 1,
      category = "Professional Life",
      scenario_description = s"The project manager is working on Project Phoenix and initially schedules a client presentation for Tuesday at 2 PM in Conference Room A. Due to scheduling conflicts, this gets moved to Thursday at 10 AM, then finally changed to Friday at 3 PM in the Virtual Meeting Room. When preparing the final agenda, they ask 'When and where is the Project Phoenix client presentation scheduled?' The AI must provide the final information (Friday 3 PM, Virtual Meeting Room) based on all the evidence provided."
    )

    UseCaseSummaryPromptParts(
      evidenceTypeDescription = "an AI's ability to track information that changes over time",
      coreTaskDescription = coreTaskDescription,
      evidenceDistributionDescription = evidenceDistribution,
      exampleUseCase = exampleUseCase,
      additionalRequirements = Some(additionalRequirements)
    )
  }

}