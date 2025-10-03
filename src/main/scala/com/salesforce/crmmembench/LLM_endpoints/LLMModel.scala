package com.salesforce.crmmembench.LLM_endpoints

import scala.util.Try

case class TokenUsage(
  inputTokens: Int,
  outputTokens: Int,
  totalTokens: Int,
  cachedInputTokens: Option[Int] = None
) {
  // Computed property for non-cached input tokens
  def nonCachedInputTokens: Option[Int] = cachedInputTokens.map(cached => inputTokens - cached)
  
  // Percentage of input tokens that were cached (0-100)
  def cacheHitRate: Option[Double] = cachedInputTokens.map { cached =>
    if (inputTokens > 0) cached.toDouble / inputTokens * 100 else 0.0
  }
  
  // Check if this response used any cached tokens
  def usedCache: Boolean = cachedInputTokens.exists(_ > 0)
}

case class LLMResponse(
  content: String, 
  modelName: String,
  tokenUsage: Option[TokenUsage] = None,
  cost: Double = 0.0
)

/**
 * Unified interface for all LLM models to provide consistent API
 */
trait LLMModel {
  def generateContent(prompt: String): Try[LLMResponse]
  def getModelName: String
  def getProvider: String
}
