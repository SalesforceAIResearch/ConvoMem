package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Random, Success, Try}

class RandomModelSelectorTest extends AnyFlatSpec with Matchers {
  
  // Mock LLMModel implementation for testing
  class MockLLMModel(
    name: String, 
    provider: String, 
    response: String,
    var callCount: Int = 0
  ) extends LLMModel {
    override def generateContent(prompt: String): Try[LLMResponse] = {
      callCount += 1
      Success(LLMResponse(response, name, None, 0.0))
    }
    override def getModelName: String = name
    override def getProvider: String = provider
  }
  
  "RandomModelSelector" should "require non-empty model list" in {
    an[IllegalArgumentException] should be thrownBy {
      new RandomModelSelector(List.empty)
    }
  }
  
  it should "select from single model" in {
    val mockModel = new MockLLMModel("test-model", "test-provider", "test response")
    val selector = new RandomModelSelector(List(mockModel))
    
    val result = selector.generateContent("test prompt")
    result shouldBe Success(LLMResponse("test response", "test-model", None, 0.0))
    mockModel.callCount shouldBe 1
  }
  
  it should "randomly select from multiple models" in {
    val model1 = new MockLLMModel("model1", "provider1", "response1")
    val model2 = new MockLLMModel("model2", "provider2", "response2")
    val model3 = new MockLLMModel("model3", "provider3", "response3")
    
    val models = List(model1, model2, model3)
    val selector = new RandomModelSelector(models)
    
    // Run multiple times to ensure randomness
    val responses = (1 to 30).map(_ => selector.generateContent("test").get)
    
    // Check that all models were called at least once (with high probability)
    model1.callCount should be > 0
    model2.callCount should be > 0
    model3.callCount should be > 0
    
    // Total calls should equal number of requests
    (model1.callCount + model2.callCount + model3.callCount) shouldBe 30
    
    // Responses should include all possible values
    val responseContents = responses.map(_.content).toSet
    responseContents should contain allOf ("response1", "response2", "response3")
    
    // Model names should match the selected models
    val modelNames = responses.map(_.modelName).toSet
    modelNames should contain allOf ("model1", "model2", "model3")
  }
  
  it should "use deterministic selection with seeded Random" in {
    val model1 = new MockLLMModel("model1", "provider1", "response1")
    val model2 = new MockLLMModel("model2", "provider2", "response2")
    
    val models = List(model1, model2)
    val selector1 = new RandomModelSelector(models, new Random(42))
    val selector2 = new RandomModelSelector(models, new Random(42))
    
    // With same seed, should produce same sequence
    val results1 = (1 to 10).map(_ => selector1.generateContent("test").get)
    
    // Reset call counts
    model1.callCount = 0
    model2.callCount = 0
    
    val results2 = (1 to 10).map(_ => selector2.generateContent("test").get)
    
    results1 shouldBe results2
  }
  
  it should "return combined model name" in {
    val model1 = new MockLLMModel("model1", "provider1", "response1")
    val model2 = new MockLLMModel("model2", "provider2", "response2")
    
    val selector = new RandomModelSelector(List(model1, model2))
    selector.getModelName shouldBe "RandomSelector[model1, model2]"
  }
  
  it should "return Mixed as provider" in {
    val model1 = new MockLLMModel("model1", "provider1", "response1")
    val model2 = new MockLLMModel("model2", "provider2", "response2")
    
    val selector = new RandomModelSelector(List(model1, model2))
    selector.getProvider shouldBe "Mixed"
  }
}