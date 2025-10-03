package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator
import com.salesforce.crmmembench.Personas
import java.io.File
import java.nio.file.{Files, Paths}
import io.circe.generic.auto._
import io.circe.parser.decode
import scala.io.Source

/**
 * Test that evidence files are appended to instead of replaced.
 */
object TestAppendEvidence {
  
  def main(args: Array[String]): Unit = {
    println("=== Testing Evidence Append Functionality ===\n")
    
    // Create a test output directory
    val testOutputPath = "test_append_evidence"
    val version = "default"
    val outputDir = s"$testOutputPath/$version"
    
    // Clean up any existing test directory
    deleteDirectory(new File(testOutputPath))
    
    // Create test person
    val person = Personas.Person(
      category = "Test Category",
      role_name = "Test User",
      description = "A test user for append functionality",
      background = Some("Test background"),
      id = "test-person-123"
    )
    
    // Create first set of evidence items
    val firstEvidence = List(
      EvidenceItem(
        question = "What's my favorite color?",
        answer = "Blue",
        message_evidences = List(Message("User", "My favorite color is blue")),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "My favorite color is blue"),
              Message("Assistant", "I'll remember that")
            ),
            id = Some("conv1"),
            containsEvidence = Some(true)
          )
        ),
        category = "Personal Preferences",
        scenario_description = Some("User shares favorite color"),
        personId = Some(person.id)
      )
    )
    
    // Save first evidence
    println("1. Saving first evidence item...")
    EvidencePersistence.saveEvidenceToFile(person, firstEvidence, testOutputPath)
    
    // Verify first save
    val file1 = new File(s"$outputDir/${person.id}_Test_User.json")
    if (!file1.exists()) {
      println("❌ ERROR: File was not created!")
      return
    }
    
    val content1 = Source.fromFile(file1).mkString
    decode[EvidencePayload](content1) match {
      case Right(payload) =>
        println(s"✅ First save successful: ${payload.evidence_items.length} item(s)")
        println(s"   Question: ${payload.evidence_items.head.question}")
      case Left(error) =>
        println(s"❌ ERROR parsing first save: $error")
        return
    }
    
    // Create second set of evidence items
    val secondEvidence = List(
      EvidenceItem(
        question = "What's my favorite food?",
        answer = "Pizza",
        message_evidences = List(Message("User", "I love pizza")),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "I love pizza"),
              Message("Assistant", "Pizza is great!")
            ),
            id = Some("conv2"),
            containsEvidence = Some(true)
          )
        ),
        category = "Personal Preferences",
        scenario_description = Some("User shares favorite food"),
        personId = Some(person.id)
      ),
      EvidenceItem(
        question = "What's my favorite number?",
        answer = "7",
        message_evidences = List(Message("User", "My favorite number is 7")),
        conversations = List(
          Conversation(
            messages = List(
              Message("User", "My favorite number is 7"),
              Message("Assistant", "Lucky number 7!")
            ),
            id = Some("conv3"),
            containsEvidence = Some(true)
          )
        ),
        category = "Personal Preferences",
        scenario_description = Some("User shares favorite number"),
        personId = Some(person.id)
      )
    )
    
    // Save second evidence (should append)
    println("\n2. Saving second set of evidence items (should append)...")
    EvidencePersistence.saveEvidenceToFile(person, secondEvidence, testOutputPath)
    
    // Verify append
    val content2 = Source.fromFile(file1).mkString
    decode[EvidencePayload](content2) match {
      case Right(payload) =>
        println(s"✅ Append successful: ${payload.evidence_items.length} total item(s)")
        payload.evidence_items.foreach { item =>
          println(s"   - ${item.question}")
        }
        
        if (payload.evidence_items.length == 3) {
          println("\n✅ SUCCESS: All 3 evidence items are present (1 + 2 = 3)")
        } else {
          println(s"\n❌ ERROR: Expected 3 items, got ${payload.evidence_items.length}")
        }
      case Left(error) =>
        println(s"❌ ERROR parsing appended file: $error")
    }
    
    // Clean up
    println("\n3. Cleaning up test files...")
    deleteDirectory(new File(testOutputPath))
    println("✅ Test complete!")
  }
  
  def deleteDirectory(dir: File): Unit = {
    if (dir.exists()) {
      if (dir.isDirectory) {
        dir.listFiles().foreach(deleteDirectory)
      }
      dir.delete()
    }
  }
}