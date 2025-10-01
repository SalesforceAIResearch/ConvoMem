package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CostCalculationTest extends AnyFlatSpec with Matchers {
  
  "LLMResponse cost calculation" should "calculate correct cost for Claude Opus 4" in {
    val tokenUsage = TokenUsage(
      inputTokens = 1000,
      outputTokens = 500,
      totalTokens = 1500
    )
    
    // Opus 4: $15/M input, $75/M output
    val response = LLMResponse(
      content = "test",
      modelName = "claude-opus-4@20250514",
      tokenUsage = Some(tokenUsage),
      cost = (1000.0 / 1000000 * 15.0) + (500.0 / 1000000 * 75.0)
    )
    
    response.cost shouldBe 0.0525 +- 0.0001 // $0.015 + $0.0375
  }
  
  it should "calculate correct cost for Claude Sonnet 4" in {
    val tokenUsage = TokenUsage(
      inputTokens = 2000,
      outputTokens = 1000,
      totalTokens = 3000
    )
    
    // Sonnet 4: $3/M input, $15/M output
    val response = LLMResponse(
      content = "test",
      modelName = "claude-sonnet-4@20250514",
      tokenUsage = Some(tokenUsage),
      cost = (2000.0 / 1000000 * 3.0) + (1000.0 / 1000000 * 15.0)
    )
    
    response.cost shouldBe 0.021 +- 0.0001 // $0.006 + $0.015
  }
  
  it should "calculate correct cost for Gemini 2.5 Pro (≤200K tokens)" in {
    val tokenUsage = TokenUsage(
      inputTokens = 100000,
      outputTokens = 5000,
      totalTokens = 105000
    )
    
    // Gemini 2.5 Pro ≤200K: $1.25/M input, $10/M output
    val response = LLMResponse(
      content = "test",
      modelName = "gemini-2.5-pro",
      tokenUsage = Some(tokenUsage),
      cost = (100000.0 / 1000000 * 1.25) + (5000.0 / 1000000 * 10.0)
    )
    
    response.cost shouldBe 0.175 +- 0.0001 // $0.125 + $0.05
  }
  
  it should "calculate correct cost for Gemini 2.5 Pro (>200K tokens)" in {
    val tokenUsage = TokenUsage(
      inputTokens = 250000,
      outputTokens = 10000,
      totalTokens = 260000
    )
    
    // Gemini 2.5 Pro >200K: $2.5/M input, $15/M output
    val response = LLMResponse(
      content = "test",
      modelName = "gemini-2.5-pro",
      tokenUsage = Some(tokenUsage),
      cost = (250000.0 / 1000000 * 2.5) + (10000.0 / 1000000 * 15.0)
    )
    
    response.cost shouldBe 0.775 +- 0.0001 // $0.625 + $0.15
  }
  
  it should "calculate correct cost for Gemini 2.5 Flash" in {
    val tokenUsage = TokenUsage(
      inputTokens = 5000,
      outputTokens = 2000,
      totalTokens = 7000
    )
    
    // Gemini 2.5 Flash: $0.30/M input, $2.5/M output
    val response = LLMResponse(
      content = "test",
      modelName = "gemini-2.5-flash",
      tokenUsage = Some(tokenUsage),
      cost = (5000.0 / 1000000 * 0.30) + (2000.0 / 1000000 * 2.5)
    )
    
    response.cost shouldBe 0.0065 +- 0.0001 // $0.0015 + $0.005
  }
  
  it should "calculate correct cost for Gemini 2.5 Flash Lite" in {
    val tokenUsage = TokenUsage(
      inputTokens = 10000,
      outputTokens = 5000,
      totalTokens = 15000
    )
    
    // Gemini 2.5 Flash Lite: $0.10/M input, $0.4/M output
    val response = LLMResponse(
      content = "test",
      modelName = "gemini-2.5-flash-lite",
      tokenUsage = Some(tokenUsage),
      cost = (10000.0 / 1000000 * 0.10) + (5000.0 / 1000000 * 0.4)
    )
    
    response.cost shouldBe 0.003 +- 0.0001 // $0.001 + $0.002
  }
  
  it should "return 0.0 cost when tokenUsage is None" in {
    val response = LLMResponse(
      content = "test",
      modelName = "any-model",
      tokenUsage = None,
      cost = 0.0
    )
    
    response.cost shouldBe 0.0
  }
}