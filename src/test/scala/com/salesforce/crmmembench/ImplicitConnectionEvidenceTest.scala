package com.salesforce.crmmembench

import com.salesforce.crmmembench.questions.evidence.generators.ImplicitConnectionEvidenceGenerator
import org.scalatest.funsuite.AnyFunSuite

class ImplicitConnectionEvidenceTest extends AnyFunSuite {
  
  test("Implicit connection evidence should have exactly 1 conversation per evidence item") {
    val generator = new ImplicitConnectionEvidenceGenerator(1)
    val evidenceItems = generator.loadEvidenceItems()
    
    println(s"Total evidence items loaded: ${evidenceItems.size}")
    
    // Find evidence items with more than 1 conversation
    val problematicItems = evidenceItems.filter(_.conversations.length > 1)
    
    if (problematicItems.nonEmpty) {
      println(s"\nFound ${problematicItems.size} evidence items with more than 1 conversation:")
      // Only print first 3 problematic items for brevity
      problematicItems.take(3).foreach { item =>
        println(s"\nEvidence Item:")
        println(s"  Person: ${item.personId.getOrElse("Unknown")}")
        println(s"  Question: ${item.question}")
        println(s"  Number of conversations: ${item.conversations.length}")
        println(s"  Answer (rubric): ${item.answer.take(100)}...")
        
        // Print conversation IDs and first few messages
        item.conversations.zipWithIndex.foreach { case (conv, idx) =>
          println(s"\n  Conversation ${idx + 1} (ID: ${conv.id}):")
          println(s"    Messages: ${conv.messages.length}")
          println(s"    Contains evidence: ${conv.containsEvidence}")
          if (conv.messages.nonEmpty) {
            println(s"    First message: ${conv.messages.head.text.take(100)}...")
          }
        }
      }
      
      if (problematicItems.length > 3) {
        println(s"\n... and ${problematicItems.length - 3} more problematic evidence items")
      }
      
      // Also check the distribution
      val distribution = evidenceItems.groupBy(_.conversations.length).mapValues(_.size)
      println(s"\nConversation count distribution:")
      distribution.toSeq.sortBy(_._1).foreach { case (count, num) =>
        println(s"  $count conversation(s): $num evidence items")
      }
    }
    
    // Assert that all evidence items should have exactly 1 conversation
    assert(problematicItems.isEmpty, 
      s"Found ${problematicItems.size} evidence items with more than 1 conversation. " +
      s"Implicit connection evidence should have exactly 1 conversation per evidence item.")
  }
  
  test("Check max evidence count calculation") {
    val generator = new ImplicitConnectionEvidenceGenerator(1)
    val evidenceItems = generator.loadEvidenceItems()
    
    val maxEvidenceCount = if (evidenceItems.nonEmpty) {
      evidenceItems.map(_.conversations.length).max
    } else {
      0
    }
    
    println(s"Max evidence count (conversations per item): $maxEvidenceCount")
    
    assert(maxEvidenceCount == 1, 
      s"Expected max evidence count to be 1, but got $maxEvidenceCount")
  }
}