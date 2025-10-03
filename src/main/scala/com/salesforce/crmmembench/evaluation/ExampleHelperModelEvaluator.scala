package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.memory.{BlockBasedMemoryFactory, MemoryAnswererFactory}
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Example evaluator demonstrating the use of a helper model for block-based processing.
 * 
 * This evaluator uses:
 * - Main model (Gemini Flash) for final answer aggregation
 * - Helper model (Gemini Flash Lite) for block extraction
 * 
 * The helper model processes individual blocks of conversations to extract relevant information,
 * while the main model aggregates all extracted information to generate the final answer.
 * This allows for using a cheaper/faster model for the bulk of the processing (block extraction)
 * while using a more capable model for the critical final answer generation.
 */
object ExampleHelperModelEvaluator extends Evaluator {
  
  // Main model for final answer aggregation
  override def model: LLMModel = Gemini.flash
  
  // Helper model for block extraction (cheaper/faster model)
  override def helperModel: Option[LLMModel] = Some(Gemini.flashLite)
  
  // Use block-based memory system (required for helper model to be used)
  override def memoryFactory: MemoryAnswererFactory = BlockBasedMemoryFactory
  
  // Test with user facts evidence
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(1))
  
  // Run in short mode for demonstration
  override val runShort: Boolean = true
  
  override def main(args: Array[String]): Unit = {
    println("="*80)
    println("HELPER MODEL DEMONSTRATION")
    println("="*80)
    println(s"Main Model: ${model.getModelName} (for final answer aggregation)")
    println(s"Helper Model: ${helperModel.map(_.getModelName).getOrElse("None")} (for block extraction)")
    println("="*80)
    println()
    println("This evaluator demonstrates the helper model feature:")
    println("1. The helper model (Gemini Flash Lite) extracts information from each block")
    println("2. The main model (Gemini Flash) aggregates extracted info for final answer")
    println("3. This allows using a cheaper model for bulk processing")
    println()
    
    super.main(args)
  }
}

/**
 * Example evaluator using different models for block extraction and aggregation.
 * Uses GPT-4o-mini for extraction and Gemini Flash for aggregation.
 */
object ExampleCrossModelEvaluator extends Evaluator {
  import com.salesforce.crmmembench.LLM_endpoints.OpenAI
  
  // Main model for final answer aggregation (Gemini)
  override def model: LLMModel = Gemini.flash
  
  // Helper model for block extraction (OpenAI)
  override def helperModel: Option[LLMModel] = Some(OpenAI.gpt4oMini)
  
  // Use block-based memory system
  override def memoryFactory: MemoryAnswererFactory = BlockBasedMemoryFactory
  
  // Test with user facts evidence
  override def testCasesGenerator: TestCasesGenerator = 
    new BatchedTestCasesGenerator(new UserFactsEvidenceGenerator(2))
  
  // Run in short mode for demonstration
  override val runShort: Boolean = true
  
  override def main(args: Array[String]): Unit = {
    println("="*80)
    println("CROSS-MODEL EVALUATION DEMONSTRATION")
    println("="*80)
    println(s"Main Model: ${model.getModelName} (${model.getProvider}) - for final answer")
    println(s"Helper Model: ${helperModel.map(m => s"${m.getModelName} (${m.getProvider})").getOrElse("None")} - for extraction")
    println("="*80)
    println()
    println("This evaluator demonstrates cross-model processing:")
    println("1. OpenAI GPT-4o-mini extracts information from conversation blocks")
    println("2. Google Gemini Flash aggregates and generates the final answer")
    println("3. This allows leveraging strengths of different model providers")
    println()
    
    super.main(args)
  }
}