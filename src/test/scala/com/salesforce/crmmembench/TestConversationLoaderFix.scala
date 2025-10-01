package com.salesforce.crmmembench

import com.salesforce.crmmembench.evaluation.ConversationLoader

object TestConversationLoaderFix {
  def main(args: Array[String]): Unit = {
    println("Testing ConversationLoader with personId extraction fix...")
    
    try {
      val conversationsByPerson = ConversationLoader.loadIrrelevantConversations()
      
      println(s"✅ Successfully loaded conversations!")
      println(s"Total persons: ${conversationsByPerson.size}")
      println(s"Total conversations: ${conversationsByPerson.values.map(_.size).sum}")
      
      // Show first few persons
      conversationsByPerson.keys.take(5).foreach { personId =>
        val convCount = conversationsByPerson(personId).size
        println(s"  Person $personId: $convCount conversations")
      }
      
      if (conversationsByPerson.size >= 100) {
        println("✅ SUCCESS: Loaded conversations for 100+ persons as expected!")
      } else if (conversationsByPerson.isEmpty) {
        println("❌ FAILURE: No conversations loaded - personId extraction may have failed")
      } else {
        println(s"⚠️  WARNING: Only ${conversationsByPerson.size} persons loaded (expected 100)")
      }
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}