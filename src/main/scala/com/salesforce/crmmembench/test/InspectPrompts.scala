package com.salesforce.crmmembench.test

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators._
import java.io.{File, PrintWriter}

/**
 * Inspect generated prompts to verify our changes
 */
object InspectPrompts {
  def main(args: Array[String]): Unit = {
    // Load a test persona
    val personas = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    val testPerson = personas.head
    
    val outputDir = new File("prompt_inspection")
    outputDir.mkdirs()
    
    // Test all generators
    val generators = List(
      ("UserFacts", new UserFactsEvidenceGenerator(1)),
      ("AssistantFacts", new AssistantFactsEvidenceGenerator(1)),
      ("Changing", new ChangingEvidenceGenerator(2)),
      ("Abstention", new AbstentionEvidenceGenerator(2)),
      ("Preference", new PreferenceEvidenceGenerator(1))
    )
    
    generators.foreach { case (name, generator) =>
      println(s"\nInspecting $name generator...")
      
      // Get prompt parts
      val promptParts = generator.getUseCaseSummaryPromptParts(testPerson)
      
      // Write example use case to file
      val exampleFile = new File(outputDir, s"${name}_example_usecase.txt")
      val pw1 = new PrintWriter(exampleFile)
      pw1.println(s"Example Use Case for $name:")
      pw1.println(s"Category: ${promptParts.exampleUseCase.category}")
      pw1.println(s"Scenario: ${promptParts.exampleUseCase.scenario_description}")
      pw1.close()
      
      // Get full prompt
      val fullPrompt = generator.getUseCaseSummaryPrompt(testPerson)
      
      // Write key sections to file
      val promptFile = new File(outputDir, s"${name}_prompt_sections.txt")
      val pw2 = new PrintWriter(promptFile)
      
      // Extract key sections
      val lines = fullPrompt.split("\n")
      var inStructureSection = false
      var inFieldDefSection = false
      
      lines.foreach { line =>
        if (line.contains("Structure of Each Scenario:")) {
          inStructureSection = true
          pw2.println("\n=== STRUCTURE SECTION ===")
        }
        if (line.contains("Field Definitions")) {
          inFieldDefSection = true
          inStructureSection = false
          pw2.println("\n=== FIELD DEFINITIONS ===")
        }
        if (line.contains("Example (for your reference)")) {
          inFieldDefSection = false
        }
        
        if (inStructureSection || inFieldDefSection) {
          pw2.println(line)
        }
      }
      
      pw2.close()
      
      println(s"✅ Wrote ${exampleFile.getName} and ${promptFile.getName}")
    }
    
    println(s"\n✅ All prompt inspections written to: ${outputDir.getAbsolutePath}")
    println("\nPlease check the files to verify:")
    println("1. Example use cases now include questions")
    println("2. Prompt structure instructs to include questions in scenario_description")
  }
}