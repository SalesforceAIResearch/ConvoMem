package com.salesforce.crmmembench

import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent.TrieMap

/**
 * Global cost tracking for all LLM inference calls.
 * Thread-safe implementation using atomic operations.
 */
object CostTracker {
  // Total cost in cents (to avoid floating point precision issues)
  val totalCostCents = new AtomicLong(0L)
  
  // Token tracking
  val totalInputTokens = new AtomicLong(0L)
  val totalOutputTokens = new AtomicLong(0L)
  
  // Call count
  val totalCalls = new AtomicInteger(0)
  
  // Cost breakdown by model
  val costByModel = new ConcurrentHashMap[String, AtomicLong]()
  
  // Token breakdown by model
  val inputTokensByModel = new ConcurrentHashMap[String, AtomicLong]()
  val outputTokensByModel = new ConcurrentHashMap[String, AtomicLong]()
  
  /**
   * Record a model inference call with its cost and token usage.
   * 
   * @param modelName Name of the model
   * @param cost Cost in dollars
   * @param inputTokens Number of input tokens (optional)
   * @param outputTokens Number of output tokens (optional)
   */
  def recordInference(
    modelName: String, 
    cost: Double,
    inputTokens: Option[Int] = None,
    outputTokens: Option[Int] = None
  ): Unit = {
    // Convert cost to cents for atomic operations
    val costCents = (cost * 100).toLong
    
    // Update totals
    totalCostCents.addAndGet(costCents)
    totalCalls.incrementAndGet()
    
    inputTokens.foreach(tokens => totalInputTokens.addAndGet(tokens))
    outputTokens.foreach(tokens => totalOutputTokens.addAndGet(tokens))
    
    // Update per-model stats
    costByModel.computeIfAbsent(modelName, _ => new AtomicLong(0L)).addAndGet(costCents)
    
    inputTokens.foreach { tokens =>
      inputTokensByModel.computeIfAbsent(modelName, _ => new AtomicLong(0L)).addAndGet(tokens)
    }
    
    outputTokens.foreach { tokens =>
      outputTokensByModel.computeIfAbsent(modelName, _ => new AtomicLong(0L)).addAndGet(tokens)
    }
  }
  
  /**
   * Get the total cost in dollars.
   */
  def getTotalCost: Double = totalCostCents.get() / 100.0
  
  /**
   * Get the total number of input tokens.
   */
  def getTotalInputTokens: Long = totalInputTokens.get()
  
  /**
   * Get the total number of output tokens.
   */
  def getTotalOutputTokens: Long = totalOutputTokens.get()
  
  /**
   * Get the total number of inference calls.
   */
  def getTotalCalls: Int = totalCalls.get()
  
  /**
   * Get cost breakdown by model.
   * @return Map of model name to cost in dollars
   */
  def getCostByModel: Map[String, Double] = {
    import scala.collection.JavaConverters._
    costByModel.asScala.map { case (model, cents) =>
      model -> (cents.get() / 100.0)
    }.toMap
  }
  
  /**
   * Get a formatted summary of costs.
   */
  def getSummary: String = {
    val total = getTotalCost
    val calls = getTotalCalls
    val avgCostPerCall = if (calls > 0) total / calls else 0.0
    
    val modelBreakdown = getCostByModel.toList.sortBy(-_._2).map { case (model, cost) =>
      f"  $model%-30s: $$${cost}%.4f"
    }.mkString("\n")
    
    f"""Cost Tracking Summary:
       |Total Cost: $$${total}%.4f
       |Total Calls: $calls
       |Average Cost per Call: $$${avgCostPerCall}%.4f
       |Total Input Tokens: ${getTotalInputTokens}%,d
       |Total Output Tokens: ${getTotalOutputTokens}%,d
       |
       |Cost by Model:
       |$modelBreakdown""".stripMargin
  }
  
  /**
   * Reset all tracking data.
   * Useful for testing or starting a new evaluation run.
   */
  def reset(): Unit = {
    totalCostCents.set(0L)
    totalInputTokens.set(0L)
    totalOutputTokens.set(0L)
    totalCalls.set(0)
    costByModel.clear()
    inputTokensByModel.clear()
    outputTokensByModel.clear()
  }
  
  /**
   * Calculate projected total cost based on current progress.
   * 
   * @param completedItems Number of items completed so far
   * @param totalItems Total number of items to process
   * @return Projected total cost in dollars
   */
  def getProjectedCost(completedItems: Int, totalItems: Int): Double = {
    if (completedItems <= 0 || totalItems <= 0) return 0.0
    
    val currentCost = getTotalCost
    val progressRatio = completedItems.toDouble / totalItems
    
    if (progressRatio > 0) currentCost / progressRatio else 0.0
  }
}