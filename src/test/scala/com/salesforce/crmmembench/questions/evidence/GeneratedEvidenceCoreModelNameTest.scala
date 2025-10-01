package com.salesforce.crmmembench.questions.evidence

import com.salesforce.crmmembench.evaluation.{BatchedTestCasesGenerator, StandardTestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.generators.{UserFactsEvidenceGenerator, AssistantFactsEvidenceGenerator}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.funsuite.AnyFunSuite

class GeneratedEvidenceCoreModelNameTest extends AnyFunSuite {
  
  test("BatchedTestCasesGenerator should use evidence generator name") {
    val userFactsGen = new UserFactsEvidenceGenerator(evidenceCount = 2)
    val batchedGen = new BatchedTestCasesGenerator(userFactsGen)
    
    assert(batchedGen.generatorType == "User facts Generator")
    
    // Simulate the processing in Evaluator and MultithreadedEvaluator
    val processedName = batchedGen.generatorType.toLowerCase
      .replace(" generator", "")
      .replace(" ", "_")
    
    assert(processedName == "user_facts")
  }
  
  test("StandardTestCasesGenerator should use evidence generator name") {
    val assistantFactsGen = new AssistantFactsEvidenceGenerator(evidenceCount = 1)
    val standardGen = new StandardTestCasesGenerator(assistantFactsGen, contextSizes = List(10, 20))
    
    assert(standardGen.generatorType == "Standard Assistant fact Generator")
    
    // Simulate the processing
    val processedName = standardGen.generatorType.toLowerCase
      .replace("standard ", "")
      .replace(" generator", "")
      .replace(" ", "_")
    
    assert(processedName == "assistant_fact")
  }
  
  test("GeneratedEvidenceCore should serialize and deserialize with model_name field") {
    val evidenceCore = GeneratedEvidenceCore(
      question = "What is my favorite color?",
      answer = "Blue",
      message_evidences = List(
        Message("User", "My favorite color is blue.")
      ),
      model_name = Some("gemini-1.5-flash")
    )
    
    val json = evidenceCore.asJson.noSpaces
    assert(json.contains("\"model_name\":\"gemini-1.5-flash\""))
    
    val decoded = decode[GeneratedEvidenceCore](json)
    assert(decoded.isRight)
    assert(decoded.toOption.get == evidenceCore)
  }
  
  test("GeneratedEvidenceCore should handle missing model_name field for backwards compatibility") {
    val jsonWithoutModelName = """{"question":"What is my favorite color?","answer":"Blue","message_evidences":[{"speaker":"User","text":"My favorite color is blue."}]}"""
    
    val decoded = decode[GeneratedEvidenceCore](jsonWithoutModelName)
    assert(decoded.isRight)
    
    val evidenceCore = decoded.toOption.get
    assert(evidenceCore.question == "What is my favorite color?")
    assert(evidenceCore.answer == "Blue")
    assert(evidenceCore.message_evidences.length == 1)
    assert(evidenceCore.message_evidences.head.speaker == "User")
    assert(evidenceCore.message_evidences.head.text == "My favorite color is blue.")
    assert(evidenceCore.model_name.isEmpty)
  }
  
  test("GeneratedEvidenceCore should preserve model_name through copy operation") {
    val original = GeneratedEvidenceCore(
      question = "Test question",
      answer = "Test answer",
      message_evidences = List(Message("User", "Test message"))
    )
    
    val withModelName = original.copy(model_name = Some("gpt-4o"))
    
    assert(withModelName.question == "Test question")
    assert(withModelName.answer == "Test answer")
    assert(withModelName.message_evidences == List(Message("User", "Test message")))
    assert(withModelName.model_name == Some("gpt-4o"))
  }
  
  test("GeneratedEvidenceCore with multiple evidence messages and model_name") {
    val evidenceCore = GeneratedEvidenceCore(
      question = "What tools do I use?",
      answer = "Slack and Jira",
      message_evidences = List(
        Message("User", "I use Slack for communication."),
        Message("User", "I also use Jira for project management.")
      ),
      model_name = Some("claude-3-sonnet")
    )
    
    val json = evidenceCore.asJson.noSpaces
    val decoded = decode[GeneratedEvidenceCore](json)
    
    assert(decoded.isRight)
    val decodedCore = decoded.toOption.get
    assert(decodedCore.message_evidences.length == 2)
    assert(decodedCore.model_name == Some("claude-3-sonnet"))
  }
}