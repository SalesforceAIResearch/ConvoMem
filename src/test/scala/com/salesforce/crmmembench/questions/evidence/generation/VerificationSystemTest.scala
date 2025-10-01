package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.{Message, Conversation, EvidenceItem}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class VerificationSystemTest extends AnyFlatSpec with Matchers {
  
  "VerificationCheck" should "track stats correctly" in {
    // Create a simple test check
    class TestCheck extends VerificationCheck {
      override def name: String = "test_check"
      
      override def verify(
        evidenceItem: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        trackCheck(true, stats)
        VerificationCheckResult(
          checkName = name,
          passed = true,
          details = "Test passed",
          modelAnswer = Some("test answer")
        )
      }
    }
    
    val stats = TrieMap[String, (AtomicInteger, AtomicInteger)]()
    val check = new TestCheck()
    val evidenceItem = createTestEvidenceItem()
    
    // Run check multiple times
    check.verify(evidenceItem, Some(stats), DefaultAnsweringEvaluation)
    check.verify(evidenceItem, Some(stats), DefaultAnsweringEvaluation)
    
    // Verify stats were tracked
    stats.get("test_check") shouldBe defined
    val (attempts, passes) = stats("test_check")
    attempts.get() shouldBe 2
    passes.get() shouldBe 2
  }
  
  "VerificationExecutor" should "combine multiple check results" in {
    val evidenceItem = createTestEvidenceItem()
    
    // Create mock checks
    val check1 = new VerificationCheck {
      override def name: String = "check1"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        VerificationCheckResult("check1", true, "Passed", Some("answer1"))
      }
    }
    
    val check2 = new VerificationCheck {
      override def name: String = "check2"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        VerificationCheckResult("check2", true, "Passed", Some("answer2"))
      }
    }
    
    val result = VerificationExecutor.execute(evidenceItem, List(check1, check2), None, DefaultAnsweringEvaluation)
    
    result.passed shouldBe true
    result.checks.length shouldBe 2
    result.lastModelAnswer shouldBe "answer2"
    result.failureReason shouldBe None
  }
  
  "VerificationExecutor" should "fail if any check fails" in {
    val evidenceItem = createTestEvidenceItem()
    
    val passingCheck = new VerificationCheck {
      override def name: String = "passing"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        VerificationCheckResult("passing", true, "Passed", Some("good"))
      }
    }
    
    val failingCheck = new VerificationCheck {
      override def name: String = "failing"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        VerificationCheckResult("failing", false, "Failed on purpose", Some("bad"))
      }
    }
    
    val result = VerificationExecutor.execute(evidenceItem, List(passingCheck, failingCheck), None, DefaultAnsweringEvaluation)
    
    result.passed shouldBe false
    result.failureReason shouldBe defined
    result.failureReason.get should include("failing: Failed on purpose")
  }
  
  "VerificationExecutor" should "pass with empty check list" in {
    val evidenceItem = createTestEvidenceItem()
    val result = VerificationExecutor.execute(evidenceItem, List.empty, None, DefaultAnsweringEvaluation)
    
    result.passed shouldBe true
    result.checks shouldBe empty
    result.lastModelAnswer shouldBe "No verification required"
  }
  
  "VerificationExecutor" should "stop at first failure without executing subsequent checks" in {
    val evidenceItem = createTestEvidenceItem()
    
    var check3Executed = false
    
    val check1 = new VerificationCheck {
      override def name: String = "check1"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        VerificationCheckResult("check1", true, "Passed", Some("answer1"))
      }
    }
    
    val check2 = new VerificationCheck {
      override def name: String = "check2"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        VerificationCheckResult("check2", false, "Failed on purpose", Some("answer2"))
      }
    }
    
    val check3 = new VerificationCheck {
      override def name: String = "check3"
      override def verify(
        item: EvidenceItem,
        stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
        answeringEvaluation: AnsweringEvaluation
      ): VerificationCheckResult = {
        check3Executed = true
        VerificationCheckResult("check3", true, "Should not execute", Some("answer3"))
      }
    }
    
    val result = VerificationExecutor.execute(evidenceItem, List(check1, check2, check3), None, DefaultAnsweringEvaluation)
    
    result.passed shouldBe false
    result.checks.length shouldBe 2 // Only check1 and check2 should have been executed
    result.checks.map(_.checkName) shouldBe List("check1", "check2")
    result.lastModelAnswer shouldBe "answer2"
    result.failureReason shouldBe Some("check2: Failed on purpose")
    check3Executed shouldBe false // Verify check3 was never executed
  }
  
  def createTestEvidenceItem(): EvidenceItem = {
    EvidenceItem(
      question = "Test question?",
      answer = "Test answer",
      message_evidences = List(Message("user", "Test evidence")),
      conversations = List(
        Conversation(
          messages = List(
            Message("user", "Test message"),
            Message("assistant", "Test response")
          ),
          id = Some("test-1"),
          containsEvidence = Some(true)
        )
      ),
      category = "test",
      scenario_description = Some("Test scenario")
    )
  }
}