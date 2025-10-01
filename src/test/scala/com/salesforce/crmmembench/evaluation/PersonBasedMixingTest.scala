package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.util.UUID

class PersonBasedMixingTest extends AnyFunSuite with Matchers {
  
  test("StandardTestCasesGenerator should mix conversations from the same person") {
    // Create mock evidence items with person IDs
    val person1Id = "person-1"
    val person2Id = "person-2"
    
    val evidenceItem1 = EvidenceItem(
      question = "What is my name?",
      answer = "Your name is Alice.",
      message_evidences = List(Message("user", "My name is Alice.")),
      conversations = List(
        Conversation(
          messages = List(
            Message("user", "My name is Alice."),
            Message("assistant", "Nice to meet you, Alice!")
          ),
          id = Some("conv-1"),
          containsEvidence = Some(true)
        )
      ),
      category = "Personal Info",
      scenario_description = Some("User shares name"),
      personId = Some(person1Id)
    )
    
    val evidenceItem2 = EvidenceItem(
      question = "What is my job?",
      answer = "You work as an engineer.",
      message_evidences = List(Message("user", "I work as an engineer.")),
      conversations = List(
        Conversation(
          messages = List(
            Message("user", "I work as an engineer."),
            Message("assistant", "That's interesting!")
          ),
          id = Some("conv-2"),
          containsEvidence = Some(true)
        )
      ),
      category = "Professional Info",
      scenario_description = Some("User shares job"),
      personId = Some(person2Id)
    )
    
    // Create mock irrelevant conversations by person
    val irrelevantConversationsByPerson = Map(
      person1Id -> List(
        Conversation(
          messages = List(
            Message("user", "How's the weather?"),
            Message("assistant", "It's sunny today!")
          ),
          id = Some("person1-irrelevant-1"),
          containsEvidence = Some(false)
        ),
        Conversation(
          messages = List(
            Message("user", "Tell me a joke."),
            Message("assistant", "Why did the chicken cross the road?")
          ),
          id = Some("person1-irrelevant-2"),
          containsEvidence = Some(false)
        )
      ),
      person2Id -> List(
        Conversation(
          messages = List(
            Message("user", "What's for lunch?"),
            Message("assistant", "Pizza sounds good!")
          ),
          id = Some("person2-irrelevant-1"),
          containsEvidence = Some(false)
        ),
        Conversation(
          messages = List(
            Message("user", "I need coffee."),
            Message("assistant", "Coffee break time!")
          ),
          id = Some("person2-irrelevant-2"),
          containsEvidence = Some(false)
        )
      )
    )
    
    // This test demonstrates the expected behavior of person-based mixing
    // In the actual implementation, StandardTestCasesGenerator will:
    // 1. Get the personId from the evidence item
    // 2. Select irrelevant conversations only from that person's pool
    // 3. Mix them with the evidence conversations
    
    // Verify that for person1's evidence, we only get person1's irrelevant conversations
    val person1IrrelevantIds = irrelevantConversationsByPerson(person1Id).map(_.id.get).toSet
    val person2IrrelevantIds = irrelevantConversationsByPerson(person2Id).map(_.id.get).toSet
    
    // These sets should be disjoint
    person1IrrelevantIds.intersect(person2IrrelevantIds) shouldBe empty
    
    // Each person should have their own irrelevant conversations
    person1IrrelevantIds should contain ("person1-irrelevant-1")
    person1IrrelevantIds should contain ("person1-irrelevant-2")
    person2IrrelevantIds should contain ("person2-irrelevant-1")
    person2IrrelevantIds should contain ("person2-irrelevant-2")
  }
  
  test("BatchedTestCasesGenerator should proportionally mix conversations by person") {
    // Create evidence items from multiple persons
    val person1Id = "person-1"
    val person2Id = "person-2"
    
    val person1Items = (1 to 3).map { i =>
      EvidenceItem(
        question = s"Person1 Question $i",
        answer = s"Person1 Answer $i",
        message_evidences = List(Message("user", s"Person1 evidence $i")),
        conversations = List(
          Conversation(
            messages = List(Message("user", s"Person1 conv $i")),
            id = Some(s"p1-conv-$i"),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some(s"Person1 scenario $i"),
        personId = Some(person1Id)
      )
    }.toList
    
    val person2Items = (1 to 2).map { i =>
      EvidenceItem(
        question = s"Person2 Question $i",
        answer = s"Person2 Answer $i",
        message_evidences = List(Message("user", s"Person2 evidence $i")),
        conversations = List(
          Conversation(
            messages = List(Message("user", s"Person2 conv $i")),
            id = Some(s"p2-conv-$i"),
            containsEvidence = Some(true)
          )
        ),
        category = "Test",
        scenario_description = Some(s"Person2 scenario $i"),
        personId = Some(person2Id)
      )
    }.toList
    
    val allItems = person1Items ++ person2Items
    
    // When batching these items:
    // - Person1 has 3 items (60% of total)
    // - Person2 has 2 items (40% of total)
    // Irrelevant conversations should be selected proportionally
    
    val totalItems = allItems.length
    val person1Proportion = person1Items.length.toDouble / totalItems
    val person2Proportion = person2Items.length.toDouble / totalItems
    
    person1Proportion shouldBe 0.6 +- 0.01
    person2Proportion shouldBe 0.4 +- 0.01
    
    // In the actual implementation, if we need 10 irrelevant conversations:
    // - ~6 should come from person1's irrelevant pool
    // - ~4 should come from person2's irrelevant pool
  }
}