package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.questions.evidence.{Conversation, GeneratedEvidenceCore, Message}

/**
 * Categories of validation failures
 */
object ValidationFailureCategory {
  val INVALID_SPEAKERS = "invalid_speakers"
  val CONVERSATION_COUNT_MISMATCH = "conversation_count_mismatch"
  val EVIDENCE_NOT_FOUND = "evidence_not_found"
  val EVIDENCE_IN_MULTIPLE_CONVERSATIONS = "evidence_in_multiple_conversations"
  val EVIDENCE_IN_WRONG_CONVERSATION = "evidence_in_wrong_conversation"
}

/**
 * Result of conversation validation
 */
case class ValidationResult(
  isValid: Boolean,
  errors: List[String],
  failureCategories: Set[String] = Set.empty
)

/**
 * Validates that generated conversations contain all evidence messages exactly once
 * and in the correct order.
 */
object ConversationValidator {
  
  /**
   * Validates that all evidence messages from the core are present in the conversations
   * exactly once and in the same order.
   * 
   * @param evidenceCore The evidence core containing the messages to find
   * @param conversations The generated conversations to validate
   * @return ValidationResult with isValid flag and any error messages
   */
  def validateConversations(
    evidenceCore: GeneratedEvidenceCore, 
    conversations: List[Conversation]
  ): ValidationResult = {
    val evidenceMessages = evidenceCore.message_evidences
    val errors = scala.collection.mutable.ListBuffer[String]()
    val failureCategories = scala.collection.mutable.Set[String]()
    
    // Check 1: Validate speakers in all conversations
    val validSpeakers = Set("User", "Assistant")
    for ((conversation, convIdx) <- conversations.zipWithIndex) {
      val invalidSpeakers = conversation.messages.filterNot(msg => validSpeakers.contains(msg.speaker))
      if (invalidSpeakers.nonEmpty) {
        val uniqueInvalidSpeakers = invalidSpeakers.map(_.speaker).distinct
        errors += s"Invalid speakers found in conversation $convIdx: ${uniqueInvalidSpeakers.mkString(", ")}. Only 'User' and 'Assistant' are valid speakers."
        failureCategories += ValidationFailureCategory.INVALID_SPEAKERS
      }
    }
    
    // Check 2: Number of conversations must match number of evidence messages
    if (conversations.length != evidenceMessages.length) {
      errors += s"Number of conversations (${conversations.length}) doesn't match number of evidence messages (${evidenceMessages.length})"
      failureCategories += ValidationFailureCategory.CONVERSATION_COUNT_MISMATCH
      return ValidationResult(false, errors.toList, failureCategories.toSet)
    }
    
    // Handle empty case
    if (evidenceMessages.isEmpty && conversations.isEmpty) {
      return ValidationResult(true, List.empty, Set.empty)
    }
    
    // Track which conversations contain each evidence message
    val evidenceToConversations = scala.collection.mutable.Map[Int, List[Int]]()
    
    // Check each evidence message against ALL conversations
    for ((evidenceMsg, evidenceIdx) <- evidenceMessages.zipWithIndex) {
      val foundInConversations = scala.collection.mutable.ListBuffer[Int]()
      
      // Check all conversations for this evidence message
      for ((conversation, convIdx) <- conversations.zipWithIndex) {
        // First try exact substring match
        val foundExact = conversation.messages.exists { msg =>
          msg.speaker == evidenceMsg.speaker && msg.text.contains(evidenceMsg.text)
        }
        
        if (foundExact) {
          foundInConversations += convIdx
        } else {
          // Try reverse partial match - evidence might be part of a longer message
          val foundPartial = conversation.messages.exists { msg =>
            msg.speaker == evidenceMsg.speaker && 
            evidenceMsg.text.contains(msg.text) && 
            msg.text.length >= evidenceMsg.text.length * 0.8  // Message should be at least 80% of evidence length
          }
          
          if (foundPartial) {
            foundInConversations += convIdx
          } else {
            // Finally try fuzzy matching with dynamic threshold
            val foundFuzzy = conversation.messages.exists { msg =>
              if (msg.speaker == evidenceMsg.speaker) {
                val threshold = math.max(10, (evidenceMsg.text.length * 0.15).toInt)  // 15% of message length, minimum 10
                val distance = levenshteinDistance(msg.text, evidenceMsg.text)
                
                // Debug logging for near misses
                if (distance > threshold && distance <= threshold * 1.5 && Config.DEBUG) {
                  println(s"Near miss in conversation $convIdx - distance $distance (threshold $threshold):")
                  println(s"  Evidence: ${evidenceMsg.text}")
                  println(s"  Found:    ${msg.text}")
                }
                
                distance <= threshold
              } else {
                false
              }
            }
            
            if (foundFuzzy) {
              foundInConversations += convIdx
            }
          }
        }
      }
      
      evidenceToConversations(evidenceIdx) = foundInConversations.toList
    }
    
    // Check that each evidence message appears in exactly one conversation
    // and that it appears in the correct conversation (same index)
    for ((evidenceIdx, conversationIndices) <- evidenceToConversations) {
      if (conversationIndices.isEmpty) {
        errors += s"Evidence message $evidenceIdx not found in any conversation: ${evidenceMessages(evidenceIdx).text}"
        failureCategories += ValidationFailureCategory.EVIDENCE_NOT_FOUND
      } else if (conversationIndices.length > 1) {
        errors += s"Evidence message $evidenceIdx found in multiple conversations: [${conversationIndices.mkString(", ")}]"
        failureCategories += ValidationFailureCategory.EVIDENCE_IN_MULTIPLE_CONVERSATIONS
      } else if (conversationIndices.head != evidenceIdx) {
        errors += s"Evidence message $evidenceIdx found in wrong conversation: expected conversation $evidenceIdx but found in conversation ${conversationIndices.head}"
        failureCategories += ValidationFailureCategory.EVIDENCE_IN_WRONG_CONVERSATION
      }
    }
    
    ValidationResult(errors.isEmpty, errors.toList, failureCategories.toSet)
  }
  
  /**
   * Calculate the Levenshtein distance between two strings.
   * This is the minimum number of single-character edits (insertions, deletions, or substitutions)
   * required to change one string into the other.
   * 
   * @param s1 First string
   * @param s2 Second string
   * @return The Levenshtein distance
   */
  def levenshteinDistance(s1: String, s2: String): Int = {
    val len1 = s1.length
    val len2 = s2.length
    
    // Create a 2D array to store distances
    val dist = Array.ofDim[Int](len1 + 1, len2 + 1)
    
    // Initialize first column and row
    for (i <- 0 to len1) dist(i)(0) = i
    for (j <- 0 to len2) dist(0)(j) = j
    
    // Fill in the rest of the matrix
    for (i <- 1 to len1) {
      for (j <- 1 to len2) {
        val cost = if (s1(i - 1) == s2(j - 1)) 0 else 1
        
        dist(i)(j) = math.min(
          math.min(
            dist(i - 1)(j) + 1,      // deletion
            dist(i)(j - 1) + 1       // insertion
          ),
          dist(i - 1)(j - 1) + cost  // substitution
        )
      }
    }
    
    dist(len1)(len2)
  }
}