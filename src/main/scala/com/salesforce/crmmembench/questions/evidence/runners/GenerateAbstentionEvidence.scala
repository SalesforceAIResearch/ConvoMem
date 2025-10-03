package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.AbstentionEvidenceGenerator

/**
 * Entry points for generating abstention evidence conversations.
 * 
 * Abstention evidence tests scenarios where no answer is available in context:
 * - Question: "What's John's phone number?"
 * - Evidence: "John works in marketing", "John joined last month"  
 * - Answer: "There is no information in prior conversations to answer this question"
 * 
 * Supports scalable evidence counts.
 */

/**
 * Generate abstention evidence with 1 message.
 * Pattern: One piece of related but insufficient information
 * Example: "John works in marketing" → Question: "What's John's phone number?"
 */
object Generate1AbstentionEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 1-message abstention evidence...")
    val generator = new AbstentionEvidenceGenerator(evidenceCount = 1)
    {
      override val targetPeopleCount: Int = 100
    }
    generator.generateEvidence()
  }
}

/**
 * Generate abstention evidence with 2 messages.
 * Pattern: Two pieces of related but insufficient information
 * Example: "John works in marketing", "John joined last month" → Question: "What's John's phone number?"
 */
object Generate2AbstentionEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 2-message abstention evidence...")
    val generator = new AbstentionEvidenceGenerator(evidenceCount = 2)
    generator.generateEvidence()
  }
}

/**
 * Generate abstention evidence with 3 messages.
 * Pattern: Three pieces of related but insufficient information
 * Example: "John works in marketing", "John joined last month", "John likes coffee" → Question: "What's John's phone number?"
 */
object Generate3AbstentionEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 3-message abstention evidence...")
    val generator = new AbstentionEvidenceGenerator(evidenceCount = 3)
    generator.generateEvidence()
  }
}

/**
 * Generate abstention evidence with 4 messages.
 * Pattern: Four pieces of related but insufficient information
 */
object Generate4AbstentionEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 4-message abstention evidence...")
    val generator = new AbstentionEvidenceGenerator(evidenceCount = 4)
    generator.generateEvidence()
  }
}

/**
 * Configurable abstention evidence generator.
 * Allows custom evidence counts via command line arguments.
 * 
 * Usage: 
 * scala GenerateConfigurableAbstentionEvidence <evidenceCount>
 * 
 * Examples:
 * scala GenerateConfigurableAbstentionEvidence 1  // 1 related message
 * scala GenerateConfigurableAbstentionEvidence 5  // 5 related messages
 */
object GenerateConfigurableAbstentionEvidence {
  def main(args: Array[String]): Unit = {
    val evidenceCount = args.toList match {
      case evidenceStr :: _ =>
        try {
          val evidence = evidenceStr.toInt
          if (evidence < 1) {
            throw new IllegalArgumentException("Evidence count must be >= 1 for abstention evidence")
          }
          evidence
        } catch {
          case _: NumberFormatException =>
            throw new IllegalArgumentException("Evidence count must be an integer")
        }
      case _ =>
        println("Usage: GenerateConfigurableAbstentionEvidence <evidenceCount>")
        println("Example: GenerateConfigurableAbstentionEvidence 3")
        println("Using default: 1 evidence")
        1
    }
    
    println(s"Generating $evidenceCount-message abstention evidence...")
    val generator = new AbstentionEvidenceGenerator(evidenceCount)
    generator.generateEvidence()
  }
}