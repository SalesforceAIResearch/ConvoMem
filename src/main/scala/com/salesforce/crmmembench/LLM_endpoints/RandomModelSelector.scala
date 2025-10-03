package com.salesforce.crmmembench.LLM_endpoints

import scala.util.{Random, Try}

/**
 * LLMModel implementation that randomly selects from a list of models for each request
 */
class RandomModelSelector(
  models: List[LLMModel],
  random: Random = new Random()
) extends LLMModel {
  
  require(models.nonEmpty, "Model list cannot be empty")
  
  override def generateContent(prompt: String): Try[LLMResponse] = {
    val selectedModel = models(random.nextInt(models.length))
    selectedModel.generateContent(prompt).map { response =>
      LLMResponse(response.content, selectedModel.getModelName, response.tokenUsage, response.cost)
    }
  }
  
  override def getModelName: String = {
    s"RandomSelector[${models.map(_.getModelName).mkString(", ")}]"
  }
  
  override def getProvider: String = "Mixed"
}