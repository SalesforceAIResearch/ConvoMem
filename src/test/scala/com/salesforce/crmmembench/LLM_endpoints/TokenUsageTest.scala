package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.util.Success

class TokenUsageTest extends AnyFunSuite with Matchers {
  
  test("Gemini models should return token usage") {
    val models = List(
      ("Gemini Flash", Gemini.flash),
      ("Gemini Pro", Gemini.pro)
    )
    
    models.foreach { case (name, model) =>
      println(s"\n=== Testing $name ===")
      val prompt = "Write a haiku about programming in Scala."
      
      model.generateContent(prompt) match {
        case Success(response) =>
          println(s"Model: ${response.modelName}")
          println(s"Response: ${response.content}")
          
          response.tokenUsage match {
            case Some(usage) =>
              println(s"Input tokens: ${usage.inputTokens}")
              println(s"Output tokens: ${usage.outputTokens}")
              println(s"Total tokens: ${usage.totalTokens}")
              
              // Validate token counts
              usage.inputTokens should be > 0
              usage.outputTokens should be > 0
              usage.totalTokens should be > 0
              // Note: totalTokens might include additional tokens (e.g., for tool use)
              // so it can be >= inputTokens + outputTokens
              usage.totalTokens should be >= (usage.inputTokens + usage.outputTokens)
              
              println(s"✓ Token tracking working for $name")
              
            case None =>
              fail(s"No token usage returned for $name")
          }
          
        case util.Failure(e) =>
          fail(s"Failed to generate content with $name: ${e.getMessage}")
      }
    }
  }
  
  test("Claude models should return token usage") {
    val models = List(
      ("Claude Sonnet", Claude.sonnet)
      // Skipping Haiku (model not found) and Opus (to save costs) during testing
    )
    
    models.foreach { case (name, model) =>
      println(s"\n=== Testing $name ===")
      val prompt = "Write a short poem about artificial intelligence."
      
      model.generateContent(prompt) match {
        case Success(response) =>
          println(s"Model: ${response.modelName}")
          println(s"Response: ${response.content}")
          
          response.tokenUsage match {
            case Some(usage) =>
              println(s"Input tokens: ${usage.inputTokens}")
              println(s"Output tokens: ${usage.outputTokens}")
              println(s"Total tokens: ${usage.totalTokens}")
              
              // Validate token counts
              usage.inputTokens should be > 0
              usage.outputTokens should be > 0
              usage.totalTokens should be > 0
              // Note: totalTokens might include additional tokens (e.g., for tool use)
              // so it can be >= inputTokens + outputTokens
              usage.totalTokens should be >= (usage.inputTokens + usage.outputTokens)
              
              println(s"✓ Token tracking working for $name")
              
            case None =>
              fail(s"No token usage returned for $name")
          }
          
        case util.Failure(e) =>
          fail(s"Failed to generate content with $name: ${e.getMessage}")
      }
    }
  }
  
  test("JSON response models should also return token usage") {
    val prompt = """Generate a JSON object representing a book with fields: 
                   |title (string), author (string), year (number), pages (number)""".stripMargin
    
    println("\n=== Testing Gemini Flash JSON ===")
    Gemini.flashJson.generateContent(prompt) match {
      case Success(response) =>
        println(s"JSON response: ${response.content}")
        
        response.tokenUsage match {
          case Some(usage) =>
            println(s"Token usage - Input: ${usage.inputTokens}, Output: ${usage.outputTokens}, Total: ${usage.totalTokens}")
            usage.inputTokens should be > 0
            usage.outputTokens should be > 0
            
          case None =>
            fail("No token usage returned for Gemini Flash JSON")
        }
        
      case util.Failure(e) =>
        fail(s"Failed to generate JSON with Gemini Flash: ${e.getMessage}")
    }
    
    println("\n=== Testing Claude Sonnet JSON ===")
    Claude.sonnetJson.generateContent(prompt) match {
      case Success(response) =>
        println(s"JSON response: ${response.content}")
        
        response.tokenUsage match {
          case Some(usage) =>
            println(s"Token usage - Input: ${usage.inputTokens}, Output: ${usage.outputTokens}, Total: ${usage.totalTokens}")
            usage.inputTokens should be > 0
            usage.outputTokens should be > 0
            
          case None =>
            fail("No token usage returned for Claude Sonnet JSON")
        }
        
      case util.Failure(e) =>
        fail(s"Failed to generate JSON with Claude Sonnet: ${e.getMessage}")
    }
  }
}