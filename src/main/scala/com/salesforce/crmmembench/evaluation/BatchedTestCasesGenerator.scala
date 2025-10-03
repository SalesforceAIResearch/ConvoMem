package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, EvidenceGenerator, Conversation}
import scala.util.Random

/**
 * Test cases generator that batches multiple evidence items into single test cases.
 * 
 * This generator is optimized for memory systems like mem0 that have high indexing costs.
 * Instead of creating one test case per evidence item, it batches multiple evidence items
 * together, allowing the evaluator to index once and ask multiple questions.
 * 
 * Key requirements:
 * 1. Each evidence item must appear in EVERY context size
 * 2. Evidence items are batched together (up to maxEvidencePerBatch)
 * 3. Evidence conversations from different items are mixed while preserving order within each
 * 4. For large contexts, supplement with irrelevant conversations
 * 5. Skip evidence items that are too large for the context
 * 
 * IMPORTANT: If a batch's total evidence conversations exceed the requested context size,
 * ALL evidence conversations are still included. This is intentional - evidence completeness
 * is critical for valid tests. The context size is a target for dilution testing, not a hard limit.
 * 
 * @param evidenceGenerator The evidence generator to load evidence items from
 * @param contextSizes List of context sizes to generate test cases for
 * @param maxEvidencePerBatch Maximum number of evidence items per test case (default: 100)
 * @param minTestCasesPerContext Minimum number of test cases per context size (default: 10)
 */
class BatchedTestCasesGenerator(
  evidenceGenerator: EvidenceGenerator,
  contextSizes: List[Int] = Config.Evaluation.CONTEXT_SIZES,
  maxEvidencePerBatch: Int = 100,
  minTestCasesPerContext: Int = 10
) extends TestCasesGenerator(evidenceGenerator) {
  
  override def generatorClassType: String = "batched"

  override def generateTestCases(): List[TestCase] = {
    println(s"\nGenerating batched test cases with up to $maxEvidencePerBatch evidence items per batch...")
    
    // Load evidence items and irrelevant conversations
    val allEvidenceItems = evidenceGenerator.loadEvidenceItems()
    val irrelevantConversationsByPerson = ConversationLoader.loadIrrelevantConversations()
    
    println(s"Loaded ${allEvidenceItems.length} evidence items")
    val totalIrrelevantConversations = irrelevantConversationsByPerson.values.map(_.size).sum
    println(s"Loaded $totalIrrelevantConversations irrelevant conversations for ${irrelevantConversationsByPerson.size} persons")
    
    // Use parent trait's method to adjust context sizes
    val adjustedContextSizes = adjustContextSizesForEvidence(contextSizes, allEvidenceItems)
    
    // Generate test cases for each context size
    val allTestCases = adjustedContextSizes.flatMap { contextSize =>
      generateForContextSize(contextSize, allEvidenceItems, irrelevantConversationsByPerson)
    }
    
    // Verify each evidence item appears in each context size
    verifyEvidenceDistribution(allTestCases, allEvidenceItems, adjustedContextSizes)
    
    // Print statistics
    printStatistics(allTestCases)
    
    // Print evidence items per context size statistics
    printEvidencePerContextStats(allTestCases)
    
    allTestCases
  }
  
  def generateForContextSize(
    contextSize: Int,
    allEvidenceItems: List[EvidenceItem],
    irrelevantConversationsByPerson: Map[String, List[Conversation]]
  ): List[TestCase] = {
    
    // Filter out evidence items that are too large for this context
    val viableEvidence = allEvidenceItems.filter(_.conversations.length <= contextSize)
    
    if (viableEvidence.isEmpty) {
      if (Config.DEBUG) {
        println(s"No evidence items fit in context size $contextSize")
      }
      return List.empty
    }
    
    // Shuffle evidence for variety
    val shuffledEvidence = Random.shuffle(viableEvidence)
    
    // Create batches
    val batches = createBatches(shuffledEvidence, contextSize, maxEvidencePerBatch)
    
    if (contextSize == 70 || Config.DEBUG) {
      println(s"\nGenerating test cases for context size $contextSize:")
      println(s"  Total evidence items available: ${viableEvidence.length}")
      println(s"  Max evidence per batch: $maxEvidencePerBatch")
      batches.foreach { batch =>
        val convCount = batch.map(_.conversations.length).sum
        println(s"  Creating test case with ${batch.length} evidence items ($convCount conversations)")
      }
      println(s"  Total test cases created for context $contextSize: ${batches.length}")
      println(s"  Total evidence items used: ${batches.map(_.length).sum}")
    }
    
    // Convert batches to test cases
    var testCases = batches.map { batch =>
      createTestCase(batch, contextSize, irrelevantConversationsByPerson)
    }
    
    // Ensure minimum test cases per context size
    if (testCases.length < minTestCasesPerContext && viableEvidence.nonEmpty) {
      val additionalNeeded = minTestCasesPerContext - testCases.length
      if (Config.DEBUG || contextSize >= 100) {
        println(s"  Creating $additionalNeeded additional test cases to meet minimum of $minTestCasesPerContext")
      }
      
      // Create additional test cases by reusing evidence with different irrelevant conversations
      val additionalTestCases = (0 until additionalNeeded).map { _ =>
        // Take evidence items in a round-robin fashion
        val startIdx = (testCases.length * maxEvidencePerBatch) % viableEvidence.length
        val evidenceForBatch = viableEvidence.drop(startIdx).take(maxEvidencePerBatch) ++
                              viableEvidence.take(Math.max(0, maxEvidencePerBatch - (viableEvidence.length - startIdx)))
        
        // Create test case with different random irrelevant conversations
        createTestCase(evidenceForBatch.take(Math.min(evidenceForBatch.length, maxEvidencePerBatch)), 
                      contextSize, irrelevantConversationsByPerson)
      }.toList
      
      testCases = testCases ++ additionalTestCases
    }
    
    testCases
  }
  
  def createBatches(
    evidenceItems: List[EvidenceItem],
    contextSize: Int,
    maxPerBatch: Int
  ): List[List[EvidenceItem]] = {
    val batches = scala.collection.mutable.ListBuffer[List[EvidenceItem]]()
    val currentBatch = scala.collection.mutable.ListBuffer[EvidenceItem]()
    var currentConvCount = 0
    
    for (evidence <- evidenceItems) {
      val evidenceConvCount = evidence.conversations.length
      
      // Check if adding this evidence would exceed limits
      // IMPORTANT: If a single evidence item has more conversations than contextSize,
      // it will still be included in its own batch. This is EXPECTED behavior - we
      // never truncate evidence conversations as they are all required for valid tests.
      val wouldExceedContext = currentConvCount + evidenceConvCount > contextSize
      val wouldExceedBatchSize = currentBatch.length >= maxPerBatch
      
      // If current batch is not empty and would exceed limits, finalize it
      if (currentBatch.nonEmpty && (wouldExceedContext || wouldExceedBatchSize)) {
        batches += currentBatch.toList
        currentBatch.clear()
        currentConvCount = 0
      }
      
      // Add evidence to current batch
      currentBatch += evidence
      currentConvCount += evidenceConvCount
    }
    
    // Don't forget the last batch
    if (currentBatch.nonEmpty) {
      batches += currentBatch.toList
    }
    
    batches.toList
  }
  
  def createTestCase(
    evidenceItems: List[EvidenceItem],
    contextSize: Int,
    irrelevantConversationsByPerson: Map[String, List[Conversation]]
  ): TestCase = {
    // Get all evidence conversations
    val evidenceConversationGroups = evidenceItems.map(_.conversations)
    val totalEvidenceConversations = evidenceConversationGroups.flatten.length
    
    // Calculate how many irrelevant conversations we need
    // IMPORTANT: If totalEvidenceConversations > contextSize, irrelevantNeeded will be negative.
    // This is EXPECTED and DESIRED - we include all evidence conversations even if they exceed
    // the context size. The context size is a target for dilution testing, not a hard limit.
    val irrelevantNeeded = contextSize - totalEvidenceConversations
    
    // Group evidence items by person to select appropriate irrelevant conversations
    val evidenceByPerson = evidenceItems.groupBy(_.personId)
    
    // Select irrelevant conversations proportionally from each person
    // Note: When irrelevantNeeded <= 0 (evidence exceeds context size), we correctly
    // select no irrelevant conversations, preserving all evidence conversations
    val selectedIrrelevant = if (irrelevantNeeded > 0) {
      selectIrrelevantConversations(evidenceByPerson, irrelevantConversationsByPerson, irrelevantNeeded)
    } else {
      List.empty
    }
    
    // Mix evidence conversations with irrelevant ones
    val mixedConversations = mixConversations(evidenceConversationGroups, selectedIrrelevant)
    
    TestCase(
      evidenceItems = evidenceItems,
      conversations = mixedConversations,
      contextSize = Some(contextSize)
    )
  }
  
  def selectIrrelevantConversations(
    evidenceByPerson: Map[Option[String], List[EvidenceItem]],
    irrelevantConversationsByPerson: Map[String, List[Conversation]],
    totalNeeded: Int
  ): List[Conversation] = {
    val selectedConversations = scala.collection.mutable.ListBuffer[Conversation]()
    
    // Handle evidence items with person IDs
    val personsWithEvidence = evidenceByPerson.collect { 
      case (Some(personId), items) => (personId, items)
    }
    
    if (personsWithEvidence.nonEmpty) {
      // Calculate proportional allocation per person
      val totalEvidenceItems = personsWithEvidence.values.map(_.length).sum
      val remainingNeeded = totalNeeded
      
      personsWithEvidence.foreach { case (personId, items) =>
        val proportion = items.length.toDouble / totalEvidenceItems
        val neededForPerson = Math.max(1, (totalNeeded * proportion).toInt)
        
        val availableConversations = irrelevantConversationsByPerson.getOrElse(personId, List.empty)
        if (availableConversations.nonEmpty) {
          val selected = Random.shuffle(availableConversations).take(neededForPerson)
          selectedConversations ++= selected
        }
      }
    }
    
    // Handle legacy evidence items without person IDs
    val itemsWithoutPersonId = evidenceByPerson.get(None).getOrElse(List.empty)
    if (itemsWithoutPersonId.nonEmpty && selectedConversations.length < totalNeeded) {
      // For legacy items, pick from a random person
      if (irrelevantConversationsByPerson.nonEmpty) {
        val randomPerson = Random.shuffle(irrelevantConversationsByPerson.keys.toList).head
        val availableConversations = irrelevantConversationsByPerson(randomPerson)
        val needed = totalNeeded - selectedConversations.length
        selectedConversations ++= Random.shuffle(availableConversations).take(needed)
      }
    }
    
    // If we still don't have enough, fill from any available person
    if (selectedConversations.length < totalNeeded && irrelevantConversationsByPerson.nonEmpty) {
      val allAvailable = irrelevantConversationsByPerson.values.flatten.toList
      val stillNeeded = totalNeeded - selectedConversations.length
      selectedConversations ++= Random.shuffle(allAvailable).take(stillNeeded)
    }
    
    selectedConversations.toList.take(totalNeeded)
  }
  
  def mixConversations(
    evidenceGroups: List[List[Conversation]],
    irrelevantConversations: List[Conversation]
  ): List[Conversation] = {
    val allEvidenceConversations = evidenceGroups.flatten
    val totalConversations = allEvidenceConversations.length + irrelevantConversations.length
    
    if (evidenceGroups.isEmpty || allEvidenceConversations.isEmpty) {
      return irrelevantConversations
    }
    
    if (irrelevantConversations.isEmpty) {
      return allEvidenceConversations
    }
    
    // Create a result array
    val result = Array.ofDim[Conversation](totalConversations)
    
    // Determine positions for each evidence group
    // We want to spread them out while maintaining order within groups
    val positionsPerGroup = distributePositions(evidenceGroups, totalConversations)
    
    // Place evidence conversations at their assigned positions
    evidenceGroups.zip(positionsPerGroup).foreach { case (group, positions) =>
      group.zip(positions).foreach { case (conv, pos) =>
        result(pos) = conv
      }
    }
    
    // Fill remaining positions with irrelevant conversations
    var irrelevantIdx = 0
    for (i <- result.indices) {
      if (result(i) == null) {
        result(i) = irrelevantConversations(irrelevantIdx)
        irrelevantIdx += 1
      }
    }
    
    result.toList
  }
  
  def distributePositions(
    evidenceGroups: List[List[Conversation]],
    totalPositions: Int
  ): List[List[Int]] = {
    val totalEvidence = evidenceGroups.map(_.length).sum
    
    // Generate random positions for all evidence conversations
    val allPositions = Random.shuffle((0 until totalPositions).toList).take(totalEvidence)
    
    // Distribute positions to groups while maintaining order within each group
    var positionIdx = 0
    evidenceGroups.map { group =>
      val groupSize = group.length
      val groupPositions = allPositions.slice(positionIdx, positionIdx + groupSize).sorted
      positionIdx += groupSize
      groupPositions
    }
  }
  
  def verifyEvidenceDistribution(
    testCases: List[TestCase],
    allEvidenceItems: List[EvidenceItem],
    contextSizes: List[Int]
  ): Unit = {
    // Count how many times each evidence item appears
    val evidenceUsageCount = scala.collection.mutable.Map[EvidenceItem, Int]().withDefaultValue(0)
    
    testCases.foreach { testCase =>
      testCase.evidenceItems.foreach { evidence =>
        evidenceUsageCount(evidence) += 1
      }
    }
    
    // Check which evidence items don't appear in all context sizes
    val problemEvidence = allEvidenceItems.filter { evidence =>
      val usageCount = evidenceUsageCount(evidence)
      // Evidence should appear once per context size where it fits
      val expectedCount = contextSizes.count(_ >= evidence.conversations.length)
      usageCount != expectedCount
    }
    
    if (problemEvidence.nonEmpty) {
      println(s"WARNING: ${problemEvidence.length} evidence items were not used in all context sizes:")
      problemEvidence.take(5).foreach { e =>
        val used = evidenceUsageCount(e)
        val expected = contextSizes.count(_ >= e.conversations.length)
        println(s"  - ${e.question} (used $used times, expected $expected)")
      }
    }
  }
  
  def printStatistics(testCases: List[TestCase]): Unit = {
    println(s"\nGenerated ${testCases.length} batched test cases")
    
    // Evidence items per test case distribution
    val distribution = testCases.groupBy(_.evidenceItems.length).toSeq.sortBy(_._1)
    println(s"Evidence items per test case distribution:")
    distribution.foreach { case (count, cases) =>
      println(s"  $count evidence items: ${cases.length} test cases")
    }
  }
  
  def printEvidencePerContextStats(testCases: List[TestCase]): Unit = {
    // Group test cases by context size
    val byContext = testCases.groupBy(_.conversationCount).toSeq.sortBy(_._1)
    
    println(s"\nEvidence items per context size statistics:")
    byContext.foreach { case (contextSize, cases) =>
      if (cases.nonEmpty) {
        val evidenceCounts = cases.map(_.evidenceItems.size)
        val avg = evidenceCounts.sum.toDouble / evidenceCounts.size
        val min = evidenceCounts.min
        val max = evidenceCounts.max
        
        val testCaseCount = cases.length
        val minIndicator = if (testCaseCount >= minTestCasesPerContext) "" else s" (min $minTestCasesPerContext required)"
        
        println(f"  Context size $contextSize%3d: $testCaseCount%3d test cases$minIndicator, avg $avg%5.1f evidence items/test case (min: $min%2d, max: $max%2d)")
      }
    }
  }
}