package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse, Gemini}
import com.salesforce.crmmembench.evaluation.EvaluationUtils
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.{Config, Utils}

import scala.collection.mutable.ListBuffer
import scala.util.{Success, Failure}

/**
 * Long context memory answerer that puts all conversation history directly into the prompt.
 * 
 * This is the "baseline" memory system that:
 * - Stores conversations in memory as a simple list
 * - Builds a single large prompt with all conversations for each question
 * - Relies on the LLM's context window to maintain memory
 * - Uses no external memory storage or retrieval
 * 
 * This implementation is stateful and NOT thread-safe. Create separate instances
 * for concurrent use.
 */
class LongContextMemoryAnswerer(model: LLMModel = Gemini.flash) extends MemoryAnswerer {

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

    // Build the prompt with judge criteria
    val prompt = buildPromptWithConversations(question, conversations.toList)
    
    // Get model response with retry logic
    val responseOpt = Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
      model.generateContent(prompt) match {
        case Success(response) => response
        case Failure(exception) => throw exception
      }
    }
    
    responseOpt match {
      case response: LLMResponse =>
        // Long context doesn't track individual conversation retrieval, return empty list
        AnswerResult(
          answer = Some(response.content),
          retrievedConversationIds = List.empty,
          inputTokens = response.tokenUsage.map(_.inputTokens),
          outputTokens = response.tokenUsage.map(_.outputTokens),
          cost = Some(response.cost),
          cachedInputTokens = response.tokenUsage.flatMap(_.cachedInputTokens)
        )
      case _ =>
        AnswerResult(None, List.empty)
    }
  }

  /**
   * Build the prompt for answering questions based on full conversation history.
   * Includes judge evaluation criteria to ensure the model understands how it will be evaluated.
   */
  def buildPromptWithConversations(question: String, conversations: List[Conversation]): String = {
    val conversationContext = buildConversationContext(conversations)
    
    s"""You are an assistant helping to answer questions based on conversation history. Your task is to incorporate ALL relevant prior knowledge from the conversations to provide accurate and complete answers.

${MemoryPromptUtils.getJudgeEvaluationCriteria}

Below are the conversations. When the User in a conversation refers to themselves ("I", "me", "my"), they are the person being asked about in the question.

$conversationContext

Based on ALL the information available in the conversations above, answer the following question. Ensure your response incorporates all relevant facts from the conversation history.

Question: $question

Answer:"""
  }

  /**
   * Build conversation context from multiple conversations.
   * Preserves conversation boundaries and structure.
   */
  def buildConversationContext(conversations: List[Conversation]): String = {
    conversations.zipWithIndex.map { case (conversation, index) =>
      val conversationNumber = index + 1
      val messages = conversation.messages.map(msg => s"${msg.speaker}: ${msg.text}").mkString("\n")
      
      // Include conversation ID if available for debugging
      val idInfo = conversation.id.map(id => s" (ID: $id)").getOrElse("")
      
      s"""Conversation $conversationNumber$idInfo:
$messages"""
    }.mkString("\n\n")
  }

  override def getMemoryType: String = "long_context"

  override def clearMemory(): Unit = {
    conversations.clear()
  }

}