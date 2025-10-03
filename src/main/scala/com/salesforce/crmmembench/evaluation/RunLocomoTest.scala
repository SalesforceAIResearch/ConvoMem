package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.evaluation.generators.{LocomoTestCasesGenerator, LocomoCategories}

/**
 * Simple runner to test Locomo data loading.
 */
object RunLocomoTest extends App {
  
  println("="*80)
  println("LOCOMO DATA LOADING TEST")
  println("="*80)
  
  // Expected counts from Python conversion
  val expectedCounts = Map(
    1 -> 282,  // Basic facts
    2 -> 321,  // Temporal
    3 -> 96,   // Reasoning
    4 -> 841,  // Multi-session
    5 -> 446   // Abstention
  )
  
  var totalQuestions = 0
  var allPassed = true
  
  // Test each category
  for (category <- 1 to 5) {
    val categoryObj = LocomoCategories.fromNumber(category).get
    println(s"\nCategory $category: ${categoryObj.description}")
    
    try {
      val generator = LocomoTestCasesGenerator(categoryObj)
      val testCases = generator.generateTestCases()
      val questionCount = testCases.flatMap(_.evidenceItems).size
      
      println(s"  Loaded: ${testCases.size} test cases, $questionCount questions")
      println(s"  Expected: ${expectedCounts(category)} questions")
      
      if (questionCount == expectedCounts(category)) {
        println(s"  ✅ PASS")
      } else {
        println(s"  ❌ FAIL - Mismatch!")
        allPassed = false
      }
      
      totalQuestions += questionCount
      
      // Show sample question
      if (testCases.nonEmpty && testCases.head.evidenceItems.nonEmpty) {
        val firstQ = testCases.head.evidenceItems.head
        println(s"  Sample Q: ${firstQ.question.take(60)}...")
        println(s"  Sample A: ${firstQ.answer.take(40)}...")
      }
      
    } catch {
      case e: Exception =>
        println(s"  ❌ ERROR: ${e.getMessage}")
        allPassed = false
    }
  }
  
  println("\n" + "="*80)
  println("SUMMARY")
  println("="*80)
  println(s"Total questions loaded: $totalQuestions")
  println(s"Expected total: ${expectedCounts.values.sum}")
  
  if (totalQuestions == expectedCounts.values.sum && allPassed) {
    println("✅ ALL TESTS PASSED!")
  } else {
    println("❌ SOME TESTS FAILED")
    System.exit(1)
  }
}