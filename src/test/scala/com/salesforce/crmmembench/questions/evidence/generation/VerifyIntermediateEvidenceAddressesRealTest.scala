package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem, Message}
import com.salesforce.crmmembench.LLM_endpoints.Gemini
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class VerifyIntermediateEvidenceAddressesRealTest extends AnyFunSpec with Matchers {
  
  describe("VerifyIntermediateEvidenceAddresses with real LLM") {
    
    val verifier = new VerifyIntermediateEvidenceAddresses()
    val stats = Some(TrieMap[String, (AtomicInteger, AtomicInteger)]())
    val answeringEvaluation = DefaultAnsweringEvaluation
    
    it("should understand the structure of changing evidence") {
      // Create a realistic changing evidence item
      val evidenceItem = EvidenceItem(
        question = "What time is our meeting scheduled?",
        answer = "The meeting is scheduled for 4 PM on Friday",
        message_evidences = List(
          Message("User", "Let's schedule our meeting for 2 PM on Wednesday"),
          Message("User", "Actually, can we move the meeting to 3 PM on Thursday?"),
          Message("User", "One more change - let's make it 4 PM on Friday instead")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "Hey, I wanted to discuss our upcoming project"),
              Message("Assistant", "Sure, I'd be happy to discuss the project with you"),
              Message("User", "Let's schedule our meeting for 2 PM on Wednesday"),
              Message("Assistant", "I've noted that - meeting scheduled for 2 PM on Wednesday"),
              Message("User", "What's the weather like today?"),
              Message("Assistant", "I don't have access to current weather information")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "I've been thinking about our timeline"),
              Message("Assistant", "What aspects of the timeline would you like to discuss?"),
              Message("User", "Actually, can we move the meeting to 3 PM on Thursday?"),
              Message("Assistant", "No problem, I've updated it to 3 PM on Thursday"),
              Message("User", "Also, should we invite Sarah?"),
              Message("Assistant", "That's a good idea if she's involved in the project")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "I just checked my calendar"),
              Message("Assistant", "Did you find any conflicts?"),
              Message("User", "One more change - let's make it 4 PM on Friday instead"),
              Message("Assistant", "Got it, the meeting is now scheduled for 4 PM on Friday"),
              Message("User", "Perfect, thanks"),
              Message("Assistant", "You're welcome! I'll send out the updated invite")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Professional",
        scenario_description = Some("Scheduling and rescheduling a meeting")
      )
      
      println("\nEvidence messages to check:")
      evidenceItem.message_evidences.foreach(msg => println(s"  ${msg.speaker}: ${msg.text}"))
      
      println("\nConversations:")
      evidenceItem.conversations.zipWithIndex.foreach { case (conv, idx) =>
        println(s"\nConversation ${idx + 1}:")
        conv.messages.foreach(msg => println(s"  ${msg.speaker}: ${msg.text}"))
      }
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nVerification result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      // This should fail because we're checking ALL messages, not just evidence messages
      result.passed should be(false)
    }
    
    it("should check only evidence messages, not all conversation messages") {
      // Create an evidence item where conversations have irrelevant messages
      // but evidence messages are all relevant
      val evidenceItem = EvidenceItem(
        question = "What's my favorite color?",
        answer = "Your favorite color is blue",
        message_evidences = List(
          Message("User", "My favorite color is red"),
          Message("User", "Actually, I changed my mind - my favorite color is blue")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "The weather is nice today"), // Irrelevant
              Message("Assistant", "Yes, it's a beautiful day!"), // Irrelevant
              Message("User", "My favorite color is red"), // This is evidence
              Message("Assistant", "I'll remember that"),
              Message("User", "What's for lunch?"), // Irrelevant
              Message("Assistant", "I can't help with meal planning") // Irrelevant
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "I'm thinking about redecorating"), // Somewhat related but not evidence
              Message("Assistant", "That sounds exciting!"),
              Message("User", "Actually, I changed my mind - my favorite color is blue"), // This is evidence
              Message("Assistant", "Noted, your favorite color is now blue")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Personal",
        scenario_description = Some("Changing color preference")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 2 - Verification result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      // This test reveals the bug - we should only check evidence messages
      // not all messages in conversations
    }
  }
}