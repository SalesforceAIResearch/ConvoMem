package com.salesforce.crmmembench.evaluation.runners

import com.salesforce.crmmembench.evaluation.{BatchedTestCasesGenerator, CachingTestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.generators._

/**
 * Generates and caches all test cases for the HuggingFace dataset release.
 * This creates pre-mixed test cases combining evidence with filler conversations
 * at various context sizes for reproducible evaluation.
 */
object GenerateAllTestCases {

  val contextSizes = List(1, 2, 3, 4, 5, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)

  def generateUserEvidence(): Unit = {
    println("\n=== Generating User Evidence Test Cases ===")
    for (count <- 1 to 6) {
      println(s"Generating user evidence with $count evidence items...")
      val generator = new UserFactsEvidenceGenerator(count)
      val testCasesGen = new CachingTestCasesGenerator(
        new BatchedTestCasesGenerator(generator, contextSizes),
        s"user_evidence/${count}_evidence/batched",
        overwrite = true
      )
      val testCases = testCasesGen.generateTestCases()
      println(s"Generated ${testCases.size} test cases for user_evidence/${count}_evidence")
    }
  }

  def generateAssistantFactsEvidence(): Unit = {
    println("\n=== Generating Assistant Facts Evidence Test Cases ===")
    for (count <- 1 to 6) {
      println(s"Generating assistant facts evidence with $count evidence items...")
      val generator = new AssistantFactsEvidenceGenerator(count)
      val testCasesGen = new CachingTestCasesGenerator(
        new BatchedTestCasesGenerator(generator, contextSizes),
        s"assistant_facts_evidence/${count}_evidence/batched",
        overwrite = true
      )
      val testCases = testCasesGen.generateTestCases()
      println(s"Generated ${testCases.size} test cases for assistant_facts_evidence/${count}_evidence")
    }
  }

  def generateChangingEvidence(): Unit = {
    println("\n=== Generating Changing Evidence Test Cases ===")
    for (count <- 2 to 6) {
      println(s"Generating changing evidence with $count evidence items...")
      val generator = new ChangingEvidenceGenerator(count)
      val testCasesGen = new CachingTestCasesGenerator(
        new BatchedTestCasesGenerator(generator, contextSizes),
        s"changing_evidence/${count}_evidence/batched",
        overwrite = true
      )
      val testCases = testCasesGen.generateTestCases()
      println(s"Generated ${testCases.size} test cases for changing_evidence/${count}_evidence")
    }
  }

  def generateAbstentionEvidence(): Unit = {
    println("\n=== Generating Abstention Evidence Test Cases ===")
    for (count <- 1 to 3) {
      println(s"Generating abstention evidence with $count evidence items...")
      val generator = new AbstentionEvidenceGenerator(count)
      val testCasesGen = new CachingTestCasesGenerator(
        new BatchedTestCasesGenerator(generator, contextSizes),
        s"abstention_evidence/${count}_evidence/batched",
        overwrite = true
      )
      val testCases = testCasesGen.generateTestCases()
      println(s"Generated ${testCases.size} test cases for abstention_evidence/${count}_evidence")
    }
  }

  def generatePreferenceEvidence(): Unit = {
    println("\n=== Generating Preference Evidence Test Cases ===")
    for (count <- 1 to 2) {
      println(s"Generating preference evidence with $count evidence items...")
      val generator = new PreferenceEvidenceGenerator(count)
      val testCasesGen = new CachingTestCasesGenerator(
        new BatchedTestCasesGenerator(generator, contextSizes),
        s"preference_evidence/${count}_evidence/batched",
        overwrite = true
      )
      val testCases = testCasesGen.generateTestCases()
      println(s"Generated ${testCases.size} test cases for preference_evidence/${count}_evidence")
    }
  }

  def generateImplicitConnectionEvidence(): Unit = {
    println("\n=== Generating Implicit Connection Evidence Test Cases ===")
    for (count <- 1 to 3) {
      println(s"Generating implicit connection evidence with $count evidence items...")
      val generator = new ImplicitConnectionEvidenceGenerator(count)
      val testCasesGen = new CachingTestCasesGenerator(
        new BatchedTestCasesGenerator(generator, contextSizes),
        s"implicit_connection_evidence/${count}_evidence/batched",
        overwrite = true
      )
      val testCases = testCasesGen.generateTestCases()
      println(s"Generated ${testCases.size} test cases for implicit_connection_evidence/${count}_evidence")
    }
  }

  def main(args: Array[String]): Unit = {
    println("Starting test case generation for all evidence types...")
    println(s"Context sizes: ${contextSizes.mkString(", ")}")

    val startTime = System.currentTimeMillis()

    try {
      generateUserEvidence()
      generateAssistantFactsEvidence()
      generateChangingEvidence()
      generateAbstentionEvidence()
      generatePreferenceEvidence()
      generateImplicitConnectionEvidence()

      val endTime = System.currentTimeMillis()
      val duration = (endTime - startTime) / 1000.0

      println(s"\n=== Test Case Generation Complete ===")
      println(s"Total time: ${duration} seconds")
      println(s"All test cases have been cached to src/main/resources/test_cases/")

    } catch {
      case e: Exception =>
        println(s"Error during test case generation: ${e.getMessage}")
        e.printStackTrace()
        System.exit(1)
    }
  }
}