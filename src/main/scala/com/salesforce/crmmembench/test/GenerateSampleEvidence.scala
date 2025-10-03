package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.questions.evidence.generators._
import com.salesforce.crmmembench.questions.evidence.generation.GenerationStats

/**
 * Simple evidence generator for testing prompt changes
 */
object GenerateSampleEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating sample evidence with new question-in-scenario prompts")
    println("="*80)
    
    // Set runShort to true to generate only a few examples
    val generators = List(
      new UserFactsEvidenceGenerator(1) { override val runShort = true },
      new AssistantFactsEvidenceGenerator(1) { override val runShort = true },
      new ChangingEvidenceGenerator(2) { override val runShort = true },
      new AbstentionEvidenceGenerator(2) { override val runShort = true },
      new PreferenceEvidenceGenerator(1) { override val runShort = true }
    )
    
    generators.foreach { generator =>
      println(s"\n\nGenerating evidence for: ${generator.getEvidenceTypeName}")
      println("-"*60)
      
      try {
        generator.generateEvidence()
        println(s"✅ Successfully generated ${generator.getEvidenceTypeName} evidence")
      } catch {
        case e: Exception =>
          println(s"❌ Error generating ${generator.getEvidenceTypeName}: ${e.getMessage}")
          e.printStackTrace()
      }
    }
    
    println("\n\n" + "="*80)
    println("GENERATION COMPLETE - Check 'short_runs' directory for output")
    println("="*80)
  }
}