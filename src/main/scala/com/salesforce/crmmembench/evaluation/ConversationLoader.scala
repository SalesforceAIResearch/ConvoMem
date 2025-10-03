package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.conversations.ConversationEvidenceGenerator
import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem}
import java.util.UUID

/**
 * Loads irrelevant conversations using the ConversationEvidenceGenerator.
 * This replaces the old FlexibleContextDataLoader.loadAllConversations() method.
 */
object ConversationLoader {
  
  /**
   * Load all irrelevant conversations from the ConversationEvidenceGenerator.
   * These conversations are used as background/irrelevant context in test cases.
   * 
   * @return Map of person ID to list of conversations for that person
   */
  def loadIrrelevantConversations(): Map[String, List[Conversation]] = {
    val generator = new ConversationEvidenceGenerator()
    
    println("Loading irrelevant conversations using ConversationEvidenceGenerator...")
    
    // Load all evidence items from the conversation generator
    val evidenceItems = generator.loadEvidenceItems()
    
    println(s"Loaded ${evidenceItems.size} conversation evidence items")
    
    // Group conversations by person ID
    val conversationsByPerson = evidenceItems
      .filter(_.personId.isDefined) // Only process items with person IDs
      .groupBy(_.personId.get)
      .map { case (personId, items) =>
        // Extract all conversations for this person
        val personConversations = items.flatMap { evidenceItem =>
          // Each evidence item contains multiple conversations
          evidenceItem.conversations.zipWithIndex.map { case (conversation, idx) =>
            // Ensure each conversation has proper ID and containsEvidence flag
            conversation.copy(
              id = conversation.id.orElse(Some(s"irrelevant-${UUID.randomUUID()}")),
              containsEvidence = Some(false)
            )
          }
        }
        personId -> personConversations
      }
    
    val totalConversations = conversationsByPerson.values.map(_.length).sum
    println(s"Total irrelevant conversations loaded: $totalConversations across ${conversationsByPerson.size} persons")
    
    if (evidenceItems.isEmpty) {
      throw new RuntimeException("No irrelevant conversations found. Please run GenerateConversationEvidence first.")
    }
    
    if (conversationsByPerson.isEmpty && evidenceItems.nonEmpty) {
      println("WARNING: All evidence items lack personId field. Returning empty map.")
      println("To use person-based conversation loading, regenerate conversation evidence with the updated code.")
    }
    
    conversationsByPerson
  }
  
  /**
   * Load a specific number of irrelevant conversations.
   * Useful for testing or when you need a limited set.
   * 
   * @param count Maximum number of conversations to load (distributed across persons)
   * @return Map of person ID to list of conversations, limited to approximately the specified count
   */
  def loadIrrelevantConversations(count: Int): Map[String, List[Conversation]] = {
    val allConversationsByPerson = loadIrrelevantConversations()
    val totalConversations = allConversationsByPerson.values.map(_.length).sum
    
    if (totalConversations <= count) {
      // If we have fewer than requested, return all
      allConversationsByPerson
    } else {
      // Distribute the count proportionally across persons
      val scaleFactor = count.toDouble / totalConversations
      allConversationsByPerson.map { case (personId, conversations) =>
        val targetCount = Math.max(1, (conversations.length * scaleFactor).toInt)
        personId -> conversations.take(targetCount)
      }
    }
  }
}