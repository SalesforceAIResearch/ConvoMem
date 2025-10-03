package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.questions.evidence.{EvidenceUseCase, GeneratedEvidenceCore, EvidenceItem}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

/**
 * Logs intermediate steps of evidence generation for debugging purposes.
 * Creates JSON files for each phase:
 * - Use cases generated for a person
 * - Evidence cores generated for each use case
 * - Final evidence items
 */
object IntermediateStepsLogger {
  
  /**
   * Directory for intermediate step logs
   */
  val INTERMEDIATE_LOGS_DIR = "logs/evidence_generation_steps"
  
  /**
   * Ensure the log directory exists
   */
  def ensureLogDirectory(generatorType: String): String = {
    val dir = s"$INTERMEDIATE_LOGS_DIR/$generatorType"
    Files.createDirectories(Paths.get(dir))
    dir
  }
  
  
  /**
   * Helper to write JSON to file
   */
  def writeJsonToFile(fileName: String, json: String): Unit = {
    val writer = new PrintWriter(new File(fileName))
    try {
      writer.write(json)
    } finally {
      writer.close()
    }
  }
  
  
  /**
   * Log use cases for a person (Phase 1).
   */
  def logUseCases(
    person: Personas.Person,
    useCases: List[EvidenceUseCase],
    generatorType: String
  ): Unit = {
    val dir = ensureLogDirectory(generatorType)
    val fileName = s"$dir/${person.id}_${person.getPrimitiveRoleName}_1_use_cases.json"
    
    val json = io.circe.Json.obj(
      "person_id" -> person.id.asJson,
      "person_role" -> person.getPrimitiveRoleName.asJson,
      "generator_type" -> generatorType.asJson,
      "timestamp" -> java.time.Instant.now().toString.asJson,
      "total_use_cases" -> useCases.length.asJson,
      "use_cases" -> useCases.asJson
    )
    
    writeJsonToFile(fileName, json.spaces2)
  }
  
  /**
   * Log evidence cores for a person (Phase 2).
   * Maps use case IDs to their generated cores.
   */
  def logEvidenceCores(
    person: Personas.Person,
    cores: Map[Int, GeneratedEvidenceCore],
    generatorType: String
  ): Unit = {
    val dir = ensureLogDirectory(generatorType)
    val fileName = s"$dir/${person.id}_${person.getPrimitiveRoleName}_2_evidence_cores.json"
    
    // Convert map to list of objects with use_case_id
    val coresWithIds = cores.map { case (useCaseId, core) =>
      io.circe.Json.obj(
        "use_case_id" -> useCaseId.asJson,
        "question" -> core.question.asJson,
        "answer" -> core.answer.asJson,
        "message_evidences" -> core.message_evidences.asJson
      )
    }.toList
    
    val json = io.circe.Json.obj(
      "person_id" -> person.id.asJson,
      "person_role" -> person.getPrimitiveRoleName.asJson,
      "generator_type" -> generatorType.asJson,
      "timestamp" -> java.time.Instant.now().toString.asJson,
      "total_cores" -> cores.size.asJson,
      "evidence_cores" -> coresWithIds.asJson
    )
    
    writeJsonToFile(fileName, json.spaces2)
  }
  
  /**
   * Log all intermediate steps for a person.
   * Now creates separate files for use cases and cores.
   */
  def logPersonAllSteps(
    person: Personas.Person,
    steps: IntermediateSteps,
    generatorType: String
  ): Unit = {
    // Log use cases (Phase 1)
    if (steps.useCases.nonEmpty) {
      logUseCases(person, steps.useCases, generatorType)
    }
    
    // Log evidence cores (Phase 2)
    if (steps.evidenceCores.nonEmpty) {
      logEvidenceCores(person, steps.evidenceCores, generatorType)
    }
  }
  
  /**
   * Clean up old logs (optional, for testing)
   */
  def cleanupLogs(generatorType: String): Unit = {
    val dir = new File(s"$INTERMEDIATE_LOGS_DIR/$generatorType")
    if (dir.exists() && dir.isDirectory) {
      dir.listFiles().foreach(_.delete())
    }
  }
}