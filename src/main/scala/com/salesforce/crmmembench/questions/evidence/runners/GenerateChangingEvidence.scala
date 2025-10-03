package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.ChangingEvidenceGenerator

/**
 * Entry points for generating changing evidence conversations.
 * 
 * Changing evidence tests scenarios where facts evolve over time:
 * - Initial state: "I'm going to Paris"
 * - Change: "My trip got cancelled" 
 * - Question: "What travel plans do I have?"
 * - Answer: "No travel plans since Paris trip got cancelled"
 * 
 * Supports scalable evidence and change counts.
 */

/**
 * Generate changing evidence with 2 messages (1 change).
 * Pattern: Initial state → Final state
 * Example: "Meeting at 3pm" → "Meeting cancelled"
 */
object Generate2ChangingEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 2-message changing evidence (1 change)...")
    val generator = new ChangingEvidenceGenerator(evidenceCount = 2) {
      override val targetPeopleCount: Int = 75
    }
    generator.generateEvidence()
  }
}

/**
 * Generate changing evidence with 3 messages (2 changes).
 * Pattern: Initial state → Intermediate change → Final state
 * Example: "Meeting at 3pm" → "Moved to 4pm" → "Meeting cancelled"
 */
object Generate3ChangingEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 3-message changing evidence (2 changes)...")
    val generator = new ChangingEvidenceGenerator(evidenceCount = 3)
    generator.generateEvidence()
  }
}

/**
 * Generate changing evidence with 4 messages (3 changes).
 * Pattern: Initial state → First change → Second change → Final state
 * Example: "Trip to Paris" → "Changed to London" → "Moved to next month" → "Trip cancelled"
 */
object Generate4ChangingEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 4-message changing evidence (3 changes)...")
    val generator = new ChangingEvidenceGenerator(evidenceCount = 4)
    generator.generateEvidence()
  }
}

object Generate5ChangingEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 4-message changing evidence (3 changes)...")
    val generator = new ChangingEvidenceGenerator(evidenceCount = 5)
    generator.generateEvidence()
  }
}

object Generate6ChangingEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 4-message changing evidence (3 changes)...")
    val generator = new ChangingEvidenceGenerator(evidenceCount = 6)
    generator.generateEvidence()
  }
}

/**
 * Configurable changing evidence generator.
 * Allows custom evidence and change counts via command line arguments.
 * 
 * Usage: 
 * scala GenerateConfigurableChangingEvidence <evidenceCount> <changeCount>
 * 
 * Examples:
 * scala GenerateConfigurableChangingEvidence 2 1  // 2 messages, 1 change
 * scala GenerateConfigurableChangingEvidence 5 4  // 5 messages, 4 changes
 */
object GenerateConfigurableChangingEvidence {
  def main(args: Array[String]): Unit = {
    val (evidenceCount, changeCount) = args.toList match {
      case evidenceStr :: changeStr :: _ =>
        try {
          val evidence = evidenceStr.toInt
          val changes = changeStr.toInt
          if (evidence < 2) {
            throw new IllegalArgumentException("Evidence count must be >= 2 for changing evidence")
          }
          if (changes != evidence - 1) {
            throw new IllegalArgumentException(s"Change count must be evidenceCount - 1. For $evidence evidence, use ${evidence - 1} changes")
          }
          (evidence, changes)
        } catch {
          case _: NumberFormatException =>
            throw new IllegalArgumentException("Evidence count and change count must be integers")
        }
      case _ =>
        println("Usage: GenerateConfigurableChangingEvidence <evidenceCount> <changeCount>")
        println("Example: GenerateConfigurableChangingEvidence 3 2")
        println("Using default: 2 evidence, 1 change")
        (2, 1)
    }
    
    println(s"Generating $evidenceCount-message changing evidence ($changeCount changes)...")
    val generator = new ChangingEvidenceGenerator(evidenceCount)
    generator.generateEvidence()
  }
}