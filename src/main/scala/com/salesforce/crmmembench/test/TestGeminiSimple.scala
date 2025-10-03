package com.salesforce.crmmembench.test
import com.salesforce.crmmembench.LLM_endpoints.Gemini

object TestGeminiSimple {
  def main(args: Array[String]): Unit = {
    println("Testing Gemini Flash...")
    try {
      val result = Gemini.flash.generateContent("Say hello")
      println(s"Result: $result")
    } catch {
      case e: Exception => 
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}
