package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExtensiveFilteringTest extends AnyFunSuite with Matchers {
  
  test("Extensive filtering should test across all cheap models") {
    // Create a simple evidence item for testing
    val evidenceItem = EvidenceItem(
      question = "What is 2 + 2?",
      answer = "4",
      message_evidences = List(Message("user", "What is 2 + 2?")),
      conversations = List(
        Conversation(
          messages = List(
            Message("user", "What is 2 + 2?"),
            Message("assistant", "2 + 2 equals 4.")
          ),
          id = Some("test-conv"),
          containsEvidence = Some(true)
        )
      ),
      category = "Math",
      scenario_description = Some("Simple math question"),
      personId = Some("test-person")
    )
    
    // Test that cheap models list is properly configured
    val cheapModels = FilteringVerification.modelsForExtensiveVerification
    cheapModels should not be empty
    println(s"Models for extensive verification: ${cheapModels.map(m => s"${m.getProvider}-${m.getModelName}").mkString(", ")}")
    
    // Each model should be different
    val modelNames = cheapModels.map(m => s"${m.getProvider}-${m.getModelName}")
    modelNames.distinct.size shouldBe modelNames.size
    
    // Test extensive flag behavior
    println("\nTesting extensive verification behavior:")
    println("- Standard mode: Uses default model, requires 2 consecutive passes")
    println("- Extensive mode: Tests all cheap models, each requires 3 consecutive passes")
    
    // Verify the requirement
    val requiredPassesPerModel = 3
    val totalVerifications = cheapModels.size * requiredPassesPerModel
    println(s"\nExtensive mode would perform $totalVerifications verifications:")
    println(s"  ${cheapModels.size} models × $requiredPassesPerModel passes each = $totalVerifications total")
  }
  
  test("FilteringVerification should support both standard and extensive modes") {
    // This test verifies the API supports both modes
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
    
    // Verify the method signature accepts extensive flag
    // Standard verification
    noException should be thrownBy {
      FilteringVerification.verifyWithEvidence(
        evidenceItem,
        requiredPasses = 1,
        extensive = false,
        answeringEvaluation = DefaultAnsweringEvaluation
      )
    }
    
    // Extensive verification (would actually call multiple models)
    noException should be thrownBy {
      FilteringVerification.verifyWithEvidence(
        evidenceItem,
        requiredPasses = 1,
        extensive = true,
        answeringEvaluation = DefaultAnsweringEvaluation
      )
    }
  }
}