package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.{Config, Personas}
import com.salesforce.crmmembench.questions.evidence._

/**
 * Case class for implementation-specific prompt parts that get embedded in the base prompt structure.
 * Each implementation provides these parts to create a complete, high-quality prompt.
 */
case class UseCaseSummaryPromptParts(
  evidenceTypeDescription: String,    // e.g., "multi-evidence", "changing evidence", "abstention"
  coreTaskDescription: String,        // Description of what the evidence type tests
  evidenceDistributionDescription: String, // How evidence is distributed across conversations
  exampleUseCase: EvidenceUseCase,    // Example use case for the evidence type
  additionalRequirements: Option[String] = None // Optional additional requirements section
)

/**
 * Case class for implementation-specific evidence core prompt parts.
 * Each implementation provides these parts to create a complete, high-quality evidence core prompt.
 */
case class EvidenceCorePromptParts(
  scenarioType: String,               // e.g., "Multi-Evidence", "Changing Evidence", "Abstention"
  taskDescription: String,            // Description of what needs to be generated for this evidence type
  specificInstructions: String,       // Evidence-type-specific instructions
  fieldDefinitions: String,           // Field definitions specific to this evidence type
  additionalGuidance: Option[String] = None // Optional additional guidance
)

/**
 * Contains all prompt templates and prompt building logic for evidence generation.
 * Extracted from EvidenceGenerator to improve modularity and maintainability.
 */
object EvidencePromptTemplates {
  
  /**
   * Build the complete use case summary prompt from common structure and implementation-specific parts.
   */
  def buildUseCaseSummaryPrompt(person: Personas.Person, parts: UseCaseSummaryPromptParts, useCasesPerPerson: Int = Config.Generation.USE_CASES_PER_PERSON): String = {
    s"""
You are an expert AI assistant specializing in creating complex and personalized benchmark scenarios for evaluating conversational AI memory. Your task is to generate a dataset of ${useCasesPerPerson} high-level scenario descriptions that will be used to test ${parts.evidenceTypeDescription}.

${parts.coreTaskDescription}

Your entire output must be a single, valid JSON object.

Input: User Profile
You will be given the following user profile. All generated scenarios must be plausible and relevant for this specific person.

| Input:
| Category: ${person.category}
| Role: ${person.role_name}
| Role description: ${person.description}
| Background: ${person.background.getOrElse("No background provided")}
|

Core Task
Based on the user profile provided in the Input section, your task is to generate ${useCasesPerPerson} unique scenario descriptions for "${parts.evidenceTypeDescription}" tests. ${parts.evidenceDistributionDescription}

These scenario descriptions will be used later by another AI system to generate the actual evidence, questions, and answers. Your job is to create compelling, realistic scenarios that would make good test cases.

Requirements
Personalization: All ${useCasesPerPerson} scenarios must be deeply reflective of the user's life, profession, and personality as described in their profile. Use the Background, Role, and Description to inspire realistic situations.

Quantity: You must generate exactly ${useCasesPerPerson} use cases.

Diversity & Balance: The scenarios must be diverse and balanced:
${useCasesPerPerson / 2} scenarios must be related to the user's Professional Life.
${useCasesPerPerson / 2} scenarios must be related to the user's Personal Life.

${parts.additionalRequirements.getOrElse("")}

Structure of Each Scenario: For each use case, you will provide a brief description that outlines a realistic scenario where ${parts.evidenceDistributionDescription} The scenario should:
1. Be specific to the user's background
2. Include a natural question that the user would ask (this helps ensure diversity across all generated questions)
3. Provide enough context for later evidence generation

IMPORTANT: Each scenario_description MUST include the question that will be asked. This question will be refined in the next step but including it here ensures question diversity across all scenarios.

Output Format: JSON Schema
Your output must be a single JSON object. This object will contain one top-level key, "use_cases", which is an array of ${useCasesPerPerson} JSON objects. Each object in the array must adhere to the following schema:

{
  "id": "integer",
  "category": "string (either 'Professional Life' or 'Personal Life')",
  "scenario_description": "string"
}

Field Definitions
id: An integer from 1 to ${useCasesPerPerson}.
category: Must be either "Professional Life" or "Personal Life".
scenario_description: A description of a realistic scenario relevant to the user's background that includes:
- The context and situation
- The question the user would naturally ask in this scenario
- Enough detail for later evidence generation

The question should be naturally embedded in the scenario description, for example: "The user is planning a team retreat and asks 'What were the key activities we did at last year's retreat?'" This ensures each scenario has a clear question while maintaining natural flow.

Example (for your reference)
${formatExampleUseCase(parts.exampleUseCase)}

Please now generate the complete JSON object containing ${useCasesPerPerson} personalized scenario descriptions based on the provided user profile, following all instructions.
""".stripMargin
  }

  /**
   * Format the example use case for display in the prompt.
   */
  def formatExampleUseCase(example: EvidenceUseCase): String = {
    s"""{
  "id": ${example.id},
  "category": "${example.category}",
  "scenario_description": "${example.scenario_description}"
}"""
  }

  /**
   * Build the complete evidence core prompt from common structure and implementation-specific parts.
   * @param expectedSpeaker The speaker that all messages should be from ("User" or "Assistant")
   */
  def buildEvidenceCorePrompt(person: Personas.Person, useCase: EvidenceUseCase, parts: EvidenceCorePromptParts, evidenceCount: Int, expectedSpeaker: String = "User"): String = {
    val messageSchema = (1 to evidenceCount).map { i =>
      s"""    {
      "speaker": "$expectedSpeaker",
      "text": "string"
    }"""
    }.mkString(",\n")

    s"""
You are an expert AI assistant tasked with creating evidence core for ${parts.scenarioType.toLowerCase} scenarios. ${parts.taskDescription}

Your entire output must be a single, valid JSON object.

Input: ${parts.scenarioType} Scenario
Person Profile:
- Category: ${person.category}
- Role: ${person.role_name}
- Description: ${person.description}
- Background: ${person.background.getOrElse("No background provided")}

Scenario Description:
${useCase.scenario_description}

Core Task
Based on the scenario description and person profile, generate a complete evidence core consisting of:
1. A refined version of the question from the scenario that ${parts.specificInstructions} (use the question provided in the scenario as a starting point, but refine it to be more precise and testable)
2. The correct answer ${parts.specificInstructions}
3. Evidence messages ${parts.specificInstructions}

The evidence should be realistic for the person's background and the scenario described.

CRITICAL SPEAKER REQUIREMENTS:
- ALL messages in message_evidences MUST have speaker: "$expectedSpeaker"
- Do NOT mix speakers - every single message must be from "$expectedSpeaker"
- The speaker field only accepts two values: "User" or "Assistant"

${if (evidenceCount > 1) {
  s"""
CRITICAL REQUIREMENTS FOR NON-REDUNDANCY (${evidenceCount} pieces of evidence):
- Each of the $evidenceCount evidence messages must contain unique, non-overlapping information
- The question must be IMPOSSIBLE to answer correctly without ALL $evidenceCount pieces of evidence
- Each evidence message should contribute essential information that cannot be inferred from the others
- Avoid evidence messages that repeat, paraphrase, or provide redundant information
- If any single piece of evidence is removed, the question should become unanswerable
- The answer should require synthesizing information from ALL evidence messages
"""
} else {
  ""
}}
${parts.additionalGuidance.getOrElse("")}

Output Format: JSON Schema
Your output must be a single, valid JSON object:

{
  "question": "string",
  "answer": "string",
  "message_evidences": [
$messageSchema
  ]
}

Field Definitions
${parts.fieldDefinitions}

Please generate the evidence core based on the provided scenario and person profile.
""".stripMargin
  }
}