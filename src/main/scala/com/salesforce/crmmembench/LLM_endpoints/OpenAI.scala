package com.salesforce.crmmembench.LLM_endpoints

import dev.langchain4j.model.openai.{OpenAiChatModel, OpenAiTokenUsage}
import dev.langchain4j.data.message.{UserMessage, AiMessage}
import dev.langchain4j.model.output.Response
// import dev.langchain4j.model.chat.ChatLanguageModel
// import dev.langchain4j.model.output.TokenUsage
import com.salesforce.crmmembench.CostTracker
import scala.util.{Try, Success, Failure}

/**
 * Base class for OpenAI-compatible models using LangChain4j
 */
abstract class Lang4jModel(modelName: String, temperature: Double = 0.2, jsonResponseFormat: Boolean = false) extends LLMModel {
  val apiKey: String = sys.env.getOrElse("OPENAI_API_KEY",
    throw new RuntimeException("OPENAI_API_KEY environment variable not set"))
  
  val model: OpenAiChatModel = {
    val builder = OpenAiChatModel.builder()
      .apiKey(apiKey)
      .modelName(modelName)
      .temperature(temperature)
    
    if (jsonResponseFormat) {
      builder.responseFormat("json_object")
    }
    
    builder.build()
  }
  
  override def generateContent(prompt: String): Try[LLMResponse] = {
    Try {
      // Use the chat method with a single user message
      val chatResponse = model.chat(UserMessage.from(prompt))
      val content = chatResponse.aiMessage().text()
      
      // Token usage is not available with the chat method, use default estimates
      val promptTokens = prompt.length / 4  // Rough estimate based on prompt
      val completionTokens = content.length / 4
      val totalTokens = promptTokens + completionTokens
        
      // Calculate actual cost based on token usage
      val cost = calculateCost(modelName, promptTokens, completionTokens)
        
      // Track costs globally with actual token info
      CostTracker.recordInference(
        modelName = modelName,
        cost = cost,
        inputTokens = Some(promptTokens),
        outputTokens = Some(completionTokens)
      )
        
      // Create TokenUsage object for LLMResponse
      val tokenUsage = com.salesforce.crmmembench.LLM_endpoints.TokenUsage(
        inputTokens = promptTokens,
        outputTokens = completionTokens,
        totalTokens = totalTokens
      )
        
      LLMResponse(content, modelName, Some(tokenUsage), cost)
    }
  }
  
  /**
   * Calculate cost based on token usage and model pricing
   */
  def calculateCost(modelName: String, inputTokens: Int, outputTokens: Int): Double = {
    val (inputPricePerMillion, outputPricePerMillion) = modelName match {
      case "gpt-4o" => (2.50, 10.00) // $2.50 per 1M input tokens, $10.00 per 1M output tokens (as of Aug 2024)
      case "gpt-4o-mini" => (0.15, 0.60) // $0.15 per 1M input tokens, $0.60 per 1M output tokens
      case "gpt-5" => (15.00, 60.00) // $15.00 per 1M input tokens, $60.00 per 1M output tokens (estimated)
      case "gpt-5-mini" => (3.00, 12.00) // $3.00 per 1M input tokens, $12.00 per 1M output tokens (estimated)
      case "gpt-5-turbo" => (8.00, 32.00) // $8.00 per 1M input tokens, $32.00 per 1M output tokens (estimated)
      case _ => throw new RuntimeException("Can't get prices for this model")
    }
    
    val inputCost = (inputTokens.toDouble / 1000000) * inputPricePerMillion
    val outputCost = (outputTokens.toDouble / 1000000) * outputPricePerMillion
    
    inputCost + outputCost
  }
  
  override def getModelName: String = modelName
  override def getProvider: String = "openai"
}

/**
 * OpenAI-specific model implementations
 */
class OpenAIModel(modelName: String, temperature: Double = 0.2, jsonResponseFormat: Boolean = false) extends Lang4jModel(modelName, temperature, jsonResponseFormat) {
  override def getProvider: String = "openai"
}

object OpenAI {
  // Legacy object for backward compatibility
  val openAi: OpenAiChatModel = {
    val apiKey = sys.env.getOrElse("OPENAI_API_KEY",
      throw new RuntimeException("OPENAI_API_KEY environment variable not set"))
    
    OpenAiChatModel.builder()
      .apiKey(apiKey)
      .modelName("gpt-4o-mini") // or "gpt-4o"
      .temperature(0.2)
      .build()
  }
  
  // New LLMModel implementations
  lazy val gpt4oMini: LLMModel = new OpenAIModel("gpt-4o-mini")
  lazy val gpt4o: LLMModel = new OpenAIModel("gpt-4o")
  
  // GPT-5 models
  lazy val gpt5: LLMModel = new OpenAIModel("gpt-5")
  lazy val gpt5Mini: LLMModel = new OpenAIModel("gpt-5-mini")
  lazy val gpt5Turbo: LLMModel = new OpenAIModel("gpt-5-turbo")
  
  // JSON response format models
  lazy val gpt4oMiniJson: LLMModel = new OpenAIModel("gpt-4o-mini", jsonResponseFormat = true)
  lazy val gpt4oJson: LLMModel = new OpenAIModel("gpt-4o", jsonResponseFormat = true)
  
  // GPT-5 JSON response format models
  lazy val gpt5Json: LLMModel = new OpenAIModel("gpt-5", jsonResponseFormat = true)
  lazy val gpt5MiniJson: LLMModel = new OpenAIModel("gpt-5-mini", jsonResponseFormat = true)
  lazy val gpt5TurboJson: LLMModel = new OpenAIModel("gpt-5-turbo", jsonResponseFormat = true)

  def main(args: Array[String]): Unit = {
    val chatResponse = openAi.chat(UserMessage.from("Hi, how are you?"))
    println(chatResponse.aiMessage().text())
    // Token usage not available with chat method
  }
}
