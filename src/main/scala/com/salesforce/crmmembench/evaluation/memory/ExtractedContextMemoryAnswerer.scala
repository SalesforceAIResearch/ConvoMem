package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel, LLMResponse}
import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import com.salesforce.crmmembench.{Config, Utils}

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
 * A memory answerer that uses a two-stage approach:
 * 1. Extraction stage: Extract all relevant information from conversations using a helper model
 * 2. Answer stage: Answer the question based on the extracted context
 */
class ExtractedContextMemoryAnswerer(
    model: LLMModel = Gemini.flash,
    helperModel: LLMModel
) extends MemoryAnswerer {
  
  val conversations: ListBuffer[Conversation] = ListBuffer()

  override def initialize(): Unit = {}

  override def addConversation(conversation: Conversation): Unit = {
    conversations += conversation
    if (Config.DEBUG) println(s"Added conversation. Total: ${conversations.length}")
  }

  override def addConversations(newConversations: List[Conversation]): Unit = {
    conversations ++= newConversations
    if (Config.DEBUG) println(s"Added ${newConversations.length} conversations. Total: ${conversations.length}")
  }

  /**
   * Extract relevant context from all conversations for a given question
   * Returns extraction result with token usage tracking
   */
  def extractRelevantContext(question: String): ExtractionResult = {
    val allMessages = conversations.flatMap { conv =>
      conv.messages.map { msg =>
        s"${msg.speaker}: ${msg.text}"
      }
    }.mkString("\n")

    val extractionPromptText = 
      s"""You are a helpful assistant that extracts relevant information from conversations.
         |
         |Given the following question and conversation history, extract ALL relevant information that could help answer the question.
         |Include:
         |- Direct quotes from the conversations
         |- Context and background information
         |- Related facts and details
         |- Tone, preferences, or patterns mentioned
         |- Any other information that might be helpful
         |
         |Be comprehensive - it's better to include too much than too little.
         |If there's no relevant information, say "No relevant information found."
         |
         |Question: $question
         |
         |Conversation History:
         |$allMessages
         |
         |Extracted Relevant Information:""".stripMargin

    // Use the helper model for extraction
    
    // Follow the pattern from LongContextMemoryAnswerer and BlockBasedMemoryAnswerer
    val responseOpt = Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
      helperModel.generateContent(extractionPromptText) match {
        case Success(response) => response
        case Failure(exception) => throw exception
      }
    }
    
    responseOpt match {
      case response: LLMResponse =>
        if (Config.DEBUG) println(s"Extraction response length: ${response.content.length} characters")
        ExtractionResult(
          extractedInfo = response.content,
          inputTokens = response.tokenUsage.map(_.inputTokens),
          outputTokens = response.tokenUsage.map(_.outputTokens),
          cost = Some(response.cost),
          cachedInputTokens = response.tokenUsage.flatMap(_.cachedInputTokens)
        )
      case _ =>
        // This shouldn't happen if retry logic is working correctly
        ExtractionResult(
          extractedInfo = "No relevant information found.",
          inputTokens = None,
          outputTokens = None,
          cost = None,
          cachedInputTokens = None
        )
    }
  }
  
  /**
   * Helper case class to hold extraction results with token tracking
   */
  case class ExtractionResult(
    extractedInfo: String,
    inputTokens: Option[Int],
    outputTokens: Option[Int],
    cost: Option[Double],
    cachedInputTokens: Option[Int]
  )

  def answerQuestion(question: String, testCaseId: String): AnswerResult = {
    if (Config.DEBUG) println(s"Answering question: $question")
    
    // Stage 1: Extract relevant context
    val extractionResult = extractRelevantContext(question)
    val extractedContext = extractionResult.extractedInfo
    
    // Stage 2: Answer based on extracted context
    val answerPromptText = 
      s"""You are an assistant helping to answer questions based on extracted information from conversation history. Your task is to incorporate ALL relevant information from the extracted context to provide accurate and complete answers.

${MemoryPromptUtils.getJudgeEvaluationCriteria}

The following information has been extracted from conversations as relevant to your question:

$extractedContext

Based on ALL the extracted information above, answer the following question. Ensure your response incorporates all relevant facts. If you have partial information, provide what you know and explicitly state what you don't know.

Question: $question

Answer:""".stripMargin

    // Follow the pattern from LongContextMemoryAnswerer
    val responseOpt = Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
      model.generateContent(answerPromptText) match {
        case Success(response) => response
        case Failure(exception) => throw exception
      }
    }
    
    responseOpt match {
      case response: LLMResponse =>
        if (Config.DEBUG) println(s"Generated answer: ${response.content}")
        
        // Only track tokens from the final answer request, not the extraction phase
        // Still aggregate output tokens and costs from both phases
        val totalOutputTokens = (
          extractionResult.outputTokens.getOrElse(0) + 
          response.tokenUsage.map(_.outputTokens).getOrElse(0)
        )
        val totalCost = (
          extractionResult.cost.getOrElse(0.0) + 
          response.cost
        )
        
        AnswerResult(
          answer = Some(response.content),
          retrievedConversationIds = List.empty,
          inputTokens = response.tokenUsage.map(_.inputTokens),  // Only final answer input tokens
          outputTokens = Some(totalOutputTokens),
          cost = Some(totalCost),
          cachedInputTokens = response.tokenUsage.flatMap(_.cachedInputTokens),  // Only final answer cached tokens
          memorySystemResponses = List(extractedContext)
        )
      case _ =>
        AnswerResult(
          answer = None,
          retrievedConversationIds = List.empty,
          inputTokens = None,  // No final answer was generated, so no input tokens
          outputTokens = extractionResult.outputTokens,  // Still track extraction output tokens
          cost = extractionResult.cost,
          cachedInputTokens = None,  // No final answer cached tokens
          memorySystemResponses = List(extractedContext)
        )
    }
  }

  def clearMemory(): Unit = {
    if (Config.DEBUG) println("Clearing ExtractedContextMemoryAnswerer memory")
    conversations.clear()
  }

  def getMemoryType: String = "extracted_context"

}