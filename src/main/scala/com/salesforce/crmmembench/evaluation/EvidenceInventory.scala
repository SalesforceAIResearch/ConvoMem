package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.questions.evidence.generation.EvidencePersistence
import com.salesforce.crmmembench.questions.evidence.generators._

/**
 * Simple object that loads all evidence generators with counts up to 6
 * and prints statistics about available evidence items vs expected counts.
 */
object EvidenceInventory {
  
  def main(args: Array[String]): Unit = {
    println("=" * 80)
    println("Evidence Inventory Report")
    println("=" * 80)
    println("Format: actual/expected (percentage)")
    println("=" * 80)
    
    // Define all evidence generator types
    val generatorTypes = List(
      ("UserFactsEvidence", (count: Int) => new UserFactsEvidenceGenerator(count), false),
      ("AssistantFactsEvidence", (count: Int) => new AssistantFactsEvidenceGenerator(count), false),
      ("ChangingEvidence", (count: Int) => new ChangingEvidenceGenerator(count), true),
      ("AbstentionEvidence", (count: Int) => new AbstentionEvidenceGenerator(count), false),
      ("PreferenceEvidence", (count: Int) => new PreferenceEvidenceGenerator(count), false),
      ("ImplicitConnectionEvidence", (count: Int) => new ImplicitConnectionEvidenceGenerator(count), false),
      ("TemporalEvidence", (count: Int) => new TemporalEvidenceGenerator(count), false)
    )
    
    // Track totals for non-LONGMEMEVAL evidences
    var totalActualEvidence = 0
    var totalExpectedEvidence = 0
    
    // For each generator type
    for ((typeName, generatorFactory, isChangingEvidence) <- generatorTypes) {
      println(s"\n$typeName:")
      println("-" * 60)
      
      // Determine the starting count (ChangingEvidence starts at 2)
      val startCount = if (isChangingEvidence) 2 else 1
      
      // For each evidence count
      for (count <- startCount to 6) {
        try {
          val generator = generatorFactory(count)
          val evidenceItems = generator.loadEvidenceItems()
          val itemCount = evidenceItems.size
          val expected = Config.Generation.getExpectedTotal(count, isChangingEvidence)
          val percentage = (itemCount * 100.0 / expected).toInt
          
          // Add to totals
          totalActualEvidence += itemCount
          totalExpectedEvidence += expected
          
          // Format the output with color coding
          val status = if (percentage >= 100) "✓" else if (percentage >= 50) "◐" else "✗"
          
          val itemCountStr = itemCount.toString.padTo(5, ' ')
          val expectedStr = expected.toString.padTo(5, ' ')
          val percentageStr = percentage.toString.padTo(3, ' ')
          println(s"  Evidence count $count: $itemCountStr / $expectedStr (${percentageStr}%) $status")
          
        } catch {
          case _: Exception =>
            // If loading fails, assume 0 items
            val expected = Config.Generation.getExpectedTotal(count, isChangingEvidence)
            val expectedStr = expected.toString.padTo(5, ' ')
            totalExpectedEvidence += expected
            println(s"  Evidence count $count:     0 / $expectedStr (  0%) ✗")
        }
      }
    }
    
    // Print total summary for non-LONGMEMEVAL evidences
    println("\n" + "=" * 80)
    println("TOTAL EVIDENCES (Excluding LONGMEMEVAL)")
    println("=" * 80)
    val totalPercentage = if (totalExpectedEvidence > 0) (totalActualEvidence * 100.0 / totalExpectedEvidence).toInt else 0
    val totalStatus = if (totalPercentage >= 100) "✓" else if (totalPercentage >= 50) "◐" else "✗"
    println(f"Total: ${totalActualEvidence}%,d / ${totalExpectedEvidence}%,d (${totalPercentage}%d%%) $totalStatus")
    
    // Add LongMemEval generators
    println("\n" + "=" * 80)
    println("LONGMEMEVAL IMPORTED DATA")
    println("=" * 80)
    
    // For LongMemEval, we need to check all evidence counts that exist
    val longMemEvalTypes = List(
      ("Multi-Session", "multi_session", 1 to 5),
      ("Preferences", "preferences", 1 to 1),
      ("Assistant Facts", "assistant_facts", 1 to 1),
      ("Knowledge Updates", "knowledge_updates", 2 to 2),
      ("Abstention", "abstention", 1 to 2)  // 3 and 4 consolidated into 2
    )
    
    for ((displayName, dirName, evidenceCounts) <- longMemEvalTypes) {
      println(s"\n  LongMemEval $displayName:")
      var totalForType = 0
      
      for (count <- evidenceCounts) {
        val path = s"src/main/resources/questions/longmemeval/$dirName/${count}_evidence"
        try {
          val items = EvidencePersistence.loadEvidenceItems(path)
          val itemCount = items.size
          totalForType += itemCount
          if (evidenceCounts.size > 1) {
            println(f"    Evidence count $count: ${itemCount}%5d items")
          }
        } catch {
          case _: Exception =>
            // Directory doesn't exist or no items
            if (evidenceCounts.size > 1) {
              println(f"    Evidence count $count:     0 items")
            }
        }
      }
      
      if (evidenceCounts.size == 1) {
        // Single evidence count, show total directly
        println(f"    Total: ${totalForType}%5d items")
      } else {
        // Multiple evidence counts, show grand total
        println(f"    Total across all: ${totalForType}%5d items")
      }
    }
    
    println("\n" + "=" * 80)
    println("Legend: ✓ = 100%+, ◐ = 50-99%, ✗ = <50%")
    println("=" * 80)
  }
}