package com.salesforce.crmmembench.questions.evidence

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.funsuite.AnyFunSuite

class EvidenceItemModelNameTest extends AnyFunSuite {
  
  test("EvidenceItem should serialize and deserialize with model_name fields") {
    val evidenceItem = EvidenceItem(
      question = "What is my favorite tool?",
      answer = "Slack",
      message_evidences = List(Message("User", "I love using Slack for team communication.")),
      conversations = List(
        Conversation(
          messages = List(
            Message("User", "I love using Slack for team communication."),
            Message("Assistant", "That's great! Slack is excellent for team collaboration.")
          ),
          id = Some("conv-1"),
          containsEvidence = Some(true)
        )
      ),
      category = "Professional Tools",
      scenario_description = Some("User discusses their favorite communication tool"),
      personId = Some("person-123"),
      use_case_model_name = Some("gemini-1.5-flash"),
      core_model_name = Some("gpt-4o-mini")
    )
    
    val json = evidenceItem.asJson.noSpaces
    assert(json.contains("\"use_case_model_name\":\"gemini-1.5-flash\""))
    assert(json.contains("\"core_model_name\":\"gpt-4o-mini\""))
    
    val decoded = decode[EvidenceItem](json)
    assert(decoded.isRight)
    assert(decoded.toOption.get == evidenceItem)
  }
  
  test("EvidenceItem should handle missing model_name fields for backwards compatibility") {
    val jsonWithoutModelNames = """{
      "question":"What is my favorite tool?",
      "answer":"Slack",
      "message_evidences":[{"speaker":"User","text":"I love using Slack for team communication."}],
      "conversations":[{
        "messages":[
          {"speaker":"User","text":"I love using Slack for team communication."},
          {"speaker":"Assistant","text":"That's great! Slack is excellent for team collaboration."}
        ],
        "id":"conv-1",
        "containsEvidence":true
      }],
      "category":"Professional Tools",
      "scenario_description":"User discusses their favorite communication tool",
      "personId":"person-123"
    }"""
    
    val decoded = decode[EvidenceItem](jsonWithoutModelNames)
    assert(decoded.isRight)
    
    val evidenceItem = decoded.toOption.get
    assert(evidenceItem.question == "What is my favorite tool?")
    assert(evidenceItem.answer == "Slack")
    assert(evidenceItem.use_case_model_name.isEmpty)
    assert(evidenceItem.core_model_name.isEmpty)
  }
  
  test("EvidenceItem should support partial model_name fields") {
    // Only use_case_model_name present
    val item1 = EvidenceItem(
      question = "Test",
      answer = "Test",
      message_evidences = List(),
      conversations = List(),
      category = "Test",
      scenario_description = None,
      personId = None,
      use_case_model_name = Some("model-1"),
      core_model_name = None
    )
    
    val json1 = item1.asJson.noSpaces
    val decoded1 = decode[EvidenceItem](json1)
    assert(decoded1.isRight)
    assert(decoded1.toOption.get.use_case_model_name == Some("model-1"))
    assert(decoded1.toOption.get.core_model_name.isEmpty)
    
    // Only core_model_name present
    val item2 = item1.copy(
      use_case_model_name = None,
      core_model_name = Some("model-2")
    )
    
    val json2 = item2.asJson.noSpaces
    val decoded2 = decode[EvidenceItem](json2)
    assert(decoded2.isRight)
    assert(decoded2.toOption.get.use_case_model_name.isEmpty)
    assert(decoded2.toOption.get.core_model_name == Some("model-2"))
  }
  
  test("EvidencePayload should work with EvidenceItems containing model_name fields") {
    val payload = EvidencePayload(
      evidence_items = List(
        EvidenceItem(
          question = "Q1",
          answer = "A1",
          message_evidences = List(Message("User", "M1")),
          conversations = List(),
          category = "C1",
          scenario_description = Some("S1"),
          personId = Some("P1"),
          use_case_model_name = Some("gemini-1.5-flash"),
          core_model_name = Some("gpt-4o")
        ),
        EvidenceItem(
          question = "Q2",
          answer = "A2",
          message_evidences = List(Message("User", "M2")),
          conversations = List(),
          category = "C2",
          scenario_description = Some("S2"),
          personId = Some("P2"),
          use_case_model_name = Some("gpt-4o-mini"),
          core_model_name = Some("gemini-1.5-flash")
        )
      )
    )
    
    val json = payload.asJson.noSpaces
    val decoded = decode[EvidencePayload](json)
    
    assert(decoded.isRight)
    val decodedPayload = decoded.toOption.get
    assert(decodedPayload.evidence_items.length == 2)
    assert(decodedPayload.evidence_items(0).use_case_model_name == Some("gemini-1.5-flash"))
    assert(decodedPayload.evidence_items(0).core_model_name == Some("gpt-4o"))
    assert(decodedPayload.evidence_items(1).use_case_model_name == Some("gpt-4o-mini"))
    assert(decodedPayload.evidence_items(1).core_model_name == Some("gemini-1.5-flash"))
  }
}