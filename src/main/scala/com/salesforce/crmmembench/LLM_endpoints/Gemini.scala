package com.salesforce.crmmembench.LLM_endpoints

import com.google.genai.Client
import com.google.genai.types.{GenerateContentConfig, GenerateContentResponse}
import com.salesforce.crmmembench.CostTracker

import scala.util.Try

/**
 * Gemini model implementation that wraps Google's Vertex AI models
 */
class GeminiModel(modelName: String, responseMimeType: Option[String] = None) extends LLMModel {
  // Create client using builder pattern for Vertex AI
  val client = new Client.Builder()
    .vertexAI(true)
    .project("salesforce-research-internal")
    .location("us-central1")
    .build()
  
  val config: GenerateContentConfig = responseMimeType match {
    case Some(mimeType) => 
      GenerateContentConfig.builder()
        .responseMimeType(mimeType)
        .build()
    case None => null
  }
  
  override def generateContent(prompt: String): Try[LLMResponse] = {
    Try {
      val response: GenerateContentResponse = client.models.generateContent(
        modelName,
        prompt,
        config
      )
      
      val text = response.text()
      
      // Extract usage metadata if available
      val tokenUsage = if (response.usageMetadata().isPresent()) {
        val usage = response.usageMetadata().get()
        Some(TokenUsage(
          inputTokens = usage.promptTokenCount().orElse(0),
          outputTokens = usage.candidatesTokenCount().orElse(0),
          totalTokens = usage.totalTokenCount().orElse(0),
          cachedInputTokens = if (usage.cachedContentTokenCount().isPresent()) {
            Some(usage.cachedContentTokenCount().get())
          } else {
            None
          }
        ))
      } else {
        None
      }
      
      // Calculate cost based on model and token usage
      val cost = tokenUsage.map { usage =>
        val (inputCostPerMillion, outputCostPerMillion, cachedCostPerMillion) = modelName match {
          case "gemini-2.5-pro" =>
            // Gemini 2.5 Pro pricing depends on input token count
            if (usage.inputTokens <= 200000) {
              (1.25, 10.0, 0.31)  // ≤ 200K tokens, cached input
            } else {
              (2.5, 15.0, 0.625)    // > 200K tokens, cached is 75% cheaper
            }
          case "gemini-2.5-flash" =>
            (0.30, 2.5, 0.075)      // Gemini 2.5 Flash (GA), cached is 75% cheaper
          case "gemini-2.5-flash-lite" =>
            (0.10, 0.4, 0.025)      // Gemini 2.5 Flash Lite, cached is 75% cheaper
          case _ =>
            throw new RuntimeException("Unknown model")
        }
        
        // Handle Google's confusing token counting API
        // According to Google's spec, promptTokenCount includes cached tokens,
        // but in practice the API sometimes reports cached > prompt
        val prompt = usage.inputTokens
        val cached = usage.cachedInputTokens.getOrElse(0)
        val total = usage.totalTokens  // This is the safest reference if available
        
        // Derive a self-consistent picture
        val effectivePrompt = if (total > 0) {
          // Use total as ceiling if available
          math.min(total - usage.outputTokens, math.max(prompt, cached))
        } else {
          // Fallback: effective prompt is at least the max of prompt and cached
          math.max(prompt, cached)
        }
        
        // Calculate buckets ensuring no negative values
        val uncachedTokens = math.max(prompt - cached, 0)  // Never negative
        val cachedTokens = math.min(cached, effectivePrompt)  // Cap at effective size
        
        // Calculate costs
        val uncachedCost = uncachedTokens.toDouble / 1000000 * inputCostPerMillion
        val cachedCost = cachedTokens.toDouble / 1000000 * cachedCostPerMillion
        val outputCost = usage.outputTokens.toDouble / 1000000 * outputCostPerMillion
        
        uncachedCost + cachedCost + outputCost
      }.getOrElse(0.0)
      
      // Track costs globally
      CostTracker.recordInference(
        modelName = modelName,
        cost = cost,
        inputTokens = tokenUsage.map(_.inputTokens),
        outputTokens = tokenUsage.map(_.outputTokens)
      )
      
      LLMResponse(text, modelName, tokenUsage, cost)
    }
  }
  
  override def getModelName: String = modelName
  override def getProvider: String = "gemini"
}

object Gemini {
  
  // Raw models without rate limiting (for backward compatibility if needed)
  lazy val flashRaw: LLMModel = new GeminiModel("gemini-2.5-flash")
  lazy val proRaw: LLMModel = new GeminiModel("gemini-2.5-pro")
  lazy val flashLiteRaw: LLMModel = new GeminiModel("gemini-2.5-flash-lite")
  
  lazy val flashJsonRaw: LLMModel = new GeminiModel("gemini-2.5-flash", Some("application/json"))
  lazy val proJsonRaw: LLMModel = new GeminiModel("gemini-2.5-pro", Some("application/json"))
  lazy val flashLiteJsonRaw: LLMModel = new GeminiModel("gemini-2.5-flash-lite", Some("application/json"))
  
  // Rate-limited models (default)
  lazy val flash: LLMModel = RateLimitedLLMModel.withDefaultConfig(flashRaw)
  lazy val pro: LLMModel = RateLimitedLLMModel.withDefaultConfig(proRaw)
  lazy val flashLite: LLMModel = RateLimitedLLMModel.withDefaultConfig(flashLiteRaw)
  
  lazy val flashJson: LLMModel = RateLimitedLLMModel.withDefaultConfig(flashJsonRaw)
  lazy val proJson: LLMModel = RateLimitedLLMModel.withDefaultConfig(proJsonRaw)
  lazy val flashLiteJson: LLMModel = RateLimitedLLMModel.withDefaultConfig(flashLiteJsonRaw)
}
 object TestGemini {
   def main(args: Array[String]): Unit = {
     println(Gemini.flash.generateContent("Hi"))
   }
 }