package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem, Message}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class VerifyIntermediateEvidenceStrictTest extends AnyFunSpec with Matchers {
  
  describe("VerifyIntermediateEvidenceAddresses - Strict Prompt Test") {
    
    val verifier = new VerifyIntermediateEvidenceAddresses()
    val stats = Some(TrieMap[String, (AtomicInteger, AtomicInteger)]())
    val answeringEvaluation = DefaultAnsweringEvaluation
    
    it("should fail when intermediate messages are too vague or general") {
      val evidenceItem = EvidenceItem(
        question = "What time is our meeting scheduled?",
        answer = "The meeting is scheduled for 4 PM on Friday",
        message_evidences = List(
          Message("User", "We should have a meeting about the project"), // Too vague - no time
          Message("User", "I'm thinking sometime this week would work"), // Too vague - no specific time
          Message("User", "Let's make it 4 PM on Friday") // This is specific and should pass
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "We should have a meeting about the project"),
              Message("Assistant", "Sure, when would you like to meet?")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "I'm thinking sometime this week would work"),
              Message("Assistant", "Please let me know a specific time")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Let's make it 4 PM on Friday"),
              Message("Assistant", "Perfect, I've scheduled it for 4 PM Friday")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Meeting scheduling with vague intermediate messages")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 1 - Vague intermediate messages")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      // With stricter criteria, this should fail because first two messages are too vague
      result.passed should be(false)
    }
    
    it("should fail when messages are tangentially related but don't directly address the question") {
      val evidenceItem = EvidenceItem(
        question = "Where is my passport?",
        answer = "Your passport is in the desk drawer",
        message_evidences = List(
          Message("User", "I need to prepare for my trip"), // Related to travel but not passport location
          Message("User", "I should check all my documents"), // About documents but not passport location
          Message("User", "Found it! My passport is in the desk drawer") // Direct answer
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "I need to prepare for my trip"),
              Message("Assistant", "What do you need to prepare?")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "I should check all my documents"),
              Message("Assistant", "Good idea to organize your documents")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Found it! My passport is in the desk drawer"),
              Message("Assistant", "Great! I'll note that your passport is in the desk drawer")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Finding passport with tangentially related messages")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 2 - Tangentially related messages")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      // Should fail because first two messages don't directly address passport location
      result.passed should be(false)
    }
    
    it("should pass only when all intermediate messages contain specific relevant information") {
      val evidenceItem = EvidenceItem(
        question = "What's my flight number?",
        answer = "Your flight number is AA789",
        message_evidences = List(
          Message("User", "I booked flight DL123 to New York"), // Specific but wrong flight
          Message("User", "Actually, I had to change it to flight AA789") // Specific update
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "I booked flight DL123 to New York"),
              Message("Assistant", "I've noted your flight DL123")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Actually, I had to change it to flight AA789"),
              Message("Assistant", "Updated - your flight is now AA789")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Flight booking with specific information")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 3 - All messages with specific information")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      // Should pass because both messages contain specific flight numbers
      result.passed should be(true)
    }
    
    it("should be strict about matching the exact question topic") {
      val evidenceItem = EvidenceItem(
        question = "What's the meeting agenda?",
        answer = "The agenda is: 1) Budget review 2) Q4 planning",
        message_evidences = List(
          Message("User", "The meeting is at 3 PM"), // About meeting but not agenda
          Message("User", "Everyone from sales will attend"), // About meeting but not agenda
          Message("User", "The agenda is: 1) Budget review 2) Q4 planning") // Direct answer
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "The meeting is at 3 PM"),
              Message("Assistant", "Meeting time noted")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "Everyone from sales will attend"),
              Message("Assistant", "I'll note the attendees")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("User", "The agenda is: 1) Budget review 2) Q4 planning"),
              Message("Assistant", "I've recorded the agenda items")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Meeting details with only final message about agenda")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      println(s"\nTest 4 - Strict topic matching")
      println(s"Result: ${result.passed}")
      println(s"Details: ${result.details}")
      
      // Should fail because first two messages are about the meeting but not the agenda
      result.passed should be(false)
    }
  }
}