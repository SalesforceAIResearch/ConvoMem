package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class ExtensiveFilteringStatsTest extends AnyFunSuite with Matchers {
  
  test("verifyWithEvidence should track stats when provided") {
    // Create a simple evidence item for testing
    val evidenceItem = EvidenceItem(
      question = "What is the capital of France?",
      answer = "Paris",
      message_evidences = List(Message("user", "The capital of France is Paris.")),
      conversations = List(
        Conversation(
          messages = List(
            Message("user", "I'm studying European capitals."),
            Message("assistant", "That's great! What would you like to know?"),
            Message("user", "The capital of France is Paris."),
            Message("assistant", "Yes, that's correct! Paris is the capital of France.")
          ),
          id = Some("test-conv-1"),
          containsEvidence = Some(true)
        )
      ),
      category = "Geography",
      scenario_description = Some("Geography question about capitals"),
      personId = Some("test-person")
    )
    
    // Create stats map
    val stats = TrieMap[String, (AtomicInteger, AtomicInteger)]()
    
    // Run verification with stats tracking
    println("\nRunning extensive verification with stats tracking...")
    val result = FilteringVerification.verifyWithEvidence(
      evidenceItem,
      requiredPasses = 2,
      extensive = true,
      stats = Some(stats),
      answeringEvaluation = DefaultAnsweringEvaluation
    )
    
    // Check that we got a result
    result should not be None
    
    // Check that stats were collected
    stats should not be empty
    
    // Print collected stats
    println("\nCollected stats:")
    stats.foreach { case (modelName, (attempts, passes)) =>
      val attemptCount = attempts.get()
      val passCount = passes.get()
      val successRate = if (attemptCount > 0) (passCount.toDouble / attemptCount * 100) else 0.0
      println(f"  $modelName%-30s: $passCount/$attemptCount (${successRate}%.1f%%)")
      
      // Verify success rate is between 0 and 100
      successRate should be >= 0.0
      successRate should be <= 100.0
    }
    
    // Verify stats structure
    val cheapModels = FilteringVerification.modelsForExtensiveVerification
    println(s"\nExpected models: ${cheapModels.map(m => s"${m.getProvider}-${m.getModelName}").mkString(", ")}")
    
    // Each model that was tested should have at least 1 attempt
    stats.values.foreach { case (attempts, passes) =>
      attempts.get() should be >= 1
    }
  }
  
  test("verifyWithEvidence should not track stats when not provided") {
    val evidenceItem = EvidenceItem(
      question = "Test question",
      answer = "Test answer",
      message_evidences = List(Message("user", "Test")),
      conversations = List(
        Conversation(
          messages = List(Message("user", "Test")),
          id = Some("test"),
          containsEvidence = Some(true)
        )
      ),
      category = "Test",
      scenario_description = Some("Test scenario"),
      personId = Some("test")
    )
    
    // Run without stats - should work normally
    noException should be thrownBy {
      FilteringVerification.verifyWithEvidence(
        evidenceItem,
        requiredPasses = 1,
        extensive = false,
        answeringEvaluation = DefaultAnsweringEvaluation
      )
    }
  }
  
  test("Stats should correctly track failures") {
    // Create an evidence item that will likely fail
    val evidenceItem = EvidenceItem(
      question = "What is my secret code?",
      answer = "XYZ123ABC789",  // Very specific answer unlikely to be guessed
      message_evidences = List(Message("user", "My secret code is XYZ123ABC789")),
      conversations = List(
        Conversation(
          messages = List(
            Message("user", "I need to remember something."),
            Message("assistant", "What do you need to remember?"),
            Message("user", "My secret code is XYZ123ABC789"),
            Message("assistant", "I'll help you remember that.")
          ),
          id = Some("test-conv-fail"),
          containsEvidence = Some(true)
        )
      ),
      category = "Memory",
      scenario_description = Some("Secret code memory test"),
      personId = Some("test-person")
    )
    
    val stats = TrieMap[String, (AtomicInteger, AtomicInteger)]()
    
    // This might fail due to the very specific answer
    val result = FilteringVerification.verifyWithEvidence(
      evidenceItem,
      requiredPasses = 2,
      extensive = true,
      stats = Some(stats),
      answeringEvaluation = DefaultAnsweringEvaluation
    )
    
    // Print results
    println("\nStats after verification (may include failures):")
    stats.foreach { case (modelName, (attempts, passes)) =>
      println(f"  $modelName%-30s: ${passes.get()} passes, ${attempts.get()} attempts")
    }
    
    // Whether it passed or failed, stats should be recorded
    stats should not be empty
  }
}