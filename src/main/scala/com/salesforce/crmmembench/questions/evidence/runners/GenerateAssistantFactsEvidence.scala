package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.AssistantFactsEvidenceGenerator

/**
 * Entry points for generating assistant facts evidence conversations.
 * 
 * Assistant facts tests scenarios where users need to recall what the assistant previously said:
 * - Assistant statement: "I would recommend Roscioli restaurant"
 * - Question: "What restaurant did you recommend?"
 * - Answer: "Roscioli"
 * 
 * Supports scalable evidence counts for multiple assistant statements.
 */

/**
 * Generate assistant facts evidence with 1 statement.
 * Pattern: Single assistant recommendation/statement to recall
 * Example: Assistant recommends a book → User asks what book was recommended
 */
object Generate1AssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 1-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount = 1) {
      override val targetPeopleCount: Int = 90
    }
    generator.generateEvidence()
  }
}

/**
 * Generate assistant facts evidence with 2 statements.
 * Pattern: Two assistant recommendations/statements to recall
 * Example: Assistant recommends tool A and tool B → User asks what tools were recommended
 */
object Generate2AssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 2-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount = 2)
    generator.generateEvidence()
  }
}

/**
 * Generate assistant facts evidence with 3 statements.
 * Pattern: Three assistant recommendations/statements to recall
 * Example: Assistant gives three pieces of advice → User asks to recall the advice
 */
object Generate3AssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 3-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount = 3)
    generator.generateEvidence()
  }
}

/**
 * Generate assistant facts evidence with 4 statements.
 * Pattern: Four assistant recommendations/statements to recall
 */
object Generate4AssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 4-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount = 4)
    generator.generateEvidence()
  }
}

/**
 * Generate assistant facts evidence with 5 statements.
 * Pattern: Five assistant recommendations/statements to recall
 */
object Generate5AssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 5-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount = 5)
    generator.generateEvidence()
  }
}

/**
 * Generate assistant facts evidence with 6 statements.
 * Pattern: Six assistant recommendations/statements to recall
 */
object Generate6AssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    println("Generating 6-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount = 6)
    generator.generateEvidence()
  }
}

/**
 * Configurable assistant facts evidence generator.
 * Allows custom evidence counts via command line arguments.
 * 
 * Usage: 
 * scala GenerateConfigurableAssistantFactsEvidence <evidenceCount>
 * 
 * Examples:
 * scala GenerateConfigurableAssistantFactsEvidence 1  // 1 assistant statement
 * scala GenerateConfigurableAssistantFactsEvidence 4  // 4 assistant statements
 */
object GenerateConfigurableAssistantFactsEvidence {
  def main(args: Array[String]): Unit = {
    val evidenceCount = args.toList match {
      case evidenceStr :: _ =>
        try {
          val evidence = evidenceStr.toInt
          if (evidence < 1) {
            throw new IllegalArgumentException("Evidence count must be >= 1 for assistant facts evidence")
          }
          evidence
        } catch {
          case _: NumberFormatException =>
            throw new IllegalArgumentException("Evidence count must be an integer")
        }
      case _ =>
        println("Usage: GenerateConfigurableAssistantFactsEvidence <evidenceCount>")
        println("Example: GenerateConfigurableAssistantFactsEvidence 2")
        println("Using default: 1 evidence")
        1
    }
    
    println(s"Generating $evidenceCount-statement assistant facts evidence...")
    val generator = new AssistantFactsEvidenceGenerator(evidenceCount)
    generator.generateEvidence()
  }
}