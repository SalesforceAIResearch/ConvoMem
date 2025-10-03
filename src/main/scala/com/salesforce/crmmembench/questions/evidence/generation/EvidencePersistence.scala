package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.Utils
import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.Message
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.io.Source

/**
 * Handles persistence operations for evidence items.
 * Responsible for loading and saving evidence data to/from JSON files.
 */
object EvidencePersistence {

  /**
   * Load evidence items from the specified resource path.
   * 
   * @param resourcePath The resource path containing evidence files
   * @return List of loaded evidence items
   */
  def loadEvidenceItems(resourcePath: String): List[EvidenceItem] = {
    val version = Utils.getCurrentVersion()
    val evidenceDir = new File(s"$resourcePath/$version")
    val allEvidenceItems = scala.collection.mutable.ListBuffer[EvidenceItem]()

    if (!evidenceDir.exists() || !evidenceDir.isDirectory) {
      // Extract evidence type and count from the path for a more helpful error message
      val pathParts = resourcePath.split("/")
      val evidenceInfo = pathParts.takeRight(2).mkString(" ")
      
      throw new RuntimeException(
        s"Evidence directory not found: ${evidenceDir.getAbsolutePath}\n" +
        s"You need to generate the $evidenceInfo evidence first.\n" +
        s"Run the appropriate evidence generator (e.g., GenerateUserFactsEvidence3 for 3-evidence user facts)."
      )
    }

    val jsonFiles = evidenceDir.listFiles().filter(_.getName.endsWith(".json"))
    
    if (jsonFiles.isEmpty) {
      val pathParts = resourcePath.split("/")
      val evidenceInfo = pathParts.takeRight(2).mkString(" ")
      
      throw new RuntimeException(
        s"No evidence files found in: ${evidenceDir.getAbsolutePath}\n" +
        s"The directory exists but contains no JSON files.\n" +
        s"You need to generate the $evidenceInfo evidence first.\n" +
        s"Run the appropriate evidence generator (e.g., GenerateUserFactsEvidence3 for 3-evidence user facts)."
      )
    }
    
    jsonFiles.foreach { jsonFile =>
      val source = Source.fromFile(jsonFile)
      val content = source.mkString
      source.close()

      // Parse as current format
      decode[EvidencePayload](content) match {
        case Right(payload) => 
          // Extract personId from filename if not present in the evidence items
          val fileNameWithoutExtension = jsonFile.getName.stripSuffix(".json")
          val personIdFromFileName = fileNameWithoutExtension.split("_").headOption
          
          // Normalize speaker fields to lowercase and set containsEvidence flag in all loaded evidence items
          val normalizedItems = payload.evidence_items.map { item =>
            // Use personId from the item if present, otherwise extract from filename
            val finalPersonId = item.personId.orElse(personIdFromFileName)
            
            item.copy(
              personId = finalPersonId,
              message_evidences = item.message_evidences.map(m => m.copy(speaker = m.speaker.toLowerCase)),
              conversations = item.conversations.map { conv =>
                conv.copy(
                  messages = conv.messages.map(m => m.copy(speaker = m.speaker.toLowerCase)),
                  containsEvidence = Some(true) // Evidence conversations always contain evidence
                )
              }
            )
          }
          allEvidenceItems ++= normalizedItems
        case Left(error) =>
          throw new RuntimeException(s"Failed to parse ${jsonFile.getName}: $error")
      }
    }

    allEvidenceItems.toList
  }

  /**
   * Save evidence items to file.
   * If file exists, appends new evidence items to existing ones.
   * 
   * @param person The person for whom evidence was generated
   * @param evidenceItems The evidence items to save
   * @param resourcePath The resource path where files should be saved
   */
  def saveEvidenceToFile(person: com.salesforce.crmmembench.Personas.Person, evidenceItems: List[EvidenceItem], resourcePath: String): Unit = {
    val version = Utils.getCurrentVersion()
    val outputDir = s"$resourcePath/$version"
    val fileName = s"${person.id}_${person.getPrimitiveRoleName}.json"
    val outputPath = Paths.get(outputDir, fileName)
    val outputFile = outputPath.toFile

    Files.createDirectories(Paths.get(outputDir))

    // Load existing evidence items if file exists
    val allEvidenceItems = if (outputFile.exists()) {
      val source = Source.fromFile(outputFile)
      val content = source.mkString
      source.close()
      
      decode[EvidencePayload](content) match {
        case Right(existingPayload) =>
          println(s"📝 Appending ${evidenceItems.length} new evidence items to existing ${existingPayload.evidence_items.length} items for ${person.getPrimitiveRoleName}")
          existingPayload.evidence_items ++ evidenceItems
        case Left(error) =>
          println(s"⚠️  Failed to parse existing file, will overwrite: $error")
          evidenceItems
      }
    } else {
      evidenceItems
    }

    val checkpoint = Utils.getGitCheckpoint()
    val payload = EvidencePayload(
      evidence_items = allEvidenceItems,
      checkpoint = Some(checkpoint)
    )
    val jsonString = payload.asJson.spaces2

    val writer = new PrintWriter(outputFile)
    try {
      writer.write(jsonString)
    } finally {
      writer.close()
    }
  }
}