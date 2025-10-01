package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.questions.evidence.{Conversation, Message}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MemoryPromptUtilsTest extends AnyFunSuite with Matchers {

  test("getJudgeEvaluationCriteria should include all key evaluation points") {
    val criteria = MemoryPromptUtils.getJudgeEvaluationCriteria
    
    // Check that all key evaluation criteria are mentioned
    criteria should include("Core Information Accuracy")
    criteria should include("Completeness")
    criteria should include("Factual Correctness")
    criteria should include("Appropriate Synthesis")
    criteria should include("Clarity")
    criteria should include("I don't know")
  }


  test("buildMemoryBasedPrompt with memories should include judge criteria and all memories") {
    val memories = List(
      "User's favorite color is blue",
      "User lives in San Francisco",
      "User works as a software engineer"
    )
    
    val question = "What is my job?"
    val prompt = MemoryPromptUtils.buildMemoryBasedPrompt(question, memories)
    
    // Check that judge criteria are included
    prompt should include("Core Information Accuracy")
    
    // Check that all memories are included and numbered
    prompt should include("1. User's favorite color is blue")
    prompt should include("2. User lives in San Francisco")
    prompt should include("3. User works as a software engineer")
    
    // Check that the question is included
    prompt should include(question)
    
    // Check instruction about incorporating all memories
    prompt should include("incorporate ALL relevant information")
  }

  test("buildMemoryBasedPrompt with no memories should ask to say 'I don't know'") {
    val memories = List.empty[String]
    val question = "What is my favorite food?"
    val prompt = MemoryPromptUtils.buildMemoryBasedPrompt(question, memories)
    
    // Check that judge criteria are included
    prompt should include("Core Information Accuracy")
    
    // Check that it mentions no memories were found
    prompt should include("No relevant memories were found")
    
    // Check that it instructs to say "I don't know"
    prompt should include("I don't know")
    
    // Check that the question is included
    prompt should include(question)
  }

}