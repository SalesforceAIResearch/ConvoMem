package com.salesforce.crmmembench.evaluation.memory

import com.salesforce.crmmembench.LLM_endpoints.LLMModel
import com.salesforce.crmmembench.memory.mem0.{Mem0Client, Mem0MemoryAnswerer}
import com.salesforce.crmmembench.evaluation.EvaluationLogger.EvaluationLogEntry

/**
 * Base trait for memory answerer factories.
 * Each memory type should have its own singleton factory implementation.
 */
trait MemoryAnswererFactory {
  /**
   * Create a new MemoryAnswerer instance.
   * 
   * @param model Optional LLM model to use (defaults to implementation's default)
   * @param helperModel Optional helper model for block-based processing (only used by BlockBasedMemoryAnswerer)
   * @return New instance of the MemoryAnswerer
   */
  def create(model: Option[LLMModel] = None, helperModel: Option[LLMModel] = None): MemoryAnswerer
  
  /**
   * Get the name of this memory type for logging/reporting
   */
  def name: String
}

/**
 * Factory for creating LongContextMemoryAnswerer instances
 */
object LongContextMemoryFactory extends MemoryAnswererFactory {
  override def create(model: Option[LLMModel] = None, helperModel: Option[LLMModel] = None): MemoryAnswerer = {
    // LongContextMemoryAnswerer doesn't use helper model, so we ignore it
    model match {
      case Some(m) => new LongContextMemoryAnswerer(m)
      case None => new LongContextMemoryAnswerer()
    }
  }
  
  override def name: String = "long_context"
}

/**
 * Factory for creating Mem0MemoryAnswerer instances
 */
object Mem0MemoryFactory extends MemoryAnswererFactory {
  override def create(model: Option[LLMModel] = None, helperModel: Option[LLMModel] = None): MemoryAnswerer = {
    // Mem0MemoryAnswerer doesn't use helper model, so we ignore it
    model match {
      case Some(m) => new Mem0MemoryAnswerer(model = m)
      case None => new Mem0MemoryAnswerer()
    }
  }
  
  override def name: String = "mem0"
}

/**
 * Factory for creating BlockBasedMemoryAnswerer instances
 */
object BlockBasedMemoryFactory extends MemoryAnswererFactory {
  override def create(model: Option[LLMModel] = None, helperModel: Option[LLMModel] = None): MemoryAnswerer = {
    import com.salesforce.crmmembench.LLM_endpoints.Gemini
    
    // Helper model is required for BlockBasedMemoryAnswerer
    val helper = helperModel.getOrElse(Gemini.flashLite)
    
    model match {
      case Some(m) => new BlockBasedMemoryAnswerer(m, helperModel = helper)
      case None => new BlockBasedMemoryAnswerer(helperModel = helper)
    }
  }
  
  override def name: String = "block_based"
}

/**
 * Factory for creating ExtractedContextMemoryAnswerer instances
 */
object ExtractedContextMemoryFactory extends MemoryAnswererFactory {
  override def create(model: Option[LLMModel] = None, helperModel: Option[LLMModel] = None): MemoryAnswerer = {
    import com.salesforce.crmmembench.LLM_endpoints.Gemini
    
    // Helper model is required for ExtractedContextMemoryAnswerer
    val helper = helperModel.getOrElse(Gemini.flashLite)
    
    model match {
      case Some(m) => new ExtractedContextMemoryAnswerer(m, helper)
      case None => new ExtractedContextMemoryAnswerer(helperModel = helper)
    }
  }
  
  override def name: String = "extracted_context"
}


/**
 * Factory for creating CachedLogMemoryAnswerer instances.
 * This factory preserves the original memory system's name to ensure proper stats tracking.
 * 
 * @param testCaseIdToLogEntry Mapping from test case ID to log entry
 * @param originalMemoryType The name of the original memory system
 */
class CachedLogMemoryFactory(
  testCaseIdToLogEntry: Map[String, EvaluationLogEntry],
  originalMemoryType: String
) extends MemoryAnswererFactory {
  
  override def create(model: Option[LLMModel] = None, helperModel: Option[LLMModel] = None): MemoryAnswerer = {
    // Both model parameters are ignored since we're using cached answers
    // Build map from test case ID to answers using the provided mapping
    val testCaseAnswerMap = testCaseIdToLogEntry.map { case (testCaseId, entry) =>
      val modelAnswer = entry.contextTestResult.modelAnswer
      val answerResult = entry.answerResult
      (testCaseId, (modelAnswer, answerResult))
    }
    
    new CachedLogMemoryAnswerer(testCaseAnswerMap, originalMemoryType)
  }
  
  override def name: String = originalMemoryType  // Preserve original memory type name
}