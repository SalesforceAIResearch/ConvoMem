package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Conversation, EvidenceGenerator}
import com.salesforce.crmmembench.questions.evidence.Message
import org.scalatest.funsuite.AnyFunSuite
import java.io.File

class CachingTestCasesGeneratorTest extends AnyFunSuite {
  
  // Helper method to clean up cache files
  def cleanupCacheFiles(): Unit = {
    val cacheDir = new File("src/main/resources/test_cases/test")
    if (cacheDir.exists()) {
      cacheDir.listFiles().foreach(_.delete())
    }
  }
  
  test("CachingTestCasesGenerator should generate and cache test cases on first run") {
    cleanupCacheFiles()
    
    // Use UserFactsEvidenceGenerator as a concrete example for testing
    import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator
    val mockEvidenceGenerator = new UserFactsEvidenceGenerator(1)
    
    // Create a mock test cases generator
    val mockGenerator = new TestCasesGenerator(mockEvidenceGenerator) {
      override def generatorType: String = "Mock Generator"
      override def generateTestCases(): List[TestCase] = {
        List(
          TestCase(
            evidenceItems = List(
              EvidenceItem(
                question = "What's the capital of France?",
                answer = "Paris",
                message_evidences = List(
                  Message("User", "The capital of France is Paris.")
                ),
                conversations = List(),
                category = "Geography",
                scenario_description = Some("Testing geography knowledge")
              )
            ),
            conversations = List(
              Conversation(
                messages = List(
                  Message("User", "The capital of France is Paris."),
                  Message("Assistant", "Thank you for that information.")
                ),
                id = Some("test-conv-1"),
                containsEvidence = Some(true)
              )
            )
          )
        )
      }
    }
    
    // Create caching generator with default overwrite=true
    val cachingGenerator = new CachingTestCasesGenerator(mockGenerator, "test/mock_cache")
    
    // First run should generate and cache
    val testCases1 = cachingGenerator.generateTestCases()
    assert(testCases1.size == 1)
    assert(testCases1.head.evidenceItems.head.question == "What's the capital of France?")
    
    // Verify cache file was created
    val cacheFile = new File("src/main/resources/test_cases/test/mock_cache.json")
    assert(cacheFile.exists())
    
    // Second run with overwrite=true should regenerate
    val cachingGenerator2 = new CachingTestCasesGenerator(mockGenerator, "test/mock_cache", overwrite = true)
    val testCases2 = cachingGenerator2.generateTestCases()
    assert(testCases2.size == 1)
    
    // Third run with overwrite=false should load from cache
    val cachingGenerator3 = new CachingTestCasesGenerator(mockGenerator, "test/mock_cache", overwrite = false)
    val testCases3 = cachingGenerator3.generateTestCases()
    
    // Should get the same data
    assert(testCases3.size == 1)
    assert(testCases3.head.evidenceItems.head.question == "What's the capital of France?")
  }
  
  test("TestCaseSerializer should correctly serialize and deserialize test cases") {
    val testCase = TestCase(
      evidenceItems = List(
        EvidenceItem(
          question = "What's your name?",
          answer = "Claude",
          message_evidences = List(
            Message("Assistant", "My name is Claude.")
          ),
          conversations = List(),
          category = "Personal",
          scenario_description = Some("Testing personal information")
        )
      ),
      conversations = List(
        Conversation(
          messages = List(
            Message("User", "What's your name?"),
            Message("Assistant", "My name is Claude.")
          ),
          id = Some("conv-1"),
          containsEvidence = Some(true)
        ),
        Conversation(
          messages = List(
            Message("User", "How's the weather?"),
            Message("Assistant", "I don't have access to weather data.")
          ),
          id = Some("conv-2"),
          containsEvidence = Some(false)
        )
      )
    )
    
    // Serialize
    val json = TestCaseSerializer.toJson(List(testCase))
    assert(json.contains("What's your name?"))
    assert(json.contains("Claude"))
    
    // Deserialize
    val deserialized = TestCaseSerializer.fromJson(json)
    assert(deserialized.isDefined)
    assert(deserialized.get.size == 1)
    
    val deserializedTestCase = deserialized.get.head
    assert(deserializedTestCase.evidenceItems.size == 1)
    assert(deserializedTestCase.conversations.size == 2)
    assert(deserializedTestCase.evidenceItems.head.answer == "Claude")
  }
}