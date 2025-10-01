package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import com.salesforce.crmmembench.questions.evidence.Message
import com.salesforce.crmmembench.questions.evidence.generation.DefaultAnsweringEvaluation

class JudgeResponseParsingTest extends AnyFunSuite with Matchers {
  
  test("Judge response parsing should handle RIGHT/WRONG correctly") {
    // Test the parsing logic used in EvaluationUtils
    def parseJudgeResponse(response: String): Option[Boolean] = {
      val judgeResponse = response.trim.toLowerCase
      val containsRight = judgeResponse.contains("right")
      val containsWrong = judgeResponse.contains("wrong")
      
      if (containsRight && containsWrong) {
        None // Ambiguous
      } else if (containsRight) {
        Some(true)
      } else if (containsWrong) {
        Some(false)
      } else {
        None // Invalid
      }
    }
    
    // Basic cases
    parseJudgeResponse("RIGHT") shouldBe Some(true)
    parseJudgeResponse("WRONG") shouldBe Some(false)
    parseJudgeResponse("right") shouldBe Some(true)
    parseJudgeResponse("wrong") shouldBe Some(false)
    parseJudgeResponse("Right") shouldBe Some(true)
    parseJudgeResponse("Wrong") shouldBe Some(false)
    
    // With whitespace
    parseJudgeResponse("  RIGHT  ") shouldBe Some(true)
    parseJudgeResponse("  WRONG  ") shouldBe Some(false)
    
    // Invalid cases - neither right nor wrong
    parseJudgeResponse("YES") shouldBe None
    parseJudgeResponse("NO") shouldBe None
    parseJudgeResponse("CORRECT") shouldBe None
    parseJudgeResponse("INCORRECT") shouldBe None
    parseJudgeResponse("maybe") shouldBe None
    parseJudgeResponse("") shouldBe None
    
    // Ambiguous cases - contains both
    parseJudgeResponse("RIGHT WRONG") shouldBe None
    parseJudgeResponse("It's both right and wrong") shouldBe None
    
    // Natural language responses
    parseJudgeResponse("That's right!") shouldBe Some(true)
    parseJudgeResponse("You're wrong.") shouldBe Some(false)
    parseJudgeResponse("The answer is right") shouldBe Some(true)
    parseJudgeResponse("This is wrong") shouldBe Some(false)
  }
  
  test("The old CORRECT/INCORRECT substring bug") {
    // This demonstrates why CORRECT/INCORRECT was problematic
    "INCORRECT".contains("CORRECT") shouldBe true // The core issue
    
    // But RIGHT/WRONG doesn't have this problem
    "WRONG".contains("RIGHT") shouldBe false
    "RIGHT".contains("WRONG") shouldBe false
  }
  
  test("EvaluationUtils.verifyAnswerCorrectness should work with actual model") {
    // Test with a simple factual case where answer matches exactly
    val question = "What color is the sky?"
    val correctAnswer = "Blue"
    val modelResponse = "Blue"
    
    val result1 = EvaluationUtils.verifyAnswerCorrectness(
      question, 
      correctAnswer, 
      modelResponse,
      List.empty[Message],
      DefaultAnsweringEvaluation
    )
    
    result1 shouldBe defined
    result1.get shouldBe true
    
    // Test with wrong answer
    val wrongResponse = "Green"
    val result2 = EvaluationUtils.verifyAnswerCorrectness(
      question,
      correctAnswer,
      wrongResponse,
      List.empty[Message],
      DefaultAnsweringEvaluation
    )
    
    result2 shouldBe defined
    result2.get shouldBe false
    
    // Test with paraphrased correct answer
    val paraphrasedResponse = "The sky is blue"
    val result3 = EvaluationUtils.verifyAnswerCorrectness(
      question,
      correctAnswer,
      paraphrasedResponse,
      List.empty[Message],
      DefaultAnsweringEvaluation
    )
    
    result3 shouldBe defined
    result3.get shouldBe true
  }
  
  test("Judge should respond with RIGHT/WRONG format") {
    // This test verifies the actual judge prompt produces RIGHT/WRONG responses
    val question = "What is 2 + 2?"
    val correctAnswer = "4"
    val modelResponse = "4"
    
    // Get the judge prompt to inspect it
    val judgePrompt = DefaultAnsweringEvaluation.getJudgePromptTemplate(
      question,
      correctAnswer, 
      modelResponse,
      List.empty[Message]
    )
    
    // Verify the prompt asks for RIGHT/WRONG
    judgePrompt should include("Answer (RIGHT/WRONG):")
    judgePrompt should not include("yes/no")
    judgePrompt should not include("CORRECT or INCORRECT")
    
    // Actually call the model to verify it returns RIGHT/WRONG
    val result = EvaluationUtils.verifyAnswerCorrectness(
      question,
      correctAnswer,
      modelResponse,
      List.empty[Message],
      DefaultAnsweringEvaluation
    )
    
    result shouldBe defined
    result.get shouldBe true
  }
}