package com.salesforce.crmmembench.LLM_endpoints

import java.util.concurrent.{Semaphore, TimeUnit}
import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable
import scala.util.{Try, Failure, Success}

/**
 * Configuration for rate limiting
 * @param requestsPerMinute Maximum requests per minute
 * @param requestsPerSecond Maximum requests per second (optional, for burst control)
 * @param maxConcurrent Maximum concurrent requests
 * @param backoffMultiplier Multiplier for exponential backoff on 429 errors
 * @param maxBackoffSeconds Maximum backoff time in seconds
 * @param initialBackoffSeconds Initial backoff time in seconds on 429
 */
case class RateLimitConfig(
  requestsPerMinute: Int,
  requestsPerSecond: Option[Int] = None,
  maxConcurrent: Int = 10,
  backoffMultiplier: Double = 2.0,
  maxBackoffSeconds: Int = 60,
  initialBackoffSeconds: Int = 1
)

/**
 * Rate limiter implementation using token bucket algorithm
 */
class TokenBucket(tokensPerInterval: Int, intervalMillis: Long) {
  private val tokens = new AtomicLong(tokensPerInterval)
  private val lastRefill = new AtomicLong(System.currentTimeMillis())
  private val maxTokens = tokensPerInterval
  
  def tryAcquire(numTokens: Int = 1): Boolean = synchronized {
    refill()
    if (tokens.get() >= numTokens) {
      tokens.addAndGet(-numTokens)
      true
    } else {
      false
    }
  }
  
  def waitAndAcquire(numTokens: Int = 1): Unit = {
    while (!tryAcquire(numTokens)) {
      Thread.sleep(50) // Check every 50ms
    }
  }
  
  private def refill(): Unit = {
    val now = System.currentTimeMillis()
    val timeSinceRefill = now - lastRefill.get()
    
    if (timeSinceRefill >= intervalMillis) {
      val intervalsElapsed = timeSinceRefill / intervalMillis
      val tokensToAdd = intervalsElapsed * tokensPerInterval
      
      tokens.updateAndGet(current => Math.min(current + tokensToAdd, maxTokens))
      lastRefill.set(now)
    }
  }
}

/**
 * A sophisticated rate limiting wrapper for LLMModel that implements:
 * - Token bucket algorithm for smooth rate limiting
 * - Per-second and per-minute rate limits
 * - Concurrent request limiting
 * - Exponential backoff on 429 errors
 * - Request queuing and prioritization
 */
class RateLimitedLLMModel(
  underlying: LLMModel,
  val config: RateLimitConfig
) extends LLMModel {
  
  // Token buckets for rate limiting
  private val minuteBucket = new TokenBucket(config.requestsPerMinute, 60000)
  private val secondBucket = config.requestsPerSecond.map(rps => new TokenBucket(rps, 1000))
  
  // Semaphore for concurrent request limiting
  private val concurrentSemaphore = new Semaphore(config.maxConcurrent)
  
  // Track backoff state per thread
  private val threadBackoffs = mutable.Map[Long, Long]()
  
  // Statistics tracking
  private val totalRequests = new AtomicLong(0)
  private val successfulRequests = new AtomicLong(0)
  private val rateLimitedRequests = new AtomicLong(0)
  private val failedRequests = new AtomicLong(0)
  
  override def generateContent(prompt: String): Try[LLMResponse] = {
    val threadId = Thread.currentThread().getId
    
    // Check if this thread is in backoff
    checkBackoff(threadId)
    
    // Wait for rate limit tokens
    acquireRateLimitTokens()
    
    // Acquire concurrent execution permit
    concurrentSemaphore.acquire()
    
    try {
      totalRequests.incrementAndGet()
      
      underlying.generateContent(prompt) match {
        case Success(response) =>
          successfulRequests.incrementAndGet()
          clearBackoff(threadId)
          Success(response)
          
        case Failure(exception) =>
          failedRequests.incrementAndGet()
          
          // Check if this is a rate limit error
          if (isRateLimitError(exception)) {
            rateLimitedRequests.incrementAndGet()
            applyBackoff(threadId)
            
            // Log rate limit hit
            if (rateLimitedRequests.get() % 10 == 1) { // Log every 10th rate limit
              println(s"Rate limited by provider (${rateLimitedRequests.get()} total). Applying backoff.")
            }
          }
          
          Failure(exception)
      }
    } finally {
      concurrentSemaphore.release()
    }
  }
  
  private def acquireRateLimitTokens(): Unit = {
    // First check per-second limit if configured
    secondBucket.foreach(_.waitAndAcquire())
    
    // Then check per-minute limit
    minuteBucket.waitAndAcquire()
  }
  
  private def checkBackoff(threadId: Long): Unit = synchronized {
    threadBackoffs.get(threadId).foreach { backoffUntil =>
      val now = System.currentTimeMillis()
      if (now < backoffUntil) {
        val waitTime = backoffUntil - now
        Thread.sleep(waitTime)
      }
    }
  }
  
  private def applyBackoff(threadId: Long): Unit = synchronized {
    val currentBackoff = threadBackoffs.get(threadId).map { backoffUntil =>
      val lastBackoff = (backoffUntil - System.currentTimeMillis()) / 1000
      Math.max(config.initialBackoffSeconds, lastBackoff)
    }.getOrElse(config.initialBackoffSeconds.toLong)
    
    val nextBackoff = Math.min(
      (currentBackoff * config.backoffMultiplier).toLong,
      config.maxBackoffSeconds.toLong
    )
    
    threadBackoffs(threadId) = System.currentTimeMillis() + (nextBackoff * 1000)
  }
  
  private def clearBackoff(threadId: Long): Unit = synchronized {
    threadBackoffs.remove(threadId)
  }
  
  private def isRateLimitError(exception: Throwable): Boolean = {
    exception.getMessage match {
      case msg if msg == null => false
      case msg => 
        msg.contains("429") || 
        msg.contains("Resource exhausted") ||
        msg.contains("rate limit") ||
        msg.toLowerCase.contains("too many requests")
    }
  }
  
  override def getModelName: String = underlying.getModelName
  
  override def getProvider: String = underlying.getProvider
  
  /**
   * Get current statistics for monitoring
   */
  def getStats: RateLimitStats = RateLimitStats(
    totalRequests = totalRequests.get(),
    successfulRequests = successfulRequests.get(),
    rateLimitedRequests = rateLimitedRequests.get(),
    failedRequests = failedRequests.get(),
    currentBackoffs = threadBackoffs.size
  )
}

/**
 * Statistics for rate limiter monitoring
 */
case class RateLimitStats(
  totalRequests: Long,
  successfulRequests: Long,
  rateLimitedRequests: Long,
  failedRequests: Long,
  currentBackoffs: Int
) {
  def successRate: Double = 
    if (totalRequests > 0) successfulRequests.toDouble / totalRequests * 100 else 0.0
    
  def rateLimitRate: Double = 
    if (totalRequests > 0) rateLimitedRequests.toDouble / totalRequests * 100 else 0.0
    
  override def toString: String = {
    f"RateLimitStats(total=$totalRequests, success=$successfulRequests (${successRate}%.1f%%), " +
    f"rateLimited=$rateLimitedRequests (${rateLimitRate}%.1f%%), failed=$failedRequests, activeBackoffs=$currentBackoffs)"
  }
}

object RateLimitedLLMModel {
  /**
   * Default rate limit configurations for different providers
   */
  object DefaultConfigs {
    // Gemini rate limits (conservative defaults)
    val geminiFlashLite = RateLimitConfig(
      requestsPerMinute = 1000,
      requestsPerSecond = Some(15),
      maxConcurrent = 10
    )
    
    val geminiFlash = RateLimitConfig(
      requestsPerMinute = 500,
      requestsPerSecond = Some(10),
      maxConcurrent = 10
    )
    
    val geminiPro = RateLimitConfig(
      requestsPerMinute = 300,
      requestsPerSecond = Some(5),
      maxConcurrent = 5
    )
    
    // OpenAI rate limits
    val gpt4 = RateLimitConfig(
      requestsPerMinute = 500,
      requestsPerSecond = Some(10),
      maxConcurrent = 10
    )
    
    val gpt35 = RateLimitConfig(
      requestsPerMinute = 3000,
      requestsPerSecond = Some(50),
      maxConcurrent = 20
    )
    
    // Claude rate limits
    val claude = RateLimitConfig(
      requestsPerMinute = 400,
      requestsPerSecond = Some(8),
      maxConcurrent = 8
    )
  }
  
  /**
   * Create a rate limited model with default config based on model name
   */
  def withDefaultConfig(model: LLMModel): RateLimitedLLMModel = {
    val config = model.getModelName match {
      case name if name.contains("gemini-2.5-flash-lite") => DefaultConfigs.geminiFlashLite
      case name if name.contains("gemini-2.5-flash") => DefaultConfigs.geminiFlash
      case name if name.contains("gemini-2.5-pro") => DefaultConfigs.geminiPro
      case name if name.contains("gpt-4") => DefaultConfigs.gpt4
      case name if name.contains("gpt-3.5") => DefaultConfigs.gpt35
      case name if name.contains("claude") => DefaultConfigs.claude
      case _ => RateLimitConfig(requestsPerMinute = 300, maxConcurrent = 5)
    }
    new RateLimitedLLMModel(model, config)
  }
  
  /**
   * Create a rate limited model with custom config
   */
  def withConfig(model: LLMModel, config: RateLimitConfig): RateLimitedLLMModel = {
    new RateLimitedLLMModel(model, config)
  }
}