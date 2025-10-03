package com.salesforce.crmmembench.memory

import com.salesforce.crmmembench.LLM_endpoints.Gemini

object TestGeminiCall {
  def main(args: Array[String]): Unit = {
    println("Testing Gemini Flash Lite...")
    
    val prompt = """Extract facts from: "User: I like pizza. Assistant: Pizza is great!"
    |Return JSON array like: [{"content": "fact", "sessionId": "123"}]""".stripMargin
    
    println(s"Prompt: $prompt")
    
    val result = Gemini.flashLite.generateContent(prompt)
    println(s"Result: $result")
    
    result match {
      case scala.util.Success(r) => 
        println(s"Success! Content: ${r.content}")
        println(s"Model: ${r.modelName}")
      case scala.util.Failure(e) => 
        println(s"Failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}