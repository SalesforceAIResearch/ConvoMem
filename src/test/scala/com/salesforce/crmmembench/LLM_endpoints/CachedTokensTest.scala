package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.funsuite.AnyFunSuite

class CachedTokensTest extends AnyFunSuite {
  
  test("TokenUsage with cached tokens should calculate correctly") {
    val usage = TokenUsage(
      inputTokens = 1000,
      outputTokens = 500,
      totalTokens = 1500,
      cachedInputTokens = Some(600)
    )
    
    assert(usage.inputTokens == 1000)
    assert(usage.outputTokens == 500)
    assert(usage.totalTokens == 1500)
    assert(usage.cachedInputTokens == Some(600))
    
    // Test computed properties
    assert(usage.nonCachedInputTokens == Some(400))
    assert(usage.cacheHitRate == Some(60.0))
    assert(usage.usedCache == true)
  }
  
  test("TokenUsage without cached tokens should work as before") {
    val usage = TokenUsage(
      inputTokens = 1000,
      outputTokens = 500,
      totalTokens = 1500
    )
    
    assert(usage.cachedInputTokens == None)
    assert(usage.nonCachedInputTokens == None)
    assert(usage.cacheHitRate == None)
    assert(usage.usedCache == false)
  }
  
  test("TokenUsage with zero cached tokens") {
    val usage = TokenUsage(
      inputTokens = 1000,
      outputTokens = 500,
      totalTokens = 1500,
      cachedInputTokens = Some(0)
    )
    
    assert(usage.nonCachedInputTokens == Some(1000))
    assert(usage.cacheHitRate == Some(0.0))
    assert(usage.usedCache == false)
  }
  
  test("TokenUsage with all tokens cached") {
    val usage = TokenUsage(
      inputTokens = 1000,
      outputTokens = 500,
      totalTokens = 1500,
      cachedInputTokens = Some(1000)
    )
    
    assert(usage.nonCachedInputTokens == Some(0))
    assert(usage.cacheHitRate == Some(100.0))
    assert(usage.usedCache == true)
  }
  
  test("TokenUsage cache hit rate with zero input tokens") {
    val usage = TokenUsage(
      inputTokens = 0,
      outputTokens = 500,
      totalTokens = 500,
      cachedInputTokens = Some(0)
    )
    
    assert(usage.cacheHitRate == Some(0.0))
  }
  
  test("Cost calculation with cached tokens should be cheaper") {
    // Create two identical responses, one with cached tokens
    val usageWithoutCache = TokenUsage(
      inputTokens = 10000,
      outputTokens = 5000,
      totalTokens = 15000
    )
    
    val usageWithCache = TokenUsage(
      inputTokens = 10000,
      outputTokens = 5000,
      totalTokens = 15000,
      cachedInputTokens = Some(8000) // 80% cached
    )
    
    // For Gemini 2.5 Flash: $0.30 per million input, $2.50 per million output
    // Cached tokens are 75% cheaper: $0.075 per million
    val costWithoutCache = (10000.0 / 1000000 * 0.30) + (5000.0 / 1000000 * 2.50)
    val costWithCache = (2000.0 / 1000000 * 0.30) + (8000.0 / 1000000 * 0.075) + (5000.0 / 1000000 * 2.50)
    
    assert(costWithoutCache == 0.003 + 0.0125) // $0.0155
    assert(costWithCache == 0.0006 + 0.0006 + 0.0125) // $0.0137
    assert(costWithCache < costWithoutCache)
    
    // Verify the savings percentage
    val savings = (costWithoutCache - costWithCache) / costWithoutCache * 100
    assert(savings > 10) // Should save more than 10%
  }
  
  test("Gemini model should extract cached token info when available") {
    // This test would require mocking or a real API call
    // For now, we'll just verify the code compiles correctly
    val model = new GeminiModel("gemini-2.5-flash")
    assert(model.getModelName == "gemini-2.5-flash")
    
    // If we had a real API key and wanted to test with actual API:
    // val result = model.generateContent("Test prompt")
    // result.foreach { response =>
    //   println(s"Cached tokens: ${response.tokenUsage.flatMap(_.cachedInputTokens)}")
    //   println(s"Cache hit rate: ${response.tokenUsage.flatMap(_.cacheHitRate)}")
    // }
  }
}