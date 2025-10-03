package com.salesforce.crmmembench.questions.evidence.runners.longmemeval

import com.salesforce.crmmembench.questions.evidence.generators.longmemeval._

/**
 * Runner objects for LongMemEval generators.
 * These runners verify the imported LongMemEval data by running verification checks.
 * Each runner can be executed with ./gradlew findAndRun -PclassName=<RunnerName>
 */

object VerifyLongMemEvalMultiSession {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running verification on LongMemEval Multi-Session data")
    println("="*80)
    
    val generator = new LongMemEvalMultiSessionGenerator()
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalPreferences {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running verification on LongMemEval Preferences data")
    println("="*80)
    
    val generator = new LongMemEvalPreferencesGenerator()
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalAssistantFacts {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running verification on LongMemEval Assistant Facts data")
    println("="*80)
    
    val generator = new LongMemEvalAssistantFactsGenerator()
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalKnowledgeUpdate {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running verification on LongMemEval Knowledge Update data")
    println("="*80)
    
    val generator = new LongMemEvalKnowledgeUpdateGenerator()
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalAbstention {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running verification on LongMemEval Abstention data")
    println("="*80)
    
    val generator = new LongMemEvalAbstentionGenerator()
    generator.generateEvidence()
  }
}

/**
 * Verify all LongMemEval data types.
 */
object VerifyAllLongMemEval {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("VERIFYING ALL LONGMEMEVAL DATA")
    println("="*80)
    
    val generators = List(
      ("Multi-Session", new LongMemEvalMultiSessionGenerator()),
      ("Preferences", new LongMemEvalPreferencesGenerator()),
      ("Assistant Facts", new LongMemEvalAssistantFactsGenerator()),
      ("Knowledge Update", new LongMemEvalKnowledgeUpdateGenerator()),
      ("Abstention", new LongMemEvalAbstentionGenerator())
    )
    
    for ((name, generator) <- generators) {
      println(s"\n>>> Verifying $name <<<")
      try {
        generator.generateEvidence()
      } catch {
        case e: Exception =>
          println(s"Error verifying $name: ${e.getMessage}")
          e.printStackTrace()
      }
    }
    
    println("\n" + "="*80)
    println("ALL VERIFICATIONS COMPLETE")
    println("="*80)
  }
}