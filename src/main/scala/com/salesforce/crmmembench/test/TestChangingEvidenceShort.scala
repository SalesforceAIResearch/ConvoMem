package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.questions.evidence.generators.ChangingEvidenceGenerator
import com.salesforce.crmmembench.{Personas, Config}
import com.salesforce.crmmembench.questions.evidence.generation.{GenerationStats, EvidencePersistence}

/**
 * Short test runner for ChangingEvidenceGenerator to verify the new intermediate evidence check works.
 * Generates evidence for just one person with limited use cases.
 */
object TestChangingEvidenceShort {
  def main(args: Array[String]): Unit = {
    println("\n" + "="*80)
    println("TESTING CHANGING EVIDENCE GENERATOR WITH NEW VERIFICATION")
    println("="*80 + "\n")
    
    // Note: Config values are val, not var, so we can't override them
    // The test will use the default configuration
    
    // Create generator with 3 evidence messages (2 changes)
    val generator = new ChangingEvidenceGenerator(evidenceCount = 3)
    
    // Generate for just the first person with 3 use cases
    val testPerson = Personas.loadPersonas(stage = "enriched_backgrounds").roles.head
    val targetUseCases = 3
    
    println(s"Configuration:")
    println(s"  Person: ${testPerson.getPrimitiveRoleName}")
    println(s"  Evidence count: ${generator.config.evidenceCount}")
    println(s"  Target use cases: $targetUseCases")
    println(s"  Output path: ${generator.config.resourcePath}")
    println()
    
    // Create stats tracker
    val stats = GenerationStats.create(1, targetUseCases, generator.config.evidenceCount)
    
    try {
      // Generate evidence for one person
      println("Starting generation...")
      generator.generateEvidenceForPerson(testPerson, stats, targetUseCases = targetUseCases)
      
      println("\n" + "="*60)
      println("GENERATION RESULTS")
      println("="*60)
      
      // Basic stats
      println(s"\nBasic Statistics:")
      println(s"  Evidence items completed: ${stats.evidenceItemsCompleted.get()}")
      println(s"  Use cases completed: ${stats.useCasesCompleted.get()}")
      println(s"  Evidence cores abandoned: ${stats.abandonedEvidenceCores.get()}")
      
      // Verification stats
      println(s"\nVerification Statistics:")
      val sortedChecks = stats.verificationCheckStats.toList.sortBy(_._1)
      sortedChecks.foreach { case (checkName, (attempts, passes)) =>
        val passRate = if (attempts.get() > 0) (passes.get() * 100.0 / attempts.get()) else 0.0
        println(f"  $checkName%-40s: ${passes.get()}/${attempts.get()} (${passRate}%.1f%%)")
      }
      
      // Check intermediate evidence verification specifically
      val intermediateCheckName = "intermediate_evidence_addresses_question"
      stats.verificationCheckStats.get(intermediateCheckName) match {
        case Some((attempts, passes)) =>
          val passRate = if (attempts.get() > 0) (passes.get() * 100.0 / attempts.get()) else 0.0
          println(s"\n🔍 Intermediate Evidence Check Performance:")
          println(f"   Passed: ${passes.get()}/${attempts.get()} (${passRate}%.1f%%)")
          
          if (passRate == 0.0 && attempts.get() > 0) {
            println("   ❌ WARNING: All intermediate evidence checks failed!")
            println("   This suggests the verification logic may still have issues.")
          } else if (passRate > 0) {
            println("   ✅ SUCCESS: Intermediate evidence verification is working!")
          }
        case None =>
          println("\n⚠️  WARNING: No intermediate evidence checks were performed!")
          println("   This suggests the check may not be running at all.")
      }
      
      // Overall success check
      if (stats.evidenceItemsCompleted.get() == 0) {
        println("\n❌ FAILED: No evidence was generated!")
        System.exit(1)
      } else {
        println(s"\n✅ Generated ${stats.evidenceItemsCompleted.get()} evidence items successfully!")
        
        // Show path to generated files
        val outputPath = s"src/main/resources/${generator.config.resourcePath}"
        println(s"\nGenerated files saved to: $outputPath")
      }
      
    } catch {
      case e: Exception =>
        println(s"\n❌ ERROR during generation: ${e.getMessage}")
        e.printStackTrace()
        System.exit(1)
    }
  }
}