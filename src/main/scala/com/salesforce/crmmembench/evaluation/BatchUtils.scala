package com.salesforce.crmmembench.evaluation

/**
 * Utilities for dividing test cases into balanced batches.
 * Ensures equal distribution across context sizes to prevent underrepresentation
 * of larger context sizes.
 * 
 * Within each batch, test cases are sorted by conversation count in descending order
 * to process larger contexts first, which can help with memory management and
 * early termination decisions.
 */
object BatchUtils {
  
  /**
   * Divides test cases into batches, ensuring equal representation of each context size.
   * 
   * @param testCases List of all test cases to be divided
   * @param numBatches Number of batches to create (default: 30)
   * @return List of batches, where each batch contains a proportional sample from each context size
   */
  def createBalancedBatches(testCases: List[TestCase], numBatches: Int = 30): List[List[TestCase]] = {
    require(numBatches > 0, "Number of batches must be positive")
    
    if (testCases.isEmpty) {
      return List.fill(numBatches)(List.empty[TestCase])
    }
    
    // Group test cases by context size
    val testCasesByContext = testCases.groupBy(_.conversationCount)
    
    // Initialize empty batches
    val batches = Array.fill(numBatches)(List.empty[TestCase])
    
    // For each context size, distribute test cases across batches
    testCasesByContext.foreach { case (contextSize, casesForContext) =>
      val totalCases = casesForContext.size
      val baseCasesPerBatch = totalCases / numBatches
      val remainder = totalCases % numBatches
      
      // Shuffle cases for this context to ensure randomness
      val shuffledCases = scala.util.Random.shuffle(casesForContext)
      
      var caseIndex = 0
      for (batchIndex <- 0 until numBatches) {
        // Calculate how many cases this batch should get
        // First 'remainder' batches get one extra case
        val casesForThisBatch = baseCasesPerBatch + (if (batchIndex < remainder) 1 else 0)
        
        // Take the appropriate number of cases for this batch
        val casesSlice = shuffledCases.slice(caseIndex, caseIndex + casesForThisBatch)
        batches(batchIndex) = batches(batchIndex) ++ casesSlice
        
        caseIndex += casesForThisBatch
      }
    }
    
    // Convert to list and sort within each batch by conversation count (descending)
    // This ensures larger contexts are processed first within each batch
    batches.map(batch => batch.sortBy(_.conversationCount)(Ordering[Int].reverse)).toList
  }
  
  /**
   * Validates that batches contain all original test cases without duplication.
   * Useful for testing.
   */
  def validateBatches(original: List[TestCase], batches: List[List[TestCase]]): Boolean = {
    val allBatchedCases = batches.flatten
    val originalSet = original.toSet
    val batchedSet = allBatchedCases.toSet
    
    // Check same number of cases
    if (original.size != allBatchedCases.size) return false
    
    // Check no duplicates in batches
    if (allBatchedCases.size != batchedSet.size) return false
    
    // Check same cases (no loss, no addition)
    originalSet == batchedSet
  }
  
  /**
   * Gets statistics about batch distribution for debugging/testing.
   */
  def getBatchStatistics(batches: List[List[TestCase]]): BatchStatistics = {
    val batchSizes = batches.map(_.size)
    val contextDistribution = batches.map { batch =>
      batch.groupBy(_.conversationCount).mapValues(_.size)
    }
    
    BatchStatistics(
      numBatches = batches.size,
      batchSizes = batchSizes,
      minBatchSize = if (batchSizes.isEmpty) 0 else batchSizes.min,
      maxBatchSize = if (batchSizes.isEmpty) 0 else batchSizes.max,
      contextDistribution = contextDistribution
    )
  }
  
  case class BatchStatistics(
    numBatches: Int,
    batchSizes: List[Int],
    minBatchSize: Int,
    maxBatchSize: Int,
    contextDistribution: List[Map[Int, Int]]
  ) {
    def isBalanced: Boolean = maxBatchSize - minBatchSize <= 1
    
    def contextSizeConsistency: Map[Int, Boolean] = {
      // For each context size, check if distribution across batches is consistent
      val allContextSizes = contextDistribution.flatMap(_.keys).distinct
      
      allContextSizes.map { contextSize =>
        val countsPerBatch = contextDistribution.map(_.getOrElse(contextSize, 0))
        val minCount = countsPerBatch.min
        val maxCount = countsPerBatch.max
        contextSize -> (maxCount - minCount <= 1)
      }.toMap
    }
  }
}