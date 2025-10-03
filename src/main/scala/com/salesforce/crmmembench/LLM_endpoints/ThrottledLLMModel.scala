package com.salesforce.crmmembench.LLM_endpoints

import scala.util.Try

/**
 * A simple throttling wrapper for LLMModel that synchronizes all generateContent calls.
 * This ensures thread safety and naturally throttles request rate by processing one at a time.
 */
class ThrottledLLMModel(underlying: LLMModel) extends LLMModel {
  
  override def generateContent(prompt: String): Try[LLMResponse] = synchronized {
    underlying.generateContent(prompt)
  }
  
  override def getModelName: String = underlying.getModelName
  
  override def getProvider: String = underlying.getProvider
}

object ThrottledLLMModel {
  def apply(model: LLMModel): ThrottledLLMModel = new ThrottledLLMModel(model)
}