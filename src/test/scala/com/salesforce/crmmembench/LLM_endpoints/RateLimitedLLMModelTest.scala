package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Success, Failure, Try}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CountDownLatch, Executors}
import scala.collection.mutable.ListBuffer

class RateLimitedLLMModelTest extends AnyFlatSpec with Matchers {
  
  // Mock LLM model for testing
  class MockLLMModel(
    var failWithRateLimit: Boolean = false,
    var responseDelay: Long = 0
  ) extends LLMModel {
    val requestCount = new AtomicInteger(0)
    val concurrentRequests = new AtomicInteger(0)
    val maxConcurrentSeen = new AtomicInteger(0)
    
    override def generateContent(prompt: String): Try[LLMResponse] = {
      requestCount.incrementAndGet()
      
      val current = concurrentRequests.incrementAndGet()
      maxConcurrentSeen.updateAndGet(max => Math.max(max, current))
      
      try {
        if (responseDelay > 0) Thread.sleep(responseDelay)
        
        if (failWithRateLimit) {
          Failure(new RuntimeException("429 . Resource exhausted. Please try again later."))
        } else {
          Success(LLMResponse(s"Response to: $prompt", "mock-model"))
        }
      } finally {
        concurrentRequests.decrementAndGet()
      }
    }
    
    override def getModelName: String = "mock-model"
    override def getProvider: String = "mock"
  }
  
  "RateLimitedLLMModel" should "limit requests per minute" in {
    val mockModel = new MockLLMModel()
    val config = RateLimitConfig(
      requestsPerMinute = 60,
      requestsPerSecond = None,
      maxConcurrent = 100
    )
    val rateLimited = new RateLimitedLLMModel(mockModel, config)
    
    val startTime = System.currentTimeMillis()
    
    // Try to make 10 requests quickly
    for (i <- 1 to 10) {
      rateLimited.generateContent(s"Test $i")
    }
    
    val elapsedTime = System.currentTimeMillis() - startTime
    
    // With 60 requests per minute, 10 requests should complete quickly
    elapsedTime should be < 2000L
    mockModel.requestCount.get() shouldBe 10
  }
  
  it should "limit requests per second when configured" in {
    val mockModel = new MockLLMModel()
    val config = RateLimitConfig(
      requestsPerMinute = 600,
      requestsPerSecond = Some(2), // 2 requests per second
      maxConcurrent = 100
    )
    val rateLimited = new RateLimitedLLMModel(mockModel, config)
    
    val startTime = System.currentTimeMillis()
    
    // Try to make 5 requests quickly
    for (i <- 1 to 5) {
      rateLimited.generateContent(s"Test $i")
    }
    
    val elapsedTime = System.currentTimeMillis() - startTime
    
    // With 2 requests per second, 5 requests should take at least 2 seconds
    elapsedTime should be >= 2000L
    mockModel.requestCount.get() shouldBe 5
  }
  
  it should "limit concurrent requests" in {
    val mockModel = new MockLLMModel(responseDelay = 100) // Add delay to ensure concurrency
    val config = RateLimitConfig(
      requestsPerMinute = 1000,
      requestsPerSecond = None,
      maxConcurrent = 3
    )
    val rateLimited = new RateLimitedLLMModel(mockModel, config)
    
    val executor = Executors.newFixedThreadPool(10)
    val latch = new CountDownLatch(10)
    
    // Submit 10 concurrent requests
    for (i <- 1 to 10) {
      executor.submit(new Runnable {
        def run(): Unit = {
          try {
            rateLimited.generateContent(s"Test $i")
          } finally {
            latch.countDown()
          }
        }
      })
    }
    
    latch.await()
    executor.shutdown()
    
    // Max concurrent should not exceed 3
    mockModel.maxConcurrentSeen.get() should be <= 3
    mockModel.requestCount.get() shouldBe 10
  }
  
  it should "apply exponential backoff on 429 errors" in {
    val mockModel = new MockLLMModel(failWithRateLimit = true)
    val config = RateLimitConfig(
      requestsPerMinute = 1000,
      maxConcurrent = 10,
      initialBackoffSeconds = 1,
      backoffMultiplier = 2.0,
      maxBackoffSeconds = 4
    )
    val rateLimited = new RateLimitedLLMModel(mockModel, config)
    
    val startTime = System.currentTimeMillis()
    
    // First request should fail immediately
    rateLimited.generateContent("Test 1") match {
      case Failure(e) => e.getMessage should include("429")
      case Success(_) => fail("Expected failure")
    }
    
    // Second request should be delayed by backoff
    val beforeSecond = System.currentTimeMillis()
    rateLimited.generateContent("Test 2") match {
      case Failure(e) => e.getMessage should include("429")
      case Success(_) => fail("Expected failure")
    }
    val afterSecond = System.currentTimeMillis()
    
    // Should have waited at least 1 second (initial backoff)
    (afterSecond - beforeSecond) should be >= 1000L
    
    // Clear the rate limit error for next test
    mockModel.failWithRateLimit = false
    
    // Next successful request should clear backoff
    rateLimited.generateContent("Test 3") match {
      case Success(response) => response.content should include("Test 3")
      case Failure(e) => fail(s"Expected success, got: ${e.getMessage}")
    }
  }
  
  it should "track statistics correctly" in {
    val mockModel = new MockLLMModel()
    val config = RateLimitConfig(
      requestsPerMinute = 1000,
      maxConcurrent = 10
    )
    val rateLimited = new RateLimitedLLMModel(mockModel, config)
    
    // Make some successful requests
    for (i <- 1 to 5) {
      rateLimited.generateContent(s"Test $i")
    }
    
    // Make some failed requests
    mockModel.failWithRateLimit = true
    for (i <- 6 to 7) {
      rateLimited.generateContent(s"Test $i")
    }
    
    val stats = rateLimited.getStats
    stats.totalRequests shouldBe 7
    stats.successfulRequests shouldBe 5
    stats.rateLimitedRequests shouldBe 2
    stats.successRate shouldBe (5.0 / 7.0 * 100) +- 0.1
    stats.rateLimitRate shouldBe (2.0 / 7.0 * 100) +- 0.1
  }
  
  it should "use correct default configs for different models" in {
    // Test Gemini Flash Lite config
    val flashLiteModel = new MockLLMModel() {
      override def getModelName = "gemini-2.5-flash-lite"
    }
    val flashLiteRateLimited = RateLimitedLLMModel.withDefaultConfig(flashLiteModel)
    flashLiteRateLimited.config.requestsPerMinute shouldBe 1000
    flashLiteRateLimited.config.requestsPerSecond shouldBe Some(15)
    
    // Test Gemini Pro config
    val proModel = new MockLLMModel() {
      override def getModelName = "gemini-2.5-pro"
    }
    val proRateLimited = RateLimitedLLMModel.withDefaultConfig(proModel)
    proRateLimited.config.requestsPerMinute shouldBe 300
    proRateLimited.config.requestsPerSecond shouldBe Some(5)
    
    // Test GPT-4 config
    val gpt4Model = new MockLLMModel() {
      override def getModelName = "gpt-4o"
    }
    val gpt4RateLimited = RateLimitedLLMModel.withDefaultConfig(gpt4Model)
    gpt4RateLimited.config.requestsPerMinute shouldBe 500
    gpt4RateLimited.config.requestsPerSecond shouldBe Some(10)
  }
}