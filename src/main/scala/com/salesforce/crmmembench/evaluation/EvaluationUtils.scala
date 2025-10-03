package com.salesforce.crmmembench.evaluation

// ResponseHandler no longer needed with new SDK
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel, LLMResponse}
import com.salesforce.crmmembench.questions.evidence.generation.AnsweringEvaluation
import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import com.salesforce.crmmembench.{Config, Utils}

import scala.util.{Failure, Success, Try}

/**
 * Shared evaluation utilities for consistent model testing across different evaluation scenarios.
 * 
 * This object contains reusable methods for:
 * - Getting model responses from conversation context and questions
 * - Verifying answer correctness using Gemini as an AI judge
 * - Standard prompt templates for consistent evaluation
 * - Logging evaluation results for analysis
 */
object EvaluationUtils {

  /**
   * Get the prompt for asking a model to answer a question based on conversations.
   */
  def getModelAnswerPrompt(conversations: List[Conversation], question: String): String = {
    val conversationContext = buildConversationContext(conversations)
    s"""Answer the question based on the conversations below. If the User in the conversation refers to themselves ("I", "me", "my"), they are the person being asked about. Be direct and factual.

$conversationContext

Question: $question

Answer:"""
  }
  
  /**
   * Get full model response including token usage for a given conversation context and question with retry logic.
   *
   * @param conversations List of Conversation objects forming the conversation context
   * @param question      The question to ask the model
   * @param model         The LLM model to use (defaults to Gemini.flash)
   * @return Option containing the full LLMResponse with content and token usage, None if all retries failed
   */
  def getModelResponse(conversations: List[Conversation], question: String, model: LLMModel = Gemini.flash): Option[LLMResponse] = {
    Try {
      Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
        val prompt = getModelAnswerPrompt(conversations, question)
        model.generateContent(prompt).get
      }
    } match {
      case Success(result) => Some(result)
      case Failure(exception) =>
        println(s"      Failed to get model answer after ${Config.Evaluation.MODEL_ANSWER_MAX_RETRIES} retries: ${exception.getMessage}")
        None
    }
  }

  /**
   * Build conversation context from multiple conversations, preserving conversation boundaries.
   *
   * @param conversations List of Conversation objects
   * @return Formatted string with clear conversation boundaries
   */
  def buildConversationContext(conversations: List[Conversation]): String = {
    conversations.zipWithIndex.map { case (conversation, index) =>
      val conversationNumber = index + 1
      val messages = conversation.messages.map(msg => s"${msg.speaker}: ${msg.text}").mkString("\n")

      s"""Conversation $conversationNumber:
$messages"""
    }.mkString("\n\n")
  }

  /**
   * Verify if a model's response is correct using a custom judge prompt with retry logic.
   *
   * @param judgePrompt The custom judge prompt to use
   * @param judgeModel The LLM model to use for judging (defaults to Gemini.flash)
   * @return Option containing Boolean result, None if all retries failed
   */
  def verifyAnswerCorrectnessWithPrompt(judgePrompt: String, judgeModel: LLMModel = Gemini.flash): Option[Boolean] = {
  //println(judgePrompt)
    Try {
      Utils.retry(Config.Evaluation.ANSWER_VERIFICATION_MAX_RETRIES) {
        val response = judgeModel.generateContent(judgePrompt).get
        val judgeResponse = response.content.trim.toLowerCase

        // Check for RIGHT/WRONG responses
        val containsRight = judgeResponse.contains("right")
        val containsWrong = judgeResponse.contains("wrong")
        
        if (containsRight && containsWrong) {
          // Ambiguous response - contains both
          println(s"⚠️  WARNING: Judge response contains both 'right' and 'wrong': '$judgeResponse'")
          println(s"     Judge prompt: ${judgePrompt.take(200)}...")
          // Default to false for ambiguous responses
          false
        } else if (containsRight) {
          true
        } else if (containsWrong) {
          false
        } else {
          // Invalid response
          println(s"⚠️  WARNING: Judge response is neither 'right' nor 'wrong': '$judgeResponse'")
          println(s"     Judge prompt: ${judgePrompt.take(200)}...")
          // Throw exception to trigger retry
          throw new RuntimeException(s"Invalid judge response: '$judgeResponse' - must contain RIGHT or WRONG")
        }
      }
    } match {
      case Success(result) => Some(result)
      case Failure(exception) =>
        println(s"      Failed to verify answer correctness after ${Config.Evaluation.ANSWER_VERIFICATION_MAX_RETRIES} retries: ${exception.getMessage}")
        None
    }
  }

  /**
   * Verify if a model's response is correct using an AI judge with retry logic.
   * Enhanced version that includes evidence messages for better context.
   *
   * @param question            The original question asked
   * @param correctAnswer       The ground truth answer
   * @param modelResponse       The model's actual response
   * @param evidenceMessages    The evidence messages that were available to answer the question
   * @param answeringEvaluation The evaluation strategy to use
   * @param judgeModel         The LLM model to use for judging (defaults to Gemini.flash)
   * @return Option containing Boolean result, None if all retries failed
   */
  def verifyAnswerCorrectness(
    question: String, 
    correctAnswer: String, 
    modelResponse: String, 
    evidenceMessages: List[Message], 
    answeringEvaluation: AnsweringEvaluation,
    judgeModel: LLMModel = Gemini.flash
  ): Option[Boolean] = {
    val judgePrompt = answeringEvaluation.getJudgePromptTemplate(question, correctAnswer, modelResponse, evidenceMessages)
    verifyAnswerCorrectnessWithPrompt(judgePrompt, judgeModel)
  }


}
