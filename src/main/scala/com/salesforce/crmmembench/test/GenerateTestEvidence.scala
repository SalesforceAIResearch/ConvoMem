package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.questions.evidence.generators._

/**
 * Generate test evidence to verify prompt changes
 */
object GenerateUserFactsTest {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(1) {
      override val runShort = true
      override lazy val useCasesPerPersonConfig = 3 // Just generate 3 use cases per person
    }
    generator.generateEvidence()
  }
}

object GenerateAssistantFactsTest {
  def main(args: Array[String]): Unit = {
    val generator = new AssistantFactsEvidenceGenerator(1) {
      override val runShort = true
      override lazy val useCasesPerPersonConfig = 3
    }
    generator.generateEvidence()
  }
}

object GenerateChangingTest {
  def main(args: Array[String]): Unit = {
    val generator = new ChangingEvidenceGenerator(2) {
      override val runShort = true
      override lazy val useCasesPerPersonConfig = 3
    }
    generator.generateEvidence()
  }
}

object GenerateAbstentionTest {
  def main(args: Array[String]): Unit = {
    val generator = new AbstentionEvidenceGenerator(2) {
      override val runShort = true
      override lazy val useCasesPerPersonConfig = 3
    }
    generator.generateEvidence()
  }
}

object GeneratePreferenceTest {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(1) {
      override val runShort = true
      override lazy val useCasesPerPersonConfig = 3
    }
    generator.generateEvidence()
  }
}