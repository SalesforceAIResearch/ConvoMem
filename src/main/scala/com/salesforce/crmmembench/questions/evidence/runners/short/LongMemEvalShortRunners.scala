package com.salesforce.crmmembench.questions.evidence.runners.short

import com.salesforce.crmmembench.questions.evidence.generators.longmemeval._

/**
 * Short runners for LongMemEval generators.
 * These runners verify only the first 10 items for quick testing.
 */

object VerifyLongMemEvalMultiSessionShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running SHORT verification on LongMemEval Multi-Session data")
    println("Verifying first 10 items only")
    println("="*80)
    
    val generator = new LongMemEvalMultiSessionGenerator() {
      override val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalPreferencesShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running SHORT verification on LongMemEval Preferences data")
    println("Verifying first 10 items only")
    println("="*80)
    
    val generator = new LongMemEvalPreferencesGenerator() {
      override val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalAssistantFactsShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running SHORT verification on LongMemEval Assistant Facts data")
    println("Verifying first 10 items only")
    println("="*80)
    
    val generator = new LongMemEvalAssistantFactsGenerator() {
      override val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalKnowledgeUpdateShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running SHORT verification on LongMemEval Knowledge Update data")
    println("Verifying first 10 items only")
    println("="*80)
    
    val generator = new LongMemEvalKnowledgeUpdateGenerator() {
      override val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalAbstentionShort {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("Running SHORT verification on LongMemEval Abstention data")
    println("Verifying first 10 items only")
    println("="*80)
    
    val generator = new LongMemEvalAbstentionGenerator() {
      override val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}