package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.GeneralPrompts.PROJECT_BACKGROUND
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.questions.evidence.{GeneratedConversations, Message}
import com.salesforce.crmmembench.{Config, Personas}
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode

/**
 * Case class to hold scenario-specific prompt parts for conversation generation.
 */
case class ConversationPromptParts(
  evidenceType: String,             // Type of evidence scenario (e.g., "multi-evidence", "changing evidence")
  scenarioDescription: String,      // Brief description of what this scenario tests
  useCaseScenario: Option[String],  // Specific scenario description from the use case
  evidenceMessages: List[Message],  // The evidence messages that need to be embedded (with speaker info)
  question: String,                // The question that will be asked
  answer: String,                  // The expected answer
  evidenceCount: Int               // Number of conversations to generate
)

/**
 * Simple conversation generator that handles LLM calls and JSON parsing.
 * Extracted from EvidenceGenerator to reduce complexity and enable reuse.
 * 
 * @param llmModel The LLM model to use for generation (defaults to Gemini Flash JSON)
 */
class ConversationGenerator(llmModel: LLMModel = Gemini.flashJson) {

  /**
   * Get the full conversation generation prompt for testing purposes.
   * 
   * @param person The person this conversation is for
   * @param promptParts The scenario-specific parts of the prompt
   * @return The complete prompt string that would be sent to the LLM
   */
  def getFullConversationPrompt(person: Personas.Person, promptParts: ConversationPromptParts): String = {
    val conversationDescription = promptParts.evidenceCount match {
      case 1 => "one conversation"
      case 2 => "two separate conversations"
      case 3 => "three separate conversations"
      case 4 => "four separate conversations"
      case n => s"$n separate conversations"
    }

    val evidenceList = promptParts.evidenceMessages.zipWithIndex.map { case (msg, i) =>
      s"${i+1}. [${msg.speaker}] ${msg.text}"
    }.mkString("\n")

    val conversationArray = (1 to promptParts.evidenceCount).map(i => s"""
    {
      "messages": [
        {
          "speaker": "User or Assistant",
          "text": "string"
        }
      ]
    }""").mkString(",")

    val specificScenario = promptParts.useCaseScenario match {
      case Some(scenario) => s"\n\nSpecific Scenario:\n$scenario"
      case None => ""
    }

    s"""
You are creating realistic conversations between a User and an Assistant for a ${promptParts.evidenceType} memory test.

This is a memory benchmark where we test whether an AI can remember and recall information from previous conversations. You will generate the training conversations that contain the evidence the AI needs to remember.

Person Context (who the User is):
The following describes the user you're creating conversations for. This context helps you generate authentic, relevant conversations that fit their background and interests.

${person.role_name}: ${person.description}
${person.background.getOrElse("")}

Test Scenario (what we're testing):
${promptParts.scenarioDescription}$specificScenario

Evidence to Embed (what the AI needs to remember):
The following evidence must be naturally embedded within your generated conversations. Each piece shows who originally said it - [User] or [Assistant] - which determines how you should incorporate it into the dialogue.

$evidenceList

Target Question & Answer (what we'll test later):
After these conversations, we will ask: "${promptParts.question}"
The AI should answer: "${promptParts.answer}"

Your Task:
Generate $conversationDescription between the User and Assistant that naturally contain the evidence messages above. The conversations should feel authentic and establish realistic context where the evidence comes up organically during normal discussion. Each conversation represents a separate interaction that occurs at different times.

Conversation Requirements:
- Each conversation should be 80-120 messages long (40-60 turns)
- Conversations should feel natural and authentic for this person
- Evidence messages must be embedded naturally within the conversation flow
- Topics should be relevant to the person's role and background

CRITICAL SPEAKER REQUIREMENTS:
- Only TWO speakers are allowed: "User" and "Assistant"
- Do NOT use any other speaker names like "Human", "AI", "Bot", "Agent", etc.
- Every message MUST have speaker field set to either "User" or "Assistant"
- The conversation should alternate between User and Assistant messages naturally

CRITICAL REQUIREMENTS FOR EVIDENCE DISTRIBUTION:
1. The number of conversations MUST equal the number of evidence messages
2. Each evidence message MUST appear in exactly ONE conversation
3. The Nth evidence message MUST appear in the Nth conversation (evidence message 1 goes in conversation 1, etc.)
4. Evidence messages MUST be copied EXACTLY as provided - do not modify the text, only the speaker field should match who says it in the conversation
5. Evidence messages should appear naturally within the conversation flow, not just dropped in randomly

Example: If you have 3 evidence messages, you must generate 3 conversations where:
- Conversation 1 contains evidence message 1
- Conversation 2 contains evidence message 2  
- Conversation 3 contains evidence message 3

Output Format (JSON):
Your response must be valid JSON in exactly this format:
{
  "conversations": [$conversationArray
  ]
}

IMPORTANT: Each message must have exactly these fields:
- "speaker": Must be either "User" or "Assistant" (no other values allowed)
- "text": The message content

Generate realistic, engaging conversations that naturally incorporate the evidence as JSON.
""".stripMargin
  }

  /**
   * Generate conversations using standard schema with scenario-specific prompt parts.
   * This method handles all the boilerplate conversation generation logic.
   * 
   * @param person The person this conversation is for
   * @param promptParts The scenario-specific parts of the prompt
   * @return The generated conversations
   */
  def generateConversations(person: Personas.Person, promptParts: ConversationPromptParts): (GeneratedConversations, String) = {
    val fullPrompt = getFullConversationPrompt(person, promptParts)
    val (conversations, modelName) = generateStructured[GeneratedConversations](fullPrompt)
    (conversations, modelName)
  }

  /**
   * Generate a conversation with structured JSON response and model name tracking.
   * 
   * @param prompt The prompt to send to the LLM
   * @param decoder Implicit decoder for the expected response type
   * @tparam T The expected response type
   * @return Tuple of (parsed response, model name)
   */
  def generateStructured[T](prompt: String)(implicit decoder: Decoder[T]): (T, String) = {
    val llmResponse = llmModel.generateContent(PROJECT_BACKGROUND + "\n" + prompt).get

    decode[T](llmResponse.content) match {
      case Right(data) => (data, llmResponse.modelName)
      case Left(error) =>
        val preview = if (llmResponse.content.length > 500) llmResponse.content.take(500) + "..." else llmResponse.content
        if (Config.DEBUG) {
          println("Could not decode LLM response:")
          println(s"Error: $error")
          println(s"Response preview: $preview")
        }
        throw new RuntimeException(s"Failed to decode JSON: $error. LLM Response preview: $preview")
    }
  }

}