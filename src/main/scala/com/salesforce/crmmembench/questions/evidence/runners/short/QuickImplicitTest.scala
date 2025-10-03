package com.salesforce.crmmembench.questions.evidence.runners.short

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generation.EvidencePersistence

import java.util.UUID

/**
 * Quick test to manually create and save an implicit connection evidence item
 */
object QuickImplicitTest {
  def main(args: Array[String]): Unit = {
    println("Creating sample implicit connection evidence...")
    
    val person = Personas.loadPersonas(stage = "enriched_backgrounds").roles.head
    
    // Create a sample evidence item
    val evidenceItem = EvidenceItem(
      question = "What should we do for the team building event next month?",
      answer = "A good response should: 1) Suggest activities that are accessible and don't require physical exertion, considering the mentioned knee surgery, 2) Focus on inclusive activities like escape rooms, cooking classes, or virtual experiences, 3) Avoid suggesting hiking, sports, or other activities requiring mobility.",
      message_evidences = List(
        Message("User", "By the way, I'll be having knee surgery in two weeks. The doctor says I'll be on crutches for about 6-8 weeks. Anyway, back to the Q3 planning...")
      ),
      conversations = List(
        Conversation(
          id = Some(UUID.randomUUID().toString),
          messages = List(
            Message("User", "I've been looking at our Q3 roadmap and timeline."),
            Message("Assistant", "I'd be happy to discuss the Q3 roadmap with you. What aspects would you like to focus on?"),
            Message("User", "Mainly the marketing campaign launches. By the way, I'll be having knee surgery in two weeks. The doctor says I'll be on crutches for about 6-8 weeks. Anyway, back to the Q3 planning..."),
            Message("Assistant", "I hope your surgery goes well! For the Q3 planning, let's look at the campaign timeline."),
            Message("User", "Thanks. So we have three major campaigns scheduled."),
            Message("Assistant", "Yes, I can help you review those campaigns.")
          ),
          containsEvidence = Some(true)
        )
      ),
      category = "Health Constraints",
      scenario_description = Some("User mentions upcoming knee surgery casually, later asks about team building without referencing it."),
      personId = Some(person.id)
    )
    
    // Save it
    val outputPath = "short_runs/questions/evidence/implicit_connection_evidence/1_evidence"
    EvidencePersistence.saveEvidenceToFile(person, List(evidenceItem), outputPath)
    
    println(s"Saved to: $outputPath/default/")
    
    // Read it back to verify
    val outputDir = new java.io.File(s"$outputPath/default/")
    if (outputDir.exists()) {
      val files = outputDir.listFiles()
      if (files.nonEmpty) {
        println(s"\nCreated file: ${files.head.getName}")
        val content = scala.io.Source.fromFile(files.head).mkString
        println("\nContent preview:")
        println(content.take(500) + "...")
      }
    }
  }
}