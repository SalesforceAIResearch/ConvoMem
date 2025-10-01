package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem, Message}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class VerifyIntermediateEvidenceAddressesFixedTest extends AnyFunSpec with Matchers {
  
  describe("VerifyIntermediateEvidenceAddresses - Fixed Implementation") {
    
    val verifier = new VerifyIntermediateEvidenceAddresses()
    val stats = Some(TrieMap[String, (AtomicInteger, AtomicInteger)]())
    val answeringEvaluation = DefaultAnsweringEvaluation
    
    it("should pass when all intermediate evidence messages are about the same topic as the question") {
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
              Message("User", "The weather is nice today"), // This should not be checked
              Message("User", "Let's schedule our meeting for 2 PM on Wednesday"),
              Message("Assistant", "Meeting scheduled for 2 PM Wednesday")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Actually, can we move the meeting to 3 PM on Thursday?"),
              Message("Assistant", "Updated to 3 PM Thursday"),
              Message("User", "I like pizza") // This should not be checked
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "One more change - let's make it 4 PM on Friday instead"),
              Message("Assistant", "Final update: 4 PM Friday")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Meeting scheduling")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 1 - All relevant intermediate messages")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      result.passed should be(true)
      result.details should include("2") // Should check 2 intermediate messages
    }
    
    it("should fail when intermediate evidence messages are not about the question topic") {
      val evidenceItem = EvidenceItem(
        question = "What's my favorite color?",
        answer = "Your favorite color is blue",
        message_evidences = List(
          Message("User", "I had pizza for lunch"), // Not about color
          Message("User", "The weather is great today"), // Not about color
          Message("User", "My favorite color is blue") // Only this is relevant
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "I had pizza for lunch"),
              Message("Assistant", "That sounds delicious!")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "The weather is great today"),
              Message("Assistant", "Perfect day to be outside!")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "My favorite color is blue"),
              Message("Assistant", "I'll remember that your favorite color is blue")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Color preference")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 2 - Irrelevant intermediate messages")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      result.passed should be(false)
      result.details should include("doesn't address the question")
    }
    
    it("should handle changing evidence scenarios correctly") {
      val evidenceItem = EvidenceItem(
        question = "Where am I traveling?",
        answer = "You're not traveling anywhere (trip was cancelled)",
        message_evidences = List(
          Message("User", "I'm planning a trip to Paris next month"),
          Message("User", "Change of plans - going to London instead"),
          Message("User", "Actually, I had to cancel my trip entirely")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "I'm planning a trip to Paris next month"),
              Message("Assistant", "Paris sounds wonderful!")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Change of plans - going to London instead"),
              Message("Assistant", "London is great too!")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Actually, I had to cancel my trip entirely"),
              Message("Assistant", "Sorry to hear about the cancellation")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Travel plans changing")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 3 - Changing travel plans")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      result.passed should be(true)
      // All intermediate messages are about travel, even though plans change
    }
    
    it("should skip check when there's only one evidence message") {
      val evidenceItem = EvidenceItem(
        question = "What's the project status?",
        answer = "The project is complete",
        message_evidences = List(
          Message("User", "The project is complete")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "Just finished everything"),
              Message("User", "The project is complete"),
              Message("Assistant", "Congratulations on completing the project!")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Project status")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 4 - Single evidence message")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      result.passed should be(true)
      result.details should include("Not enough evidence messages")
    }
  }
}