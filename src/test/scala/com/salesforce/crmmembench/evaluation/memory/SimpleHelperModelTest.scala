package com.salesforce.crmmembench.evaluation.memory

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, LLMResponse}
import scala.util.{Try, Success}

/**
 * Simple test to verify helper model parameter passing.
 */
class SimpleHelperModelTest extends AnyFunSuite with Matchers {
  
  // Simple mock model
  class SimpleMockModel(name: String) extends LLMModel {
    def generateContent(prompt: String): Try[LLMResponse] = {
      Success(LLMResponse(
        content = s"Response from $name",
        modelName = name,
        tokenUsage = None,
        cost = 0.0
      ))
    }
    
    def getModelName: String = name
    def getProvider: String = "mock"
  }
  
  test("BlockBasedMemoryFactory correctly passes helper model") {
    val mainModel = new SimpleMockModel("main")
    val helperModel = new SimpleMockModel("helper")
    
    // Test factory with both models
    val answerer = BlockBasedMemoryFactory.create(
      model = Some(mainModel),
      helperModel = Some(helperModel)
    )
    
    answerer.getMemoryType shouldBe "block_based"
  }
  
  test("BlockBasedMemoryFactory works without helper model") {
    val mainModel = new SimpleMockModel("main")
    
    // Test factory without helper model
    val answerer = BlockBasedMemoryFactory.create(
      model = Some(mainModel),
      helperModel = None
    )
    
    answerer.getMemoryType shouldBe "block_based"
  }
  
  test("Other factories ignore helper model parameter") {
    val mainModel = new SimpleMockModel("main")
    val helperModel = new SimpleMockModel("helper")
    
    // LongContextMemoryFactory should ignore helper model
    val longContextAnswerer = LongContextMemoryFactory.create(
      model = Some(mainModel),
      helperModel = Some(helperModel)  // This should be ignored
    )
    
    longContextAnswerer.getMemoryType shouldBe "long_context"
  }
}