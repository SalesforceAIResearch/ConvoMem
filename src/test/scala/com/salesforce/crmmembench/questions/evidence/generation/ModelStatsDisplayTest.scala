package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence._
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.funsuite.AnyFunSuite

class ModelStatsDisplayTest extends AnyFunSuite {
  
  test("GenerationStats should display model breakdowns correctly") {
    // Create stats with some sample data
    val stats = GenerationStats.create(totalPeople = 10, totalUseCases = 100, evidenceCount = 1)
    
    // Simulate some completed use cases with different models
    stats.useCasesCompleted.set(50)
    stats.useCaseModelStats.put("gemini-1.5-flash", new AtomicInteger(30))
    stats.useCaseModelStats.put("gpt-4o-mini", new AtomicInteger(20))
    
    // Simulate some completed cores with different models
    stats.evidenceCoresGenerated.set(50)
    stats.coreModelStats.put("gemini-1.5-flash", new AtomicInteger(25))
    stats.coreModelStats.put("gpt-4o-mini", new AtomicInteger(25))
    
    // Simulate some completed conversations with different models
    stats.conversationsGenerated.set(50)
    stats.conversationModelStats.put("gemini-1.5-pro", new AtomicInteger(35))
    stats.conversationModelStats.put("gpt-4o", new AtomicInteger(15))
    
    // Simulate verification stats
    stats.verificationAttempts.set(100)
    stats.verificationPasses.set(10)
    stats.verificationCheckStats.put("with_evidence", (new AtomicInteger(50), new AtomicInteger(8)))
    stats.verificationCheckStats.put("without_evidence", (new AtomicInteger(50), new AtomicInteger(2)))
    
    // Simulate conversation verification stats
    stats.conversationVerificationStats.put("gemini-1.5-pro", (new AtomicInteger(35), new AtomicInteger(30)))
    stats.conversationVerificationStats.put("gpt-4o", (new AtomicInteger(15), new AtomicInteger(10)))
    
    // Other stats
    stats.evidenceItemsCompleted.set(10)
    stats.peopleCompleted.set(5)
    stats.filesGenerated.set(5)
    stats.abandonedEvidenceCores.set(5)
    stats.totalRetryAttempts.set(25)
    
    // Get the stats string
    val statsString = stats.getStatsString
    
    // Print it for visual verification
    println("\n" + "="*80)
    println("MODEL STATS DISPLAY TEST")
    println("="*80)
    println(statsString)
    
    // Verify key elements are present
    assert(statsString.contains("Use Cases"))
    assert(statsString.contains("50 / 100"))
    assert(statsString.contains("gemini-1.5-flash"))
    assert(statsString.contains("30 (60.0%)"))
    assert(statsString.contains("gpt-4o-mini"))
    assert(statsString.contains("20 (40.0%)"))
    
    assert(statsString.contains("Evidence Cores"))
    assert(statsString.contains("Conversations"))
    assert(statsString.contains("gemini-1.5-pro"))
    assert(statsString.contains("35 (70.0%)"))
    
    assert(statsString.contains("Conversations Passed"))
    assert(statsString.contains("40 /  50"))  // Note the extra space
    assert(statsString.contains("30/35"))
    
    println("\n✅ Model stats display test passed!")
  }
}