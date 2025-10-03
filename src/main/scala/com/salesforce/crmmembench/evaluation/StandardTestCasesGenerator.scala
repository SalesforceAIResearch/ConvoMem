package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, EvidenceGenerator, Conversation}
import scala.util.Random
import java.util.UUID

/**
 * Standard test cases generator that implements the current evaluation pattern.
 * 
 * This generator creates test cases where:
 * - Each evidence item is tested individually
 * - Evidence conversations are mixed with varying amounts of irrelevant conversations
 * - The total context size is controlled by the contextSizes parameter
 * 
 * IMPORTANT: If an evidence item contains more conversations than the requested context size,
 * ALL evidence conversations are still included. This is intentional - evidence completeness
 * is critical for valid tests. The context size is a target for dilution testing, not a hard limit.
 * 
 * @param evidenceGenerator The evidence generator to load evidence items
 * @param contextSizes List of context sizes to test (e.g., [1, 5, 10, 20])
 */
class StandardTestCasesGenerator(
  evidenceGenerator: EvidenceGenerator,
  contextSizes: List[Int]
) extends TestCasesGenerator(evidenceGenerator) {
  
  override def generatorType: String = s"Standard ${evidenceGenerator.getEvidenceTypeName} Generator"
  
  override def generatorClassType: String = "standard"
  
  override def generateTestCases(): List[TestCase] = {
    println(s"Loading ${evidenceGenerator.getEvidenceTypeName.toLowerCase} evidence data...")
    
    val evidenceItems = evidenceGenerator.loadEvidenceItems()
    val irrelevantConversationsByPerson = ConversationLoader.loadIrrelevantConversations()
    
    println(f"Loaded ${evidenceItems.size} ${evidenceGenerator.getEvidenceTypeName.toLowerCase} evidence items")
    
    // Debug: Check evidence conversation counts
    val evidenceConvCounts = evidenceItems.map(_.conversations.length).groupBy(identity).mapValues(_.size)
    println(s"Evidence items by conversation count: ${evidenceConvCounts.toSeq.sortBy(_._1).map { case (count, items) => s"$count convs: $items items" }.mkString(", ")}")
    val totalIrrelevantConversations = irrelevantConversationsByPerson.values.map(_.size).sum
    println(f"Loaded $totalIrrelevantConversations irrelevant conversations for ${irrelevantConversationsByPerson.size} persons")
    
    // Use parent trait's method to adjust context sizes
    val adjustedContextSizes = adjustContextSizesForEvidence(contextSizes, evidenceItems)
    
    println(s"Expected test cases: ${evidenceItems.size} evidence items × ${adjustedContextSizes.length} context sizes = ${evidenceItems.size * adjustedContextSizes.length}")
    
    // Generate test cases for each combination of evidence item and context size
    val testCases = for {
      evidenceItem <- evidenceItems
      contextSize <- adjustedContextSizes
    } yield generateTestCase(evidenceItem, contextSize, irrelevantConversationsByPerson)
    
    println(f"Generated ${testCases.size} test cases")
    
    // Log test case distribution by context size
    val testCasesByContextSize = testCases.groupBy(_.conversationCount)
    println(s"\nTest cases by context size:")
    testCasesByContextSize.toSeq.sortBy(_._1).foreach { case (contextSize, cases) =>
      println(f"  Context size $contextSize%3d: ${cases.size}%4d test cases")
    }
    println(s"Total unique context sizes: ${testCasesByContextSize.size}")
    
    testCases
  }
  
  /**
   * Generate a single test case for an evidence item with a specific context size.
   * 
   * @param evidenceItem The evidence item to test
   * @param contextSize The total number of conversations to include
   * @param irrelevantConversationsByPerson Map of person ID to irrelevant conversations
   * @return A test case
   */
  def generateTestCase(
    evidenceItem: EvidenceItem,
    contextSize: Int,
    irrelevantConversationsByPerson: Map[String, List[Conversation]]
  ): TestCase = {
    // Get evidence conversations and ensure they have IDs and containsEvidence flag
    val evidenceConversations = evidenceItem.conversations.map { conv =>
      conv.copy(
        id = conv.id.orElse(Some(s"evidence-${UUID.randomUUID()}")),
        containsEvidence = Some(true)
      )
    }
    
    val evidenceCount = evidenceConversations.length
    // IMPORTANT: If contextSize < evidenceCount, irrelevantCount will be negative.
    // This is EXPECTED and DESIRED behavior - we always include ALL evidence conversations
    // even if it means exceeding the target context size. Evidence completeness is critical
    // for valid test cases. The context size is a target for dilution, not a hard limit.
    val irrelevantCount = contextSize - evidenceCount
    
    // Get irrelevant conversations for the same person
    val personId = evidenceItem.personId
    val availableIrrelevantConversations = personId match {
      case Some(pid) => irrelevantConversationsByPerson.getOrElse(pid, List.empty)
      case None => 
        // For legacy evidence without personId, use a random person's conversations
        if (irrelevantConversationsByPerson.nonEmpty) {
          val randomPerson = Random.shuffle(irrelevantConversationsByPerson.keys.toList).head
          irrelevantConversationsByPerson(randomPerson)
        } else {
          List.empty
        }
    }
    
    // Select random irrelevant conversations from the same person
    // Note: When irrelevantCount <= 0 (context size smaller than evidence count),
    // we correctly select no irrelevant conversations, allowing all evidence to be included
    val selectedIrrelevantConversations = if (irrelevantCount > 0 && availableIrrelevantConversations.nonEmpty) {
      Random.shuffle(availableIrrelevantConversations).take(irrelevantCount)
    } else {
      List.empty
    }
    
    // Debug logging for insufficient conversations
    if (selectedIrrelevantConversations.length < irrelevantCount && contextSize > evidenceCount) {
      if (personId.isDefined && availableIrrelevantConversations.isEmpty) {
        println(s"WARNING: No irrelevant conversations found for person ${personId.get}")
      } else if (selectedIrrelevantConversations.length < irrelevantCount) {
        println(s"WARNING: Insufficient irrelevant conversations for context size $contextSize. " +
                s"Needed: $irrelevantCount, Available: ${availableIrrelevantConversations.length}, Selected: ${selectedIrrelevantConversations.length}")
      }
    }
    
    // Mix evidence conversations with irrelevant ones
    val allConversations = distributeEvidenceConversations(
      selectedIrrelevantConversations,
      evidenceConversations
    )
    
    TestCase(
      evidenceItems = List(evidenceItem),
      conversations = allConversations,
      contextSize = Some(contextSize)
    )
  }
  
  /**
   * Distribute evidence conversations randomly among irrelevant conversations while preserving evidence order.
   * 
   * @param irrelevantConversations List of irrelevant conversations
   * @param evidenceConversations List of evidence conversations (order preserved)
   * @return Mixed list of conversations
   */
  def distributeEvidenceConversations(
    irrelevantConversations: List[Conversation],
    evidenceConversations: List[Conversation]
  ): List[Conversation] = {
    val totalConversations = irrelevantConversations.length + evidenceConversations.length
    
    if (evidenceConversations.isEmpty) {
      return irrelevantConversations
    }
    
    if (irrelevantConversations.isEmpty) {
      return evidenceConversations
    }
    
    // Choose random positions for evidence conversations (sorted to maintain evidence order)
    val evidencePositions = Random.shuffle((0 until totalConversations).toList)
      .take(evidenceConversations.length)
      .sorted
    
    // Create a map of position -> evidence conversation
    val evidenceMap = evidencePositions.zip(evidenceConversations).toMap
    
    // Build the final conversation order
    val finalConversations = scala.collection.mutable.ListBuffer[Conversation]()
    var irrelevantIndex = 0
    
    for (position <- 0 until totalConversations) {
      evidenceMap.get(position) match {
        case Some(evidenceConv) =>
          finalConversations += evidenceConv
        case None =>
          if (irrelevantIndex < irrelevantConversations.length) {
            finalConversations += irrelevantConversations(irrelevantIndex)
            irrelevantIndex += 1
          }
      }
    }
    
    finalConversations.toList
  }
}