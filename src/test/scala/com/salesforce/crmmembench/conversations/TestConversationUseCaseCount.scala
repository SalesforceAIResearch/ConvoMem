package com.salesforce.crmmembench.conversations

import com.salesforce.crmmembench.{Config, Personas}
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.questions.evidence.generators._

/**
 * Test to verify that ConversationEvidenceGenerator uses the correct number of use cases.
 */
object TestConversationUseCaseCount {
  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("TESTING CONVERSATION USE CASE COUNT CONFIGURATION")
    println("=" * 80)
    
    val generator = new ConversationEvidenceGenerator()
    
    // Use reflection to access the protected getter method (Scala vals create getter methods)
    val method = classOf[EvidenceGenerator].getDeclaredMethod("useCasesPerPersonConfig")
    method.setAccessible(true)
    val useCaseCount = method.invoke(generator).asInstanceOf[Int]
    
    println(s"Default USE_CASES_PER_PERSON: ${Config.Generation.USE_CASES_PER_PERSON}")
    println(s"ConversationEvidenceGenerator useCasesPerPersonConfig: $useCaseCount")
    println()
    
    if (useCaseCount == 400) {
      println("✅ SUCCESS: ConversationEvidenceGenerator correctly overrides to 400 use cases")
    } else {
      println(s"❌ FAILURE: Expected 400 but got $useCaseCount")
    }
    
    // Test that other generators use the default
    val userFactsGenerator = new UserFactsEvidenceGenerator(1)
    val userFactsCount = method.invoke(userFactsGenerator).asInstanceOf[Int]
    
    println()
    println(s"UserFactsEvidenceGenerator useCasesPerPersonConfig: $userFactsCount")
    
    if (userFactsCount == Config.Generation.USE_CASES_PER_PERSON) {
      println("✅ SUCCESS: Other generators use the default value")
    } else {
      println(s"❌ FAILURE: Expected ${Config.Generation.USE_CASES_PER_PERSON} but got $userFactsCount")
    }
    
    // Test the prompt generation to ensure it includes the correct count
    println()
    println("Testing prompt generation...")
    
    val testPerson = Personas.Person(
      category = "Test",
      role_name = "Test Person",
      description = "A test person",
      background = Some("Test background"),
      id = "test-id"
    )
    
    val prompt = generator.getUseCaseSummaryPrompt(testPerson)
    
    if (prompt.contains("400")) {
      println("✅ SUCCESS: Prompt contains the correct count (400)")
      
      // Count occurrences
      val count = "400".r.findAllIn(prompt).length
      println(s"   Found $count occurrences of '400' in the prompt")
    } else {
      println("❌ FAILURE: Prompt does not contain '400'")
    }
    
    println()
    println("=" * 80)
    println("TEST COMPLETE")
    println("=" * 80)
  }
}