package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse, Gemini}
import com.salesforce.crmmembench.conversations.ConversationValidator
import com.salesforce.crmmembench.evaluation.EvaluationUtils
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.{Config, Utils}

import scala.collection.mutable.ListBuffer
import scala.util.{Success, Failure}

/**
 * Block-based memory answerer that processes conversations in chunks of 10.
 * 
 * This memory system:
 * - Breaks down conversations into blocks of 10
 * - Extracts relevant information from each block independently
 * - Aggregates all extracted information
 * - Generates final answer based on aggregated information
 * 
 * This approach helps with:
 * - Better handling of very long conversation lists
 * - More focused information extraction
 * - Reduced context confusion for the model
 * 
 * @param model The main model used for generating final answers
 * @param blockSize The size of each conversation block
 * @param helperModel Helper model for block extraction
 * 
 * This implementation is stateful and NOT thread-safe. Create separate instances
 * for concurrent use.
 */
class BlockBasedMemoryAnswerer(
  model: LLMModel = Gemini.flash,
  blockSize: Int = 10,
  helperModel: LLMModel
) extends MemoryAnswerer {

  // Store conversations in memory
  val conversations = ListBuffer[Conversation]()

  override def addConversation(conversation: Conversation): Unit = {
    conversations += conversation
  }

  override def answerQuestion(question: String, testCaseId: String): AnswerResult = {
    if (conversations.isEmpty) {
      println("      No conversations loaded in memory")
      return AnswerResult(None, List.empty)
    }

    // Split conversations into blocks
    val conversationBlocks = conversations.toList.grouped(blockSize).toList
    
    // Extract information from each block in parallel
    val blockExtractions = Utils.parallelMap(conversationBlocks, threadCount = 100) { block =>
      extractInformationFromBlock(question, block)
    }
    
    // Collect all extracted information and token usage
    val allExtractedInfo = blockExtractions.flatMap(_.extractedInfo)
    val totalInputTokens = blockExtractions.flatMap(_.inputTokens).sum
    val totalOutputTokens = blockExtractions.flatMap(_.outputTokens).sum
    val totalCost = blockExtractions.flatMap(_.cost).sum
    val totalCachedTokens = blockExtractions.flatMap(_.cachedInputTokens).sum
    
    // Generate final answer using aggregated information
    val finalAnswer = if (allExtractedInfo.isEmpty || allExtractedInfo.forall(isNoInformationRelevant)) {
      // No relevant information found in any block
      AnswerResult(
        answer = Some("I don't have any information to answer this question."),
        retrievedConversationIds = List.empty,
        inputTokens = None,  // No final answer LLM call was made, so no input tokens
        outputTokens = Some(totalOutputTokens),  // Still track extraction output tokens
        cost = Some(totalCost),
        cachedInputTokens = None,  // No final answer cached tokens
        memorySystemResponses = allExtractedInfo
      )
    } else {
      // Generate final answer from extracted information
      val relevantExtractions = allExtractedInfo.filterNot(isNoInformationRelevant)
      generateFinalAnswer(question, relevantExtractions, allExtractedInfo, totalOutputTokens, totalCost)
    }
    
    finalAnswer
  }

  /**
   * Extract relevant information from a block of conversations.
   */
  def extractInformationFromBlock(
    question: String, 
    conversations: List[Conversation]
  ): BlockExtractionResult = {
    val prompt = buildExtractionPrompt(question, conversations)
    
    // Use helper model for extraction
    
    // Get model response with retry logic
    val responseOpt = Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
      helperModel.generateContent(prompt) match {
        case Success(response) => response
        case Failure(exception) => throw exception
      }
    }
    
    responseOpt match {
      case response: LLMResponse =>
        BlockExtractionResult(
          extractedInfo = Some(response.content.trim),
          inputTokens = response.tokenUsage.map(_.inputTokens),
          outputTokens = response.tokenUsage.map(_.outputTokens),
          cost = Some(response.cost),
          cachedInputTokens = response.tokenUsage.flatMap(_.cachedInputTokens)
        )
      case _ =>
        BlockExtractionResult(
          extractedInfo = Some("No information is relevant."),
          inputTokens = None,
          outputTokens = None,
          cost = None,
          cachedInputTokens = None
        )
    }
  }

  /**
   * Build prompt for extracting information from a conversation block.
   */
  def buildExtractionPrompt(
    question: String, 
    conversations: List[Conversation]
  ): String = {
    val conversationContext = EvaluationUtils.buildConversationContext(conversations)
    
    s"""You are an assistant that extracts relevant information from conversations to help answer questions.

Your task is to carefully review the following conversations and extract ALL information that might be relevant to answering the question below. This includes:
- Direct facts that answer the question
- Related context that provides background
- Any details that could be useful for a complete answer

IMPORTANT INSTRUCTIONS:
- If you find ANY relevant information, extract it in DETAIL
- Include DIRECT QUOTES from the conversations whenever possible
- Provide the full context around relevant statements
- Specify which conversation and speaker provided each piece of information
- Include exact wording, not paraphrases or summaries
- If multiple messages discuss the same topic, include ALL of them with their direct quotes
- Format your extraction as:
  * From Conversation X, [Speaker]: "exact quote here"
  * Context: [any relevant surrounding information]
- If there is absolutely NO relevant information in these conversations, respond with exactly: "No information is relevant."
- Do not answer the question directly - only extract relevant information

EXAMPLE of detailed extraction:
If asked about user preferences and you find relevant information:
* From Conversation 2, User: "I really prefer coffee over tea, especially in the mornings"
* Context: This was mentioned while discussing breakfast habits
* From Conversation 2, Assistant: "Noted that you prefer coffee. Would you like recommendations for coffee shops?"
* From Conversation 3, User: "As I mentioned before, I'm a coffee person"

Conversations:

$conversationContext

Question to consider: $question

Extracted relevant information:"""
  }

  /**
   * Generate final answer using all extracted information.
   */
  def generateFinalAnswer(
    question: String,
    relevantExtractedInfo: List[String],
    allExtractedInfo: List[String],
    previousOutputTokens: Int,
    previousCost: Double
  ): AnswerResult = {
    val prompt = buildFinalAnswerPrompt(question, relevantExtractedInfo)
    
    // Get model response with retry logic
    val responseOpt = Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
      model.generateContent(prompt) match {
        case Success(response) => response
        case Failure(exception) => throw exception
      }
    }
    
    responseOpt match {
      case response: LLMResponse =>
        AnswerResult(
          answer = Some(response.content),
          retrievedConversationIds = List.empty,
          inputTokens = response.tokenUsage.map(_.inputTokens),  // Only final answer input tokens
          outputTokens = Some(previousOutputTokens + response.tokenUsage.map(_.outputTokens).getOrElse(0)),
          cost = Some(previousCost + response.cost),
          cachedInputTokens = response.tokenUsage.flatMap(_.cachedInputTokens),  // Only final answer cached tokens
          memorySystemResponses = allExtractedInfo
        )
      case _ =>
        AnswerResult(
          answer = None,
          retrievedConversationIds = List.empty,
          inputTokens = None,  // No final answer was generated, so no input tokens
          outputTokens = Some(previousOutputTokens),  // Still track extraction output tokens
          cost = Some(previousCost),
          cachedInputTokens = None,  // No final answer cached tokens
          memorySystemResponses = allExtractedInfo
        )
    }
  }

  /**
   * Build prompt for generating final answer from extracted information.
   */
  def buildFinalAnswerPrompt(question: String, extractedInfo: List[String]): String = {
    val infoContext = extractedInfo.zipWithIndex.map { case (info, idx) =>
      s"Information Block ${idx + 1}:\n$info"
    }.mkString("\n\n")
    
    s"""You are an assistant helping to answer questions based on extracted information. Your task is to synthesize ALL the provided information to give a complete and accurate answer.

${MemoryPromptUtils.getJudgeEvaluationCriteria}

The following information has been extracted from conversations as relevant to your question:

$infoContext

Based on ALL the extracted information above, answer the following question. Ensure your response incorporates all relevant facts.

Question: $question

Answer:"""
  }

  override def getMemoryType: String = "block_based"

  override def clearMemory(): Unit = {
    conversations.clear()
  }

  /**
   * Check if a string is approximately "No information is relevant" using fuzzy matching.
   * Normalizes strings by converting to lowercase and keeping only alphanumeric characters,
   * then checks if the Levenshtein distance is within the threshold.
   */
  def isNoInformationRelevant(text: String): Boolean = {
    val targetPhrase = "No information is relevant."
    val normalizedTarget = normalizeForComparison(targetPhrase)
    val normalizedText = normalizeForComparison(text)
    
    // Allow Levenshtein distance of 5
    val distance = ConversationValidator.levenshteinDistance(normalizedTarget, normalizedText)
    distance <= 5
  }

  /**
   * Normalize a string for fuzzy comparison by converting to lowercase
   * and keeping only alphanumeric characters.
   */
  def normalizeForComparison(text: String): String = {
    text.toLowerCase.replaceAll("[^a-z0-9]", "")
  }

  /**
   * Result from extracting information from a single block.
   */
  case class BlockExtractionResult(
    extractedInfo: Option[String],
    inputTokens: Option[Int],
    outputTokens: Option[Int],
    cost: Option[Double],
    cachedInputTokens: Option[Int]
  )
}