package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GeminiModelsTest extends AnyFlatSpec with Matchers {
  
  "Gemini object" should "have all expected model variations" in {
    // Check that all models exist
    Gemini.flash should not be null
    Gemini.pro should not be null
    Gemini.flashJson should not be null
    Gemini.proJson should not be null
    
    // Check model names
    Gemini.flash.getModelName shouldBe "gemini-2.5-flash"
    Gemini.pro.getModelName shouldBe "gemini-2.5-pro"
    Gemini.flashJson.getModelName shouldBe "gemini-2.5-flash"
    Gemini.proJson.getModelName shouldBe "gemini-2.5-pro"
    
    // Check provider
    Gemini.flash.getProvider shouldBe "gemini"
    Gemini.pro.getProvider shouldBe "gemini"
    Gemini.flashJson.getProvider shouldBe "gemini"
    Gemini.proJson.getProvider shouldBe "gemini"
  }
  
  it should "have unique model objects" in {
    // Check that all models are unique
    val allModels = List(Gemini.flash, Gemini.pro, Gemini.flashJson, Gemini.proJson)
    allModels.distinct.size shouldBe allModels.size
    
    // Verify JSON models have different behavior than regular ones
    Gemini.flash should not be Gemini.flashJson
    Gemini.pro should not be Gemini.proJson
  }
}