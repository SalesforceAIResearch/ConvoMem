package com.salesforce.crmmembench

import com.salesforce.crmmembench.questions.evidence.Message
import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem}

/**
 * Centralized message normalization utility.
 * Ensures consistent speaker role normalization across the codebase.
 */
object MessageNormalizer {
  
  /**
   * Normalize a single message by converting speaker to lowercase.
   */
  def normalizeMessage(message: Message): Message = {
    message.copy(speaker = message.speaker.toLowerCase)
  }
  
  /**
   * Normalize all messages in a conversation.
   */
  def normalizeConversation(conversation: Conversation): Conversation = {
    conversation.copy(
      messages = conversation.messages.map(normalizeMessage)
    )
  }
  
  /**
   * Normalize all messages in an evidence item.
   */
  def normalizeEvidenceItem(item: EvidenceItem): EvidenceItem = {
    item.copy(
      message_evidences = item.message_evidences.map(normalizeMessage),
      conversations = item.conversations.map(normalizeConversation)
    )
  }
  
  /**
   * Normalize a list of messages.
   */
  def normalizeMessages(messages: List[Message]): List[Message] = {
    messages.map(normalizeMessage)
  }
}