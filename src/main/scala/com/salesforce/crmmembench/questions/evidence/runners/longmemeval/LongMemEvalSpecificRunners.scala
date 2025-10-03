package com.salesforce.crmmembench.questions.evidence.runners.longmemeval

import com.salesforce.crmmembench.questions.evidence.generators.longmemeval._
import com.salesforce.crmmembench.questions.evidence._

/**
 * Specific runners for each LongMemEval evidence count that exists.
 * Each runner targets a specific evidence directory.
 */

// Multi-Session runners (5 different evidence counts)
object VerifyLongMemEvalMultiSession1 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalMultiSessionGenerator(1)
    println(s"Running verification on Multi-Session with 1 evidence")
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalMultiSession2 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalMultiSessionGenerator(2)
    println(s"Running verification on Multi-Session with 2 evidence")
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalMultiSession3 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalMultiSessionGenerator(3)
    println(s"Running verification on Multi-Session with 3 evidence")
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalMultiSession4 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalMultiSessionGenerator(4)
    println(s"Running verification on Multi-Session with 4 evidence")
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalMultiSession5 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalMultiSessionGenerator(5)
    println(s"Running verification on Multi-Session with 5 evidence")
    generator.generateEvidence()
  }
}

// Abstention runners (only 1 and 2 - we consolidated 3 and 4 into 2)
object VerifyLongMemEvalAbstention1 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalAbstentionGenerator(1)
    println(s"Running verification on Abstention with 1 evidence")
    generator.generateEvidence()
  }
}

object VerifyLongMemEvalAbstention2 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalAbstentionGenerator(2)
    println(s"Running verification on Abstention with 2 evidence (includes consolidated 3 & 4)")
    generator.generateEvidence()
  }
}

// Assistant Facts runner (only 1 evidence count)
object VerifyLongMemEvalAssistantFacts1 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalAssistantFactsGenerator(1)
    println(s"Running verification on Assistant Facts with 1 evidence")
    generator.generateEvidence()
  }
}

// Knowledge Updates runner (only 2 evidence count)
object VerifyLongMemEvalKnowledgeUpdates2 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalKnowledgeUpdateGenerator(2)
    println(s"Running verification on Knowledge Updates with 2 evidence")
    generator.generateEvidence()
  }
}

// Preferences runner (only 1 evidence count)
object VerifyLongMemEvalPreferences1 {
  def main(args: Array[String]): Unit = {
    val generator = new LongMemEvalPreferencesGenerator(1)
    println(s"Running verification on Preferences with 1 evidence")
    generator.generateEvidence()
  }
}