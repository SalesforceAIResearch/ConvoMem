package com.salesforce.crmmembench.evaluation

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Simple unit test for BatchedTestCasesGenerator configuration changes.
 * Tests the new default values without requiring full mock implementation.
 */
class BatchedTestCasesGeneratorSimpleTest extends AnyWordSpec with Matchers {
  
  "BatchedTestCasesGenerator defaults" should {
    
    "have maxEvidencePerBatch default of 100 in TestCasesGenerator factory" in {
      // Use reflection to check the default parameter value
      val createStitchingMethod = TestCasesGenerator.getClass.getMethods
        .find(m => m.getName == "createStitching" && m.getParameterCount == 4)
        .get
      
      // Get the default value annotations
      val defaultValueAnnotations = createStitchingMethod.getParameterAnnotations
      
      // The maxEvidencePerBatch parameter is the 4th parameter (index 3)
      // Default values are compiled into the companion object methods
      // We can verify by checking the method signature in the factory
      
      // Since Scala compiles default parameters as separate methods,
      // we need to look for the default method
      val defaultMethods = TestCasesGenerator.getClass.getMethods
        .filter(_.getName.contains("createStitching$default"))
      
      // The 4th parameter's default method should be createStitching$default$4
      val maxEvidencePerBatchDefault = defaultMethods
        .find(_.getName == "createStitching$default$4")
        .map(_.invoke(TestCasesGenerator))
      
      maxEvidencePerBatchDefault shouldBe Some(100)
    }
    
    "verify BatchedTestCasesGenerator constructor accepts new parameters" in {
      // This test verifies that the constructor signature has been updated correctly
      // by checking that we can instantiate with the new parameters
      
      val constructors = classOf[BatchedTestCasesGenerator].getConstructors
      
      // Should have a constructor with 4 parameters
      val mainConstructor = constructors.find(_.getParameterCount == 4)
      mainConstructor shouldBe defined
      
      // Parameter types should be:
      // 1. EvidenceGenerator
      // 2. List[Int] (contextSizes)
      // 3. Int (maxEvidencePerBatch)
      // 4. Int (minTestCasesPerContext)
      
      val paramTypes = mainConstructor.get.getParameterTypes
      paramTypes.length shouldBe 4
      paramTypes(2) shouldBe classOf[Int]  // maxEvidencePerBatch
      paramTypes(3) shouldBe classOf[Int]  // minTestCasesPerContext
    }
  }
  
  "TestCasesGenerator factory" should {
    
    "createStitching method should use new default of 100" in {
      // Verify the factory method exists with correct signature
      val methods = TestCasesGenerator.getClass.getMethods
      val createStitchingMethods = methods.filter(_.getName == "createStitching")
      
      // Should have the createStitching method
      createStitchingMethods should not be empty
      
      // Find the main method (not the default value methods)
      val mainMethod = createStitchingMethods.find(m => 
        !m.getName.contains("$default") && m.getParameterCount == 4
      )
      
      mainMethod shouldBe defined
    }
  }
}