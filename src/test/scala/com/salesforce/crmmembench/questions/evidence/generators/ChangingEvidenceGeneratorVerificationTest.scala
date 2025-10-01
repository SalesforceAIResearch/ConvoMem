package com.salesforce.crmmembench.questions.evidence.generators

import com.salesforce.crmmembench.questions.evidence.generation._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class ChangingEvidenceGeneratorVerificationTest extends AnyWordSpec with Matchers {
  
  "ChangingEvidenceGenerator" should {
    
    "include VerifyWithPartialEvidenceForChanging in its verification checks" in {
      // Create a test subclass to access protected method
      class TestChangingEvidenceGenerator(evidenceCount: Int) extends ChangingEvidenceGenerator(evidenceCount) {
        def getChecks(): List[VerificationCheck] = getVerificationChecks()
      }
      
      val generator = new TestChangingEvidenceGenerator(evidenceCount = 3)
      val checks = generator.getChecks()
      
      // Should have exactly 4 checks
      checks.length shouldBe 4
      
      // Check types of verification
      checks.map(_.getClass.getSimpleName) should contain allOf(
        "VerifyWithEvidence",
        "VerifyWithoutEvidence", 
        "VerifyWithPartialEvidenceForChanging",
        "VerifyIntermediateEvidenceAddresses"
      )
      
      // Verify the specific check for changing evidence is present
      checks.exists(_.isInstanceOf[VerifyWithPartialEvidenceForChanging]) shouldBe true
    }
    
    "have the correct verification check names" in {
      // Create a test subclass to access protected method
      class TestChangingEvidenceGenerator(evidenceCount: Int) extends ChangingEvidenceGenerator(evidenceCount) {
        def getChecks(): List[VerificationCheck] = getVerificationChecks()
      }
      
      val generator = new TestChangingEvidenceGenerator(evidenceCount = 2)
      val checks = generator.getChecks()
      
      val checkNames = checks.map(_.name)
      checkNames should contain allOf(
        "with_evidence",
        "without_evidence",
        "partial_evidence_latest",
        "intermediate_evidence_addresses_question"
      )
    }
    
    "use the same verification checks regardless of evidence count" in {
      // Create a test subclass to access protected method
      class TestChangingEvidenceGenerator(evidenceCount: Int) extends ChangingEvidenceGenerator(evidenceCount) {
        def getChecks(): List[VerificationCheck] = getVerificationChecks()
      }
      
      val generator2 = new TestChangingEvidenceGenerator(evidenceCount = 2)
      val generator3 = new TestChangingEvidenceGenerator(evidenceCount = 3)
      val generator4 = new TestChangingEvidenceGenerator(evidenceCount = 4)
      
      val checks2 = generator2.getChecks()
      val checks3 = generator3.getChecks()
      val checks4 = generator4.getChecks()
      
      // All should have the same number of checks
      checks2.length shouldBe 4
      checks3.length shouldBe 4
      checks4.length shouldBe 4
      
      // All should have the same check types
      checks2.map(_.name) shouldBe checks3.map(_.name)
      checks3.map(_.name) shouldBe checks4.map(_.name)
    }
  }
}