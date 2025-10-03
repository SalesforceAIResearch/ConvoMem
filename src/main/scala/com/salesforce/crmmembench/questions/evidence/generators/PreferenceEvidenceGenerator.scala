package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation.{UseCaseSummaryPromptParts, EvidenceCorePromptParts, AnsweringEvaluation, RubricBasedAnsweringEvaluation}
import com.salesforce.crmmembench.{Config, Personas}

/**
 * Generator for preference evidence conversations.
 * Tests whether a model can remember user preferences and apply them when making recommendations.
 * Unlike factual recall where answers are specific facts, preference questions evaluate if the model
 * can provide appropriate responses based on remembered preferences.
 * 
 * Key Differences from Fact-Based Evidence:
 * - Answer is a rubric describing appropriate responses, not a specific fact
 * - Tests application of preferences rather than pure recall
 * - Focuses on personalization and recommendation scenarios
 * 
 * QUALITY ASSURANCE: This generator includes immediate verification of generated evidence.
 * Each evidence item is automatically verified using the same logic as the verification
 * system. If verification fails, the generator retries with a new generation attempt.
 */
class PreferenceEvidenceGenerator(evidenceCount: Int) extends EvidenceGenerator {
  
  override val config: EvidenceConfig = PreferenceEvidenceConfig(evidenceCount)

  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String = "Preferences"
  
  /**
   * Use rubric-based evaluation since preference answers are evaluation criteria.
   */
  override def getAnsweringEvaluation(): AnsweringEvaluation = {
    RubricBasedAnsweringEvaluation
  }

  /**
   * Get implementation-specific prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
    if (evidenceCount == 1) {
      // Single preference scenario
      EvidenceCorePromptParts(
        scenarioType = "User Preference Application",
        taskDescription = "You need to generate the question, answer (as a rubric), and evidence message for a test where the user asks for a recommendation that should be influenced by their previously expressed preference.",
        specificInstructions = "tests the AI's ability to remember and apply a user preference when making recommendations",
        fieldDefinitions = s"""question: A natural, standalone question asking for recommendations, suggestions, or advice. The question must NOT reference prior conversations, memory, or "my experience/preferences". It should be phrased as if asking for general advice (e.g., "What CRM would you recommend for a small subsidiary?" NOT "Given my experience with Salesforce, what CRM...").
answer: A rubric describing what kind of response would be appropriate based on the user's preference. This should describe what the AI should prefer/suggest and what it should NOT prefer/suggest.
message_evidences: An array of exactly 1 message object containing the user's preference statement.""",
        additionalGuidance = Some("""CRITICAL: The answer must be a RUBRIC, not a specific factual answer. 

CRITICAL QUESTION REQUIREMENTS:
The question must be NATURAL and STANDALONE. DO NOT:
- Reference prior conversations ("As I mentioned before...")
- Reference memory ("Given what you know about me...")
- Reference preferences explicitly ("Based on my preferences...")
- Use phrases like "my experience", "my background", "what I've told you"

GOOD question examples:
- "What CRM would you recommend for a small subsidiary?"
- "Which project management tools work best for remote teams?"
- "What programming languages are good for data science?"

BAD question examples:
- "Given my extensive experience with Salesforce, what CRM..."
- "Based on what I've told you about my preferences..."
- "Considering my background in React development..."

IMPORTANT: The rubric must be CONCRETE and VERIFIABLE. It should specify:
1. Exact types of recommendations that align with the preference (be specific about names, features, characteristics)
2. Clear criteria that a judge can check (e.g., "must mention React-based tools" not just "frontend tools")
3. Specific examples of what should and should not be recommended

Example of a GOOD, verifiable rubric:
"The user would prefer responses that recommend React-based tools and libraries (such as Next.js, Gatsby, Create React App, or React Native). The response should specifically mention React ecosystem tools and emphasize component-based architecture. They would NOT prefer recommendations for Vue.js, Angular, or vanilla JavaScript solutions."

Example of a BAD, vague rubric:
"The user would prefer modern frontend frameworks with good developer experience."

The preference should be expressed with SPECIFIC details:
- "I've been really enjoying React's component-based approach and hooks API in my frontend projects"
- "I do my morning runs at 6 AM before work - it's become essential for my productivity"
- "Adobe Premiere Pro's timeline editing and color grading tools have transformed my video workflow"

Make the preferences CONCRETE with specific tools, times, features, or measurable characteristics that can be verified in recommendations.""")
      )
    } else {
      // Multi-preference scenario
      EvidenceCorePromptParts(
        scenarioType = "Multiple User Preferences Application",
        taskDescription = s"You need to generate the question, answer (as a rubric), and evidence messages for a test where the user asks for recommendations that should be influenced by multiple previously expressed preferences.",
        specificInstructions = s"tests the AI's ability to combine and apply $evidenceCount user preferences when making recommendations",
        fieldDefinitions = s"""question: A natural, standalone question asking for recommendations, suggestions, or advice. The question must NOT reference prior conversations, memory, or "my experience/preferences". It should be phrased as if asking for general advice without mentioning any context (e.g., "What tools would you recommend for building a web app?" NOT "Based on what I've told you, what...").
answer: A rubric describing what kind of response would be appropriate based on ALL the user's preferences combined. This should describe a holistic recommendation that considers all preferences.
message_evidences: An array of exactly $evidenceCount message objects, each containing a different user preference.""",
        additionalGuidance = Some(s"""CRITICAL: The answer must be a RUBRIC that considers ALL $evidenceCount preferences holistically.

CRITICAL QUESTION REQUIREMENTS:
The question must be NATURAL and STANDALONE. DO NOT:
- Reference prior conversations or context
- Reference memory or what the AI knows about the user
- Reference preferences explicitly in the question
- Use phrases like "my experience", "my background", "based on what I've told you"

GOOD question examples:
- "What full-stack framework would you recommend for a new startup?"
- "Which productivity tools work well together for remote teams?"
- "What resources are best for learning machine learning?"

BAD question examples:
- "Given my preferences for React and Python, what should I use?"
- "Based on everything I've shared, what tools do you recommend?"
- "Considering my background and requirements, which framework..."

IMPORTANT: The rubric must be CONCRETE and VERIFIABLE with specific criteria:
1. List exact products/services/approaches that satisfy ALL preferences
2. Specify measurable features that align with each preference
3. Give concrete examples of what should and should NOT be recommended

Example of a GOOD, verifiable multi-preference rubric:
"Based on their preferences for React (frontend), Python (backend), and Docker (deployment), the user would prefer responses recommending:
- Full-stack frameworks like FastAPI (Python) with React frontend
- Containerized deployment solutions using Docker Compose
- Specific tools: Create React App + FastAPI + Docker setup guides
They would NOT prefer: PHP/Laravel stacks, monolithic Java applications, or serverless-only architectures."

Example of a BAD, vague rubric:
"The user would prefer modern, scalable solutions that are easy to deploy."

Each preference should include SPECIFIC, verifiable details that can be checked in recommendations.""")
      )
    }
  }

  /**
   * Get conversation prompt parts for preference scenarios.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
    if (evidenceCount == 1) {
      ConversationPromptParts(
        evidenceType = "user preference application",
        scenarioDescription = "This tests the AI's ability to remember user preferences and apply them when making personalized recommendations or suggestions. Unlike factual recall where the answer is a specific piece of information, preference questions evaluate whether the AI can provide appropriate responses based on remembered user preferences. The AI must demonstrate that it can use stored preference information to personalize its recommendations, showing true understanding of user needs rather than just memory recall. This is essential for providing personalized assistance that aligns with user preferences.",
        useCaseScenario = Some(useCase.scenario_description),
        evidenceMessages = evidenceCore.message_evidences,
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        evidenceCount = evidenceCount
      )
    } else {
      ConversationPromptParts(
        evidenceType = "multiple user preferences application",
        scenarioDescription = s"This tests the AI's ability to combine and apply multiple user preferences when making recommendations. The AI must synthesize $evidenceCount different preferences to provide holistic recommendations that consider all expressed preferences. This is cognitively complex because it requires not just remembering multiple preferences but understanding how they interact and influence recommendations together. The AI must demonstrate sophisticated preference modeling - balancing potentially competing preferences and finding recommendations that satisfy multiple criteria simultaneously.",
        useCaseScenario = Some(useCase.scenario_description),
        evidenceMessages = evidenceCore.message_evidences,
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        evidenceCount = evidenceCount
      )
    }
  }

  /**
   * Get implementation-specific prompt parts for use case summary generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
    if (evidenceCount == 1) {
      // Single preference scenarios
      val evidenceDistribution = "In each test, the user expresses one preference or personalized information naturally in conversation."
      
      val coreTaskDescription = s"""These scenarios specifically test "user preference application" - situations where the AI must remember user preferences and apply them when making recommendations or providing personalized responses. Unlike factual recall, this tests the AI's ability to personalize its assistance based on remembered preferences.

Key Pattern: User Expresses Preference → User Asks for Recommendation
Each scenario follows this pattern:
1. User naturally expresses a preference, taste, or personal inclination in conversation
2. User later asks for recommendations, suggestions, or advice in a related area
3. The correct response (rubric) describes how the AI should personalize its recommendation based on the preference

CRITICAL: The answer is NOT a specific fact but a RUBRIC describing appropriate personalized responses."""

      val exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Professional Life", 
        scenario_description = "The software developer mentions really enjoying the reactive programming paradigm while discussing their current React project. Later, when exploring backend options, they ask 'What backend frameworks would you recommend for my new microservice?' The AI should recommend frameworks that align with reactive programming principles, like Spring WebFlux or Vert.x, showing it remembered and applied the developer's preference."
      )

      UseCaseSummaryPromptParts(
        evidenceTypeDescription = "an AI's ability to remember and apply user preferences in recommendations",
        coreTaskDescription = coreTaskDescription,
        evidenceDistributionDescription = evidenceDistribution,
        exampleUseCase = exampleUseCase,
        additionalRequirements = Some("""Types of Preferences to Include (BE SPECIFIC):
- Technology preferences: Name exact tools, frameworks, versions (e.g., "React 18 with TypeScript", not just "modern JavaScript")
- Learning preferences: Specific platforms, formats, instructors (e.g., "Udemy video courses by Stephen Grider", not just "video learning")
- Lifestyle preferences: Exact times, locations, durations (e.g., "6 AM runs for 45 minutes", not just "morning exercise")
- Work preferences: Specific tools and workflows (e.g., "Slack for async, Zoom for sync meetings", not just "remote work")
- Entertainment preferences: Specific titles, creators, platforms (e.g., "Christopher Nolan films on IMAX", not just "movies")

CRITICAL: Each preference must include CONCRETE details that can be verified in recommendations:
- Specific product/tool names
- Measurable characteristics (times, sizes, versions)
- Named features or capabilities
- Clear inclusion/exclusion criteria""")
      )
    } else {
      // Multi-preference scenarios
      val evidenceDistribution = evidenceCount match {
        case 2 => "In each test, the user expresses two related preferences at different times."
        case 3 => "In each test, the user expresses three related preferences at different times."
        case n => s"In each test, the user expresses $n related preferences at different times."
      }
      
      val coreTaskDescription = s"""These scenarios test an AI's ability to combine and apply multiple user preferences across $evidenceCount distinct interactions. Each scenario will form the basis for a "$evidenceCount-preference application" test. $evidenceDistribution Later, the user asks for recommendations that should be influenced by ALL their expressed preferences.

The AI must synthesize multiple preferences to provide holistic, personalized recommendations that consider all preferences together."""

      val exampleUseCase = EvidenceUseCase(
        id = 1,
        category = "Professional Life",
        scenario_description = s"The software developer mentions preferring visual learning tools in one conversation, expresses love for Python's simplicity in another, and shares their preference for self-paced learning in a third. When they later ask 'Can you recommend resources for learning machine learning?' the AI should suggest visual, Python-based, self-paced courses or tutorials that combine all three preferences."
      )

      UseCaseSummaryPromptParts(
        evidenceTypeDescription = "an AI's ability to combine and apply multiple user preferences",
        coreTaskDescription = coreTaskDescription,
        evidenceDistributionDescription = evidenceDistribution,
        exampleUseCase = exampleUseCase,
        additionalRequirements = Some(s"""Types of Multi-Preference Combinations (BE SPECIFIC AND CONCRETE):
- Technology stack: Exact tools that work together (e.g., "Next.js 14 + Prisma ORM + PostgreSQL 15")
- Learning approach: Platform + instructor + schedule (e.g., "Coursera courses by Andrew Ng + evening study 7-9 PM")
- Lifestyle: Specific activities + times + tools (e.g., "Peloton cycling at 6 AM + MyFitnessPal tracking + 16:8 intermittent fasting")
- Work environment: Tools + hours + communication (e.g., "VS Code + Pomodoro timer + Slack threads for async")
- Entertainment: Platform + genre + specific preferences (e.g., "Netflix true crime documentaries under 60 minutes")

CRITICAL for verifiability:
- Each preference must name SPECIFIC products, tools, or services
- Include measurable criteria (times, versions, durations)
- Recommendations must be checkable against concrete criteria
- The $evidenceCount preferences should create clear inclusion/exclusion rules""")
      )
    }
  }

}