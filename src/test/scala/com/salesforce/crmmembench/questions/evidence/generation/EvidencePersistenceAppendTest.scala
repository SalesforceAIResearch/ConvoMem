package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import com.salesforce.crmmembench.Personas
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.io.File
import java.nio.file.{Files, Paths}
import io.circe.generic.auto._
import io.circe.parser.decode
import scala.io.Source

class EvidencePersistenceAppendTest extends AnyWordSpec with Matchers with BeforeAndAfterEach {
  
  val testOutputPath = "test_append_evidence_unit"
  
  override def beforeEach(): Unit = {
    deleteDirectory(new File(testOutputPath))
  }
  
  override def afterEach(): Unit = {
    deleteDirectory(new File(testOutputPath))
  }
  
  def deleteDirectory(dir: File): Unit = {
    if (dir.exists()) {
      if (dir.isDirectory) {
        dir.listFiles().foreach(deleteDirectory)
      }
      dir.delete()
    }
  }
  
  "EvidencePersistence.saveEvidenceToFile" should {
    "append evidence items to existing file instead of replacing" in {
      // Create test person
      val person = Personas.Person(
        category = "Test",
        role_name = "Test User",
        description = "Test user",
        background = Some("Test background"),
        id = "test-123"
      )
      
      // Create first evidence item
      val evidence1 = List(
        EvidenceItem(
          question = "Q1",
          answer = "A1",
          message_evidences = List(Message("User", "Evidence 1")),
          conversations = List(
            Conversation(
              messages = List(Message("User", "Conv 1")),
              id = Some("conv1"),
              containsEvidence = Some(true)
            )
          ),
          category = "Test",
          scenario_description = Some("Test 1"),
          personId = Some(person.id)
        )
      )
      
      // Save first evidence
      EvidencePersistence.saveEvidenceToFile(person, evidence1, testOutputPath)
      
      // Create second evidence item
      val evidence2 = List(
        EvidenceItem(
          question = "Q2",
          answer = "A2",
          message_evidences = List(Message("User", "Evidence 2")),
          conversations = List(
            Conversation(
              messages = List(Message("User", "Conv 2")),
              id = Some("conv2"),
              containsEvidence = Some(true)
            )
          ),
          category = "Test",
          scenario_description = Some("Test 2"),
          personId = Some(person.id)
        )
      )
      
      // Save second evidence (should append)
      EvidencePersistence.saveEvidenceToFile(person, evidence2, testOutputPath)
      
      // Read and verify
      val file = new File(s"$testOutputPath/default/${person.id}_Test_User.json")
      val content = Source.fromFile(file).mkString
      val payload = decode[EvidencePayload](content).getOrElse(
        fail("Failed to parse JSON")
      )
      
      // Should have both evidence items
      payload.evidence_items.length shouldBe 2
      payload.evidence_items.map(_.question) should contain allOf ("Q1", "Q2")
    }
    
    "create new file if it doesn't exist" in {
      val person = Personas.Person(
        category = "Test",
        role_name = "New User",
        description = "New user",
        background = None,
        id = "new-123"
      )
      
      val evidence = List(
        EvidenceItem(
          question = "Q",
          answer = "A",
          message_evidences = List(Message("User", "E")),
          conversations = List(
            Conversation(
              messages = List(Message("User", "C")),
              id = Some("c1"),
              containsEvidence = Some(true)
            )
          ),
          category = "Test",
          scenario_description = Some("Test"),
          personId = Some(person.id)
        )
      )
      
      EvidencePersistence.saveEvidenceToFile(person, evidence, testOutputPath)
      
      val file = new File(s"$testOutputPath/default/${person.id}_New_User.json")
      file.exists() shouldBe true
      
      val content = Source.fromFile(file).mkString
      val payload = decode[EvidencePayload](content).getOrElse(
        fail("Failed to parse JSON")
      )
      
      payload.evidence_items.length shouldBe 1
    }
  }
}