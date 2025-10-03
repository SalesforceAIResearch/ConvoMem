package com.salesforce.crmmembench.conversations

/**
 * Limited runner for generating a small set of irrelevant conversations for testing.
 * This generates conversations for only a few personas to complete in reasonable time.
 */
object GenerateConversationEvidenceLimited {
  def main(args: Array[String]): Unit = {
    println("="*80)
    println("GENERATING LIMITED IRRELEVANT CONVERSATIONS")
    println("="*80)
    println("\nGenerating conversations for 3 personas with 2 use cases each for testing.\n")
    
    // Create a custom limited generator
    val generator = new ConversationEvidenceGeneratorLimited()
    generator.generateEvidence()
    
    println("\n" + "="*80)
    println("GENERATION COMPLETE")
    println("="*80)
  }
}

/**
 * Limited conversation generator that runs in short mode.
 */
class ConversationEvidenceGeneratorLimited extends ConversationEvidenceGenerator {
  override val runShort: Boolean = true
}