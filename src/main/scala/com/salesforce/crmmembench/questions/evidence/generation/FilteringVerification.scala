package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.questions.evidence.Message
import com.salesforce.crmmembench.evaluation.EvaluationUtils
import com.salesforce.crmmembench.questions.evidence.{Conversation, EvidenceItem}
import com.salesforce.crmmembench.LLM_endpoints.{LLMModel, OpenAI, Gemini, Claude}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

/**
 * Static methods for verification used throughout the system.
 */
object FilteringVerification {
  
  /**
   * List of models to use for extensive verification.
   * These are models that provide good quality at reasonable cost,
   * suitable for thorough evidence verification during generation.
   * 
   * Made package-private for testing purposes.
   */
  lazy val modelsForExtensiveVerification: List[LLMModel] = List(
    OpenAI.gpt4oMini,
    Gemini.flash,
    /*Claude.sonnet*/
  )
  
  /**
   * Verify that the model answers correctly with evidence present.
   * 
   * @param evidenceItem The evidence item containing conversations and expected answer
   * @param requiredPasses Number of consecutive correct answers required
   * @param extensive If true, verifies across all cheap models (each must pass 3 times)
   * @param stats Optional stats map to track model performance (model name -> (passes, attempts))
   * @return Verification result
   */
  def verifyWithEvidence(
    evidenceItem: EvidenceItem,
    requiredPasses: Int = Config.Evidence.VERIFICATION_CONSECUTIVE_PASSES_REQUIRED,
    extensive: Boolean = true,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]] = None,
    answeringEvaluation: AnsweringEvaluation
  ): Option[VerificationResult] = {
    if (extensive) {
      // Extensive verification: test across all cheap models
      verifyWithExtensiveModels(evidenceItem, requiredPasses, stats, answeringEvaluation)
    } else {
      // Standard verification: use default model (no stats tracking)
      verifyWithSingleModel(evidenceItem, requiredPasses, answeringEvaluation)
    }
  }
  
  /**
   * Standard verification with single model
   * Requires consecutive correct answers (not attempts)
   */
  def verifyWithSingleModel(
    evidenceItem: EvidenceItem,
    requiredPasses: Int,
    answeringEvaluation: AnsweringEvaluation,
    model: LLMModel = Gemini.flash
  ): Option[VerificationResult] = {
    var consecutivePasses = 0
    var lastAnswer = ""
    
    // Continue until we get required consecutive passes or fail
    while (consecutivePasses < requiredPasses) {
      // Get model answer
      EvaluationUtils.getModelResponse(evidenceItem.conversations, evidenceItem.question, model).map(_.content) match {
        case Some(modelAnswer) =>
          lastAnswer = modelAnswer
          
          // Verify if the answer is correct
          EvaluationUtils.verifyAnswerCorrectness(
            evidenceItem.question,
            evidenceItem.answer,
            modelAnswer,
            evidenceItem.message_evidences,
            answeringEvaluation
          ) match {
            case Some(true) =>
              // Correct answer - increment consecutive passes
              consecutivePasses += 1
            case Some(false) =>
              // Incorrect answer - return failure
              return Some(VerificationResult(
                passed = false,
                lastModelAnswer = modelAnswer,
                failureReason = Some(s"Model answered incorrectly: $modelAnswer (expected: ${evidenceItem.answer})")
              ))
            case None =>
              // Error in verification
              return None
          }
        case None =>
          // Error getting model answer
          return None
      }
    }
    
    // Successfully completed all required consecutive passes
    Some(VerificationResult(passed = true, lastModelAnswer = lastAnswer))
  }
  
  /**
   * Extensive verification across multiple cheap models
   * Each model must pass the specified number of times in a row
   * Tracks statistics for each model's performance
   */
  def verifyWithExtensiveModels(
    evidenceItem: EvidenceItem,
    requiredPasses: Int,
    stats: Option[TrieMap[String, (AtomicInteger, AtomicInteger)]],
    answeringEvaluation: AnsweringEvaluation
  ): Option[VerificationResult] = {
    val cheapModels = scala.util.Random.shuffle(modelsForExtensiveVerification)
    
    for (model <- cheapModels) {
      val modelName = model.getModelName
      if (Config.DEBUG) {
        println(s"      Testing with $modelName...")
      }
      
      // Initialize stats for this model if tracking is enabled
      stats.foreach { statsMap =>
        statsMap.getOrElseUpdate(modelName, (new AtomicInteger(0), new AtomicInteger(0)))
      }
      
      // Use verifyWithSingleModel for this specific model
      val verificationResult = verifyWithSingleModel(
        evidenceItem, 
        requiredPasses, 
        answeringEvaluation,
        model
      )
      
      verificationResult match {
        case Some(result) if result.passed =>
          // Update stats - model succeeded
          stats.foreach { statsMap =>
            val (attempts, passes) = statsMap(modelName)
            attempts.incrementAndGet()
            passes.incrementAndGet()
          }
          if (Config.DEBUG) {
            println(s"        ✅ Passed all $requiredPasses consecutive passes")
          }
        case Some(result) =>
          // Update stats - model failed
          stats.foreach { statsMap =>
            val (attempts, _) = statsMap(modelName)
            attempts.incrementAndGet()
          }
          if (Config.DEBUG) {
            println(s"        ❌ Failed: ${result.lastModelAnswer} (expected: ${evidenceItem.answer})")
          }
          // Return the failure result with model name in failure reason
          return Some(result.copy(
            failureReason = result.failureReason.map(reason => s"$modelName: $reason")
          ))
        case None =>
          // Verification error
          return None
      }
    }
    
    // All models passed all attempts
    if (Config.DEBUG) {
      println(s"    ✅ All ${cheapModels.length} models passed extensive verification")
    }
    Some(VerificationResult(passed = true, lastModelAnswer = "All models passed"))
  }
  
  /**
   * Verify that the model answers incorrectly without evidence.
   * Creates a dummy conversation without evidence to test.
   * 
   * @param evidenceItem The evidence item containing the question and expected answer
   * @return Verification result
   */
  def verifyWithoutEvidence(evidenceItem: EvidenceItem, answeringEvaluation: AnsweringEvaluation): Option[VerificationResult] = {
    // Create a dummy conversation that doesn't contain evidence
    val dummyConversation = Conversation(
      messages = List(
        Message("User", "Hello, how are you today?"),
        Message("Assistant", "I'm doing well, thank you! How can I help you?"),
        Message("User", "What's the weather like?"),
        Message("Assistant", "I don't have access to real-time weather data, but I'd be happy to help you with other questions."),
        Message("User", "Can you tell me a joke?"),
        Message("Assistant", "Sure! Why don't scientists trust atoms? Because they make up everything!")
      ),
      id = Some("dummy-no-evidence"),
      containsEvidence = Some(false)
    )
    
    // Ask the question with this dummy conversation
    val result = for {
      modelResponse <- EvaluationUtils.getModelResponse(List(dummyConversation), evidenceItem.question)
    } yield modelResponse.content
    
    result match {
      case Some(modelAnswer) =>
        // Check if the answer is similar to the expected answer
        val isCorrect = EvaluationUtils.verifyAnswerCorrectness(
          evidenceItem.question, 
          evidenceItem.answer, 
          modelAnswer, 
          List.empty[Message],
          answeringEvaluation
        ).getOrElse(false)
        
        Some(VerificationResult(
          passed = !isCorrect, // We want the model to be incorrect without evidence
          lastModelAnswer = modelAnswer,
          failureReason = if (isCorrect) Some("Model should not answer correctly without evidence") else None
        ))
      case None =>
        // Failed to get answer
        None
    }
  }
}