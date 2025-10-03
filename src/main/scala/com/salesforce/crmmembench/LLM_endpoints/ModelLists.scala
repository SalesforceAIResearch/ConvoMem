package com.salesforce.crmmembench.LLM_endpoints

/**
 * Pre-configured lists of models grouped by cost tier
 */
object ModelLists {
  
  /**
   * Expensive models - higher quality, higher cost
   * Includes: Claude Opus, GPT-4o, Gemini Pro (regular and JSON)
   */
  lazy val EXPENSIVE_MODELS: LLMModel = new RandomModelSelector(
    List(
      Claude.sonnet,
      OpenAI.gpt4o,
      Gemini.pro
    )
  )
  
  /**
   * Cheap models - good quality, lower cost
   * Includes: GPT-4o-mini, Gemini Flash (regular and JSON)
   */
  lazy val cheapModels: LLMModel = new RandomModelSelector(
    List(
      Claude.sonnet,
      OpenAI.gpt4oMini,
      Gemini.flash
    )
  )
  

  
  /**
   * All available models as a list (not a RandomSelector)
   */
  lazy val allModels: List[LLMModel] = List(
    // Claude models
    Claude.opus,
    Claude.sonnet,
    Claude.haiku,
    // OpenAI models
    OpenAI.gpt4o,
    OpenAI.gpt4oMini,
    OpenAI.gpt4oJson,
    OpenAI.gpt4oMiniJson,
    // Gemini models
    Gemini.pro,
    Gemini.proJson,
    Gemini.flash,
    Gemini.flashJson
  )
  
  /**
   * Test all models for responsiveness and print statistics
   */
  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("MODEL RESPONSIVENESS TEST")
    println("=" * 80)
    println(s"Testing ${allModels.length} models...\n")
    
    val testPrompt = "Say Hello in one word"
    var responsiveModels = List.empty[(LLMModel, String)]
    var nonResponsiveModels = List.empty[(LLMModel, String)]
    
    allModels.foreach { model =>
      val modelInfo = s"${model.getProvider} - ${model.getModelName}"
      print(s"Testing $modelInfo... ")
      
      model.generateContent(testPrompt) match {
        case scala.util.Success(response) =>
          println(s"[SUCCESS] Response: '${response.content.trim.take(50)}' (model: ${response.modelName})")
          responsiveModels = responsiveModels :+ (model, response.content.trim)
        case scala.util.Failure(e) =>
          println(s"[FAILED] Error: ${e.getMessage}")
          nonResponsiveModels = nonResponsiveModels :+ (model, e.getMessage)
      }
    }
    
    println("\n" + "=" * 80)
    println("SUMMARY")
    println("=" * 80)
    
    println(s"\nResponsive Models (${responsiveModels.length}/${allModels.length}):")
    responsiveModels.foreach { case (model, response) =>
      println(s"  [OK] ${model.getProvider} - ${model.getModelName}")
      println(s"    Response: '${response.take(60)}'")
    }
    
    if (nonResponsiveModels.nonEmpty) {
      println(s"\nNon-Responsive Models (${nonResponsiveModels.length}/${allModels.length}):")
      nonResponsiveModels.foreach { case (model, error) =>
        println(s"  [FAIL] ${model.getProvider} - ${model.getModelName}")
        println(s"    Error: ${error.take(100)}")
      }
    } else {
      println("\nAll models are responsive! Great success!")
    }
    
    // Print statistics
    println("\n" + "=" * 80)
    println("STATISTICS")
    println("=" * 80)
    val successRate = f"${responsiveModels.length.toDouble / allModels.length * 100}%.1f"
    println(s"Success Rate: $successRate% (${responsiveModels.length}/${allModels.length})")
    
    // Group by provider
    val modelsByProvider = allModels.groupBy(_.getProvider)
    println("\nBy Provider:")
    modelsByProvider.foreach { case (provider, models) =>
      val responsiveCount = models.count(m => responsiveModels.exists(_._1 == m))
      println(s"  $provider: $responsiveCount/${models.length} responsive")
    }
  }
}