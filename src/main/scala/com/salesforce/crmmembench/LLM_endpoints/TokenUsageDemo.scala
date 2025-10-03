package com.salesforce.crmmembench.LLM_endpoints

import scala.util.{Success, Failure}

object TokenUsageDemo {
  def main(args: Array[String]): Unit = {
    println("=== Token Usage Tracking Demo ===\n")
    
    // Test Gemini Flash
    println("Testing Gemini Flash:")
    val geminiPrompt = "What is the capital of France? Please answer in one sentence."
    Gemini.flash.generateContent(geminiPrompt) match {
      case Success(response) =>
        println(s"Response: ${response.content}")
        response.tokenUsage match {
          case Some(usage) =>
            println(s"Token usage:")
            println(s"  - Input tokens: ${usage.inputTokens}")
            println(s"  - Output tokens: ${usage.outputTokens}")
            println(s"  - Total tokens: ${usage.totalTokens}")
          case None =>
            println("No token usage data available")
        }
      case Failure(e) =>
        println(s"Error: ${e.getMessage}")
    }
    
    println("\n" + "="*50 + "\n")
    
    // Test Claude Sonnet
    println("Testing Claude Sonnet:")
    val claudePrompt = "What is 2 + 2? Please provide just the number."
    Claude.sonnet.generateContent(claudePrompt) match {
      case Success(response) =>
        println(s"Response: ${response.content}")
        response.tokenUsage match {
          case Some(usage) =>
            println(s"Token usage:")
            println(s"  - Input tokens: ${usage.inputTokens}")
            println(s"  - Output tokens: ${usage.outputTokens}")
            println(s"  - Total tokens: ${usage.totalTokens}")
          case None =>
            println("No token usage data available")
        }
      case Failure(e) =>
        println(s"Error: ${e.getMessage}")
    }
    
    println("\n" + "="*50 + "\n")
    
    // Test JSON responses
    println("Testing Gemini Flash JSON:")
    val jsonPrompt = """Create a JSON object for a person with fields: name, age, city"""
    Gemini.flashJson.generateContent(jsonPrompt) match {
      case Success(response) =>
        println(s"Response: ${response.content}")
        response.tokenUsage match {
          case Some(usage) =>
            println(s"Token usage:")
            println(s"  - Input tokens: ${usage.inputTokens}")
            println(s"  - Output tokens: ${usage.outputTokens}")
            println(s"  - Total tokens: ${usage.totalTokens}")
          case None =>
            println("No token usage data available")
        }
      case Failure(e) =>
        println(s"Error: ${e.getMessage}")
    }
  }
}