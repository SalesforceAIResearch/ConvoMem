package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem, Message}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class VerifyIntermediateEvidenceAddressesTest extends AnyFunSpec with Matchers {
  
  describe("VerifyIntermediateEvidenceAddresses") {
    
    val verifier = new VerifyIntermediateEvidenceAddresses()
    val stats = Some(TrieMap[String, (AtomicInteger, AtomicInteger)]())
    val answeringEvaluation = DefaultAnsweringEvaluation
    
    it("should pass when all intermediate evidence messages address the question") {
      val evidenceItem = EvidenceItem(
        question = "What is my favorite color?",
        answer = "Your favorite color is now blue",
        message_evidences = List(
          Message("John", "My favorite color is red"),
          Message("John", "Actually, I've changed my mind. My favorite color is now blue")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("John", "My favorite color is red"),
              Message("Assistant", "I'll remember that your favorite color is red")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("John", "Actually, I've changed my mind. My favorite color is now blue"),
              Message("Assistant", "Got it, I've updated that your favorite color is blue")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Test scenario")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      result.passed should be(true)
      result.details should include("intermediate evidence messages address the question")
    }
    
    it("should fail when intermediate evidence messages don't address the question") {
      val evidenceItem = EvidenceItem(
        question = "What is my meeting time?",
        answer = "Your meeting is at 3 PM",
        message_evidences = List(
          Message("John", "The weather is nice today"),
          Message("John", "My meeting is at 3 PM")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("John", "The weather is nice today"),
              Message("Assistant", "Yes, it's a beautiful day!")
            ),
            containsEvidence = Some(true)
          ),
          Conversation(
            messages = List(
              Message("John", "My meeting is at 3 PM"),
              Message("Assistant", "I'll note that your meeting is at 3 PM")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Test scenario")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      result.passed should be(false)
      result.details should include("doesn't address the question")
    }
    
    it("should pass when there's only one conversation") {
      val evidenceItem = EvidenceItem(
        question = "What is my favorite food?",
        answer = "Your favorite food is pizza",
        message_evidences = List(
          Message("John", "My favorite food is pizza")
        ),
        conversations = List(
          Conversation(
            messages = List(
              Message("John", "My favorite food is pizza"),
              Message("Assistant", "I'll remember that your favorite food is pizza")
            ),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some("Test scenario")
      )
      
      val result = verifier.verify(evidenceItem, stats, answeringEvaluation)
      result.passed should be(true)
      result.details should include("Not enough conversations")
    }
  }
}