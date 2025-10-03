package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator

/**
 * Consolidated user facts evidence generation objects.
 * All user facts evidence generation runnable objects in one file.
 */

/**
 * Generate 1 user fact evidence conversations (single fact recall).
 * Run this object directly from IDE.
 */
object GenerateUserFacts1Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 1) {
      override val targetPeopleCount: Int = 50
    }
    generator.generateEvidence()
  }
}

/**
 * Generate 2 user facts evidence conversations (multi-fact aggregation).
 * Run this object directly from IDE.
 */
object GenerateUserFacts2Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 2)
    generator.generateEvidence()
  }
}

/**
 * Generate 3 user facts evidence conversations (multi-fact aggregation).
 * Run this object directly from IDE.
 */
object GenerateUserFacts3Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 3)
    generator.generateEvidence()
  }
}

/**
 * Generate 4 user facts evidence conversations (multi-fact aggregation).
 * Run this object directly from IDE.
 */
object GenerateUserFacts4Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 4)
    generator.generateEvidence()
  }
}

/**
 * Generate 5 user facts evidence conversations (multi-fact aggregation).
 * Run this object directly from IDE.
 */
object GenerateUserFacts5Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 5)
    generator.generateEvidence()
  }
}

/**
 * Generate 6 user facts evidence conversations (multi-fact aggregation).
 * Run this object directly from IDE.
 */
object GenerateUserFacts6Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new UserFactsEvidenceGenerator(evidenceCount = 6)
    generator.generateEvidence()
  }
}