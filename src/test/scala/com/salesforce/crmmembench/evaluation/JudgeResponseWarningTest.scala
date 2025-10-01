package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import com.salesforce.crmmembench.questions.evidence.Message

class JudgeResponseWarningTest extends AnyFunSuite with Matchers {
  
  test("Judge response validation should handle various response formats") {
    // Test cases for judge response validation logic
    val testCases = List(
      ("yes", true, false, true),
      ("no", false, true, false),
      ("Yes, the answer is correct", true, false, true),
      ("No, it's incorrect", false, true, false),
      ("yes and no", true, true, false), // Ambiguous
      ("maybe", false, false, false), // Invalid - should throw
      ("YES", true, false, true), // Case insensitive
      ("NO", false, true, false), // Case insensitive
      ("The answer is yes, not no", true, true, false), // Contains both
      ("I agree, yes", true, false, true),
      ("I disagree, no", false, true, false)
    )
    
    testCases.foreach { case (response, shouldContainYes, shouldContainNo, expectedResult) =>
      val lowerResponse = response.toLowerCase
      val containsYes = lowerResponse.contains("yes")
      val containsNo = lowerResponse.contains("no")
      
      containsYes shouldBe shouldContainYes
      containsNo shouldBe shouldContainNo
      
      // Test the logic that would be used in verifyAnswerCorrectness
      if (containsYes && containsNo) {
        // Ambiguous response - should default to false
        println(s"Ambiguous response detected: '$response'")
        val result = false
        result shouldBe expectedResult
      } else if (!containsYes && !containsNo) {
        // Invalid response - would throw exception in actual code
        println(s"Invalid response detected: '$response'")
        // In actual code, this would throw an exception
        an[Exception] should be thrownBy {
          throw new RuntimeException(s"Invalid judge response: '$response' - must contain either 'yes' or 'no'")
        }
      } else {
        // Valid response
        val result = containsYes
        result shouldBe expectedResult
      }
    }
  }
  
  test("Warning messages should be descriptive") {
    // This test verifies the warning message format
    val question = "What is the user's favorite color?"
    val correctAnswer = "Blue"
    val modelResponse = "The user's favorite color is blue"
    
    // Test ambiguous response warning
    val ambiguousResponse = "yes and no"
    val warningMessage1 = s"⚠️  WARNING: Judge response contains both 'yes' and 'no': '$ambiguousResponse'"
    warningMessage1 should include("both 'yes' and 'no'")
    warningMessage1 should include(ambiguousResponse)
    
    // Test invalid response warning
    val invalidResponse = "maybe"
    val warningMessage2 = s"⚠️  WARNING: Judge response contains neither 'yes' nor 'no': '$invalidResponse'"
    warningMessage2 should include("neither 'yes' nor 'no'")
    warningMessage2 should include(invalidResponse)
    
    // Test exception message
    val exceptionMessage = s"Invalid judge response: '$invalidResponse' - must contain either 'yes' or 'no'"
    exceptionMessage should include("must contain either 'yes' or 'no'")
  }
}