package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.generators.ImplicitConnectionEvidenceGenerator
import com.salesforce.crmmembench.Personas

/**
 * Quick test to generate a single evidence item for inspection
 */
object TestImplicitSingle {
  def main(args: Array[String]): Unit = {
    println("Generating a single implicit connection evidence item...")
    
    val generator = new ImplicitConnectionEvidenceGenerator(1) {
      override val runShort: Boolean = true
      override lazy val useCasesPerPersonConfig: Int = 1
    }
    
    val person = Personas.loadPersonas(stage = "enriched_backgrounds").roles.head
    val stats = GenerationStats.create(1, 1, 1)
    
    println(s"Using person: ${person.role_name}")
    
    try {
      generator.generateEvidenceForPerson(person, stats)
      
      println("\nGeneration complete!")
      println("Check output at: short_runs/questions/evidence/preference_evidence/1_evidence/default/")
      
      // Read and display the generated file
      val outputDir = new java.io.File("short_runs/questions/evidence/preference_evidence/1_evidence/default/")
      if (outputDir.exists()) {
        val files = outputDir.listFiles()
        if (files.nonEmpty) {
          val content = scala.io.Source.fromFile(files.head).mkString
          println(s"\nGenerated evidence:")
          println(content)
        }
      }
    } catch {
      case e: Exception =>
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}