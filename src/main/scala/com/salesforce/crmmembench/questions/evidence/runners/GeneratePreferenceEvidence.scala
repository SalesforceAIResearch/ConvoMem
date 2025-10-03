package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.PreferenceEvidenceGenerator

/**
 * Preference evidence generation objects.
 * All preference evidence generation runnable objects in one file.
 */

/**
 * Generate 1 preference evidence conversations (single preference application).
 * Run this object directly from IDE.
 */
object GeneratePreference1Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(evidenceCount = 1)
    generator.generateEvidence()
  }
}

/**
 * Generate 2 preferences evidence conversations (multi-preference application).
 * Run this object directly from IDE.
 */
object GeneratePreference2Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(evidenceCount = 2)
    generator.generateEvidence()
  }
}

/**
 * Generate 3 preferences evidence conversations (multi-preference application).
 * Run this object directly from IDE.
 */
object GeneratePreference3Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(evidenceCount = 3)
    generator.generateEvidence()
  }
}

/**
 * Generate 4 preferences evidence conversations (multi-preference application).
 * Run this object directly from IDE.
 */
object GeneratePreference4Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(evidenceCount = 4)
    generator.generateEvidence()
  }
}

/**
 * Generate 5 preferences evidence conversations (multi-preference application).
 * Run this object directly from IDE.
 */
object GeneratePreference5Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(evidenceCount = 5)
    generator.generateEvidence()
  }
}

/**
 * Generate 6 preferences evidence conversations (multi-preference application).
 * Run this object directly from IDE.
 */
object GeneratePreference6Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new PreferenceEvidenceGenerator(evidenceCount = 6)
    generator.generateEvidence()
  }
}