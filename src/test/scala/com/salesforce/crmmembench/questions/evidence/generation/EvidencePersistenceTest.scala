package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

class EvidencePersistenceTest extends AnyFunSuite with Matchers {
  
  test("loadEvidenceItems should extract personId from filename when not present in JSON") {
    // Create a temporary directory for test files
    val tempDir = Files.createTempDirectory("evidence_test")
    val version = "default"
    val versionDir = tempDir.resolve(version)
    Files.createDirectory(versionDir)
    
    try {
      // Create test evidence items without personId field
      val evidenceItemsWithoutPersonId = List(
        EvidenceItem(
          question = "What's my name?",
          answer = "Your name is Alice.",
          message_evidences = List(Message("user", "My name is Alice")),
          conversations = List(
            Conversation(
              messages = List(
                Message("user", "My name is Alice"),
                Message("assistant", "Nice to meet you, Alice!")
              ),
              id = Some("conv-1"),
              containsEvidence = Some(true)
            )
          ),
          category = "Personal",
          scenario_description = Some("User introduces themselves"),
          personId = None // No personId in the data
        )
      )
      
      // Save with filename pattern: {personId}_{roleName}.json
      val personId = "abc123def"
      val roleName = "Sales_Manager"
      val filename = s"${personId}_${roleName}.json"
      val filePath = versionDir.resolve(filename)
      
      val payload = EvidencePayload(evidence_items = evidenceItemsWithoutPersonId)
      val writer = new PrintWriter(filePath.toFile)
      try {
        writer.write(payload.asJson.spaces2)
      } finally {
        writer.close()
      }
      
      // Load the evidence items
      val loadedItems = EvidencePersistence.loadEvidenceItems(tempDir.toString)
      
      // Verify that personId was extracted from the filename
      loadedItems should have size 1
      val loadedItem = loadedItems.head
      loadedItem.personId shouldBe Some(personId)
      
      // Other fields should remain unchanged
      loadedItem.question shouldBe "What's my name?"
      loadedItem.answer shouldBe "Your name is Alice."
      loadedItem.category shouldBe "Personal"
    } finally {
      // Clean up temp directory
      def deleteRecursively(file: File): Unit = {
        if (file.isDirectory) {
          file.listFiles().foreach(deleteRecursively)
        }
        file.delete()
      }
      deleteRecursively(tempDir.toFile)
    }
  }
  
  test("loadEvidenceItems should preserve existing personId if present") {
    // Create a temporary directory for test files
    val tempDir = Files.createTempDirectory("evidence_test")
    val version = "default"
    val versionDir = tempDir.resolve(version)
    Files.createDirectory(versionDir)
    
    try {
      // Create test evidence items WITH personId field
      val existingPersonId = "existing123"
      val evidenceItemsWithPersonId = List(
        EvidenceItem(
          question = "What's my job?",
          answer = "You work as an engineer.",
          message_evidences = List(Message("user", "I work as an engineer")),
          conversations = List(
            Conversation(
              messages = List(
                Message("user", "I work as an engineer"),
                Message("assistant", "That's great!")
              ),
              id = Some("conv-2"),
              containsEvidence = Some(true)
            )
          ),
          category = "Professional",
          scenario_description = Some("User shares job info"),
          personId = Some(existingPersonId) // Has personId in the data
        )
      )
      
      // Save with different personId in filename
      val filenamePersonId = "different456"
      val roleName = "Engineer"
      val filename = s"${filenamePersonId}_${roleName}.json"
      val filePath = versionDir.resolve(filename)
      
      val payload = EvidencePayload(evidence_items = evidenceItemsWithPersonId)
      val writer = new PrintWriter(filePath.toFile)
      try {
        writer.write(payload.asJson.spaces2)
      } finally {
        writer.close()
      }
      
      // Load the evidence items
      val loadedItems = EvidencePersistence.loadEvidenceItems(tempDir.toString)
      
      // Verify that existing personId in JSON is preserved (not overwritten by filename)
      loadedItems should have size 1
      val loadedItem = loadedItems.head
      loadedItem.personId shouldBe Some(existingPersonId) // Should keep the JSON value, not filename
      
      // Other fields should remain unchanged
      loadedItem.question shouldBe "What's my job?"
      loadedItem.answer shouldBe "You work as an engineer."
      loadedItem.category shouldBe "Professional"
    } finally {
      // Clean up temp directory
      def deleteRecursively(file: File): Unit = {
        if (file.isDirectory) {
          file.listFiles().foreach(deleteRecursively)
        }
        file.delete()
      }
      deleteRecursively(tempDir.toFile)
    }
  }
  
  test("loadEvidenceItems should handle files without underscore in name") {
    // Create a temporary directory for test files
    val tempDir = Files.createTempDirectory("evidence_test")
    val version = "default"
    val versionDir = tempDir.resolve(version)
    Files.createDirectory(versionDir)
    
    try {
      // Create test evidence items without personId field
      val evidenceItems = List(
        EvidenceItem(
          question = "Test question",
          answer = "Test answer",
          message_evidences = List(Message("user", "Test")),
          conversations = List(
            Conversation(
              messages = List(Message("user", "Test")),
              id = Some("conv-3"),
              containsEvidence = Some(true)
            )
          ),
          category = "Test",
          scenario_description = Some("Test scenario"),
          personId = None
        )
      )
      
      // Save with filename without underscore (edge case)
      val filename = "singlename.json"
      val filePath = versionDir.resolve(filename)
      
      val payload = EvidencePayload(evidence_items = evidenceItems)
      val writer = new PrintWriter(filePath.toFile)
      try {
        writer.write(payload.asJson.spaces2)
      } finally {
        writer.close()
      }
      
      // Load the evidence items
      val loadedItems = EvidencePersistence.loadEvidenceItems(tempDir.toString)
      
      // Verify that personId is extracted as the whole filename (before .json)
      loadedItems should have size 1
      val loadedItem = loadedItems.head
      loadedItem.personId shouldBe Some("singlename")
    } finally {
      // Clean up temp directory
      def deleteRecursively(file: File): Unit = {
        if (file.isDirectory) {
          file.listFiles().foreach(deleteRecursively)
        }
        file.delete()
      }
      deleteRecursively(tempDir.toFile)
    }
  }
}