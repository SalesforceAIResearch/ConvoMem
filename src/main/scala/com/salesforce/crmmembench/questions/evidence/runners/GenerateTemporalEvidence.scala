package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.TemporalEvidenceGenerator

/**
 * Consolidated temporal evidence generation objects.
 * All temporal evidence generation runnable objects in one file.
 */

/**
 * Generate 1 temporal evidence conversations (single temporal fact).
 * Tests duration calculation, age tracking, or time elapsed scenarios.
 * Run this object directly from IDE.
 */
object GenerateTemporal1Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new TemporalEvidenceGenerator(evidenceCount = 1)
    generator.generateEvidence()
  }
}

/**
 * Generate 2 temporal evidence conversations (temporal comparison).
 * Tests event sequencing, duration between events, or temporal state comparison.
 * Run this object directly from IDE.
 */
object GenerateTemporal2Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new TemporalEvidenceGenerator(evidenceCount = 2)
    generator.generateEvidence()
  }
}

/**
 * Generate 3 temporal evidence conversations (complex timeline).
 * Tests timeline tracking, temporal aggregation, or multi-stage processes.
 * Run this object directly from IDE.
 */
object GenerateTemporal3Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new TemporalEvidenceGenerator(evidenceCount = 3)
    generator.generateEvidence()
  }
}

/**
 * Generate 4 temporal evidence conversations (extended timeline).
 * Tests complex temporal patterns with multiple events.
 * Run this object directly from IDE.
 */
object GenerateTemporal4Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new TemporalEvidenceGenerator(evidenceCount = 4)
    generator.generateEvidence()
  }
}

/**
 * Generate 5 temporal evidence conversations (comprehensive timeline).
 * Tests sophisticated temporal reasoning across multiple time points.
 * Run this object directly from IDE.
 */
object GenerateTemporal5Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new TemporalEvidenceGenerator(evidenceCount = 5)
    generator.generateEvidence()
  }
}

/**
 * Generate 6 temporal evidence conversations (maximum complexity).
 * Tests the most complex temporal scenarios with multiple interconnected events.
 * Run this object directly from IDE.
 */
object GenerateTemporal6Evidence {
  def main(args: Array[String]): Unit = {
    val generator = new TemporalEvidenceGenerator(evidenceCount = 6)
    generator.generateEvidence()
  }
}