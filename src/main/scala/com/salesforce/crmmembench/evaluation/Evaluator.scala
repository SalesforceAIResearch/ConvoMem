package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, Gemini}
import com.salesforce.crmmembench.evaluation.memory.MemoryAnswererFactory
import com.salesforce.crmmembench.questions.evidence.generation.AnsweringEvaluation

/**
 * Abstract base class for evaluation runners using the new test case generation pattern.
 * 
 * This evaluation framework tests how different memory systems perform with test cases
 * that combine evidence and irrelevant conversations. The key improvement is the separation
 * of test case generation from evaluation execution.
 * 
 * Evaluation runner objects extend this class and provide:
 * - A test cases generator (instead of just an evidence generator)
 * - Memory system configuration
 * - Other evaluation parameters
 */
abstract class Evaluator {

  /**
   * Flag to run in short mode for testing.
   * When true, only evaluates with limited context sizes and evidence items.
   */
  val runShort: Boolean = false

  /**
   * Test cases generator to use for this evaluation.
   * Each implementation provides the specific generator instance.
   * 
   * This replaces the previous pattern of providing an evidence generator
   * and context sizes separately. Now all test case generation logic
   * is encapsulated in the generator.
   */
  def testCasesGenerator: TestCasesGenerator

  /**
   * Memory factory to use for this evaluation.
   * Each implementation provides the specific memory system factory.
   */
  def memoryFactory: MemoryAnswererFactory

  /**
   * LLM model to use for this evaluation.
   * Each implementation can override to specify a custom model.
   */
  def model: LLMModel = Gemini.flash
  
  /**
   * Optional helper model for block-based processing.
   * Only used by BlockBasedMemoryAnswerer. Defaults to None.
   */
  def helperModel: Option[LLMModel] = None
  
  /**
   * Judge model to use for verifying answers.
   * By default, uses Gemini.flash. Subclasses can override for different judge models.
   */
  def judgeModel: LLMModel = Gemini.flash
  
  /**
   * AnsweringEvaluation to use for this evaluation.
   * Gets the evaluation strategy from the test cases generator's evidence generator.
   */
  def answeringEvaluation: AnsweringEvaluation = testCasesGenerator.getAnsweringEvaluation


  /**
   * Number of threads to use for processing test cases in parallel.
   * Each implementation can override to customize parallelism.
   */
  def testCaseThreads: Int = Config.Threading.EVIDENCE_ITEM_THREADS


  /**
   * Generate all test cases. Override this method to customize test case generation.
   * By default, delegates to the testCasesGenerator.
   */
  def generateAllTestCases(): List[TestCase] = testCasesGenerator.generateTestCases()

  /**
   * Run evaluation using the configured test cases generator and memory type.
   */
  def runEvaluation(): Unit = {
    println("="*80)
    println(s"${testCasesGenerator.generatorType.toUpperCase} - ${memoryFactory.name.toUpperCase} MEMORY SYSTEM")
    println("="*80)
    println(s"Mode: MULTITHREADED")
    
    if (runShort) {
      println("🚀 Running in SHORT mode: limited test cases for quick testing")
    }
    
    // Generate test cases
    println(s"\nGenerating test cases using ${testCasesGenerator.generatorType}...")
    val allTestCases = generateAllTestCases()
    
    // Check if we have any test cases at all
    if (allTestCases.isEmpty) {
      println("❌ ERROR: No test cases generated. Halting evaluation.")
      println("This might happen if:")
      println("  - The log files are empty or corrupt")
      println("  - The test case generator failed")
      println("  - No valid evidence items were found")
      return
    }
    
    // Limit test cases in short mode
    val testCases = if (runShort) {
      // Take only first few test cases for quick testing
      // This should run in about 2 minutes
      val shortTestCount = 5
      println(s"SHORT mode: Taking only first $shortTestCount test cases out of ${allTestCases.size}")
      allTestCases.take(shortTestCount)
    } else {
      allTestCases
    }
    
    println(s"\nMemory System: ${memoryFactory.name}")
    println(s"Starting systematic evaluation with ${testCases.size} test cases...\n")

    // Create and run multithreaded evaluator with test cases
    val evaluator = new MultithreadedEvaluator(
      caseType = testCasesGenerator.generatorType.toLowerCase.replace(" generator", ""),
      memoryFactory = memoryFactory,
      model = Some(model),
      helperModel = helperModel,
      testCaseThreads = if (runShort) 10 else testCaseThreads, // Reduce threads in short mode
      answeringEvaluation = answeringEvaluation,
      testCaseGeneratorType = testCasesGenerator.generatorClassType,
      judgeModel = judgeModel,
      evidenceCount = testCasesGenerator.getEvidenceCount()
    )
    
    evaluator.runEvaluation(testCases)
  }

  /**
   * Main method that evaluation runners inherit.
   * Executes the evaluation with the configured test cases generator.
   */
  def main(args: Array[String]): Unit = {
    runEvaluation()
  }
}