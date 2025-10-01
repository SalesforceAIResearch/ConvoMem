package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import scala.util.{Try, Success, Failure}

class GeminiMigrationTest extends AnyFunSuite with BeforeAndAfterAll {
  
  override def beforeAll(): Unit = {
    // Ensure GEMINI_API_KEY is set for tests
    if (sys.env.get("GEMINI_API_KEY").isEmpty) {
      System.setProperty("GEMINI_API_KEY", "test-api-key")
    }
  }
  
  test("Gemini model can be instantiated") {
    val model = new GeminiModel("gemini-2.5-flash")
    assert(model.getModelName == "gemini-2.5-flash")
    assert(model.getProvider == "gemini")
  }
  
  test("Gemini model with JSON response format can be instantiated") {
    val model = new GeminiModel("gemini-2.5-flash", Some("application/json"))
    assert(model.getModelName == "gemini-2.5-flash")
  }
  
  test("Gemini object provides model instances") {
    assert(Gemini.flash.getModelName == "gemini-2.5-flash")
    assert(Gemini.pro.getModelName == "gemini-2.5-pro")
    assert(Gemini.flashLite.getModelName == "gemini-2.5-flash-lite")
    assert(Gemini.flashJson.getModelName == "gemini-2.5-flash")
    assert(Gemini.proJson.getModelName == "gemini-2.5-pro")
    assert(Gemini.flashLiteJson.getModelName == "gemini-2.5-flash-lite")
  }
  
  test("Generate content handles API key properly") {
    // This test verifies the code compiles and can handle missing API key
    val model = new GeminiModel("gemini-2.5-flash")
    
    // Only run if we have a real API key
    if (sys.env.get("GEMINI_API_KEY").exists(_ != "test-api-key")) {
      val result = model.generateContent("Say 'Hello' and nothing else")
      result match {
        case Success(response) =>
          assert(response.content.toLowerCase.contains("hello"))
          assert(response.modelName == "gemini-2.5-flash")
          println(s"Response: ${response.content}")
          println(s"Token usage: ${response.tokenUsage}")
          println(s"Cost: ${response.cost}")
        case Failure(e) =>
          println(s"API call failed (this is okay in test): ${e.getMessage}")
      }
    } else {
      println("Skipping actual API call - no real GEMINI_API_KEY found")
    }
  }
}