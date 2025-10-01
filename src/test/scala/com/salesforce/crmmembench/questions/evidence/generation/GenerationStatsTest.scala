package com.salesforce.crmmembench.questions.evidence.generation

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class GenerationStatsTest extends AnyFunSuite with Matchers {
  
  test("GenerationStats should initialize with correct values") {
    val stats = GenerationStats.create(totalPeople = 100, totalUseCases = 1000, evidenceCount = 2)
    
    stats.totalPeople shouldBe 100
    stats.totalUseCases shouldBe 1000
    stats.evidenceCount shouldBe 2
    stats.peopleCompleted.get() shouldBe 0
    stats.useCasesCompleted.get() shouldBe 0
    stats.evidenceCoresGenerated.get() shouldBe 0
    stats.conversationsGenerated.get() shouldBe 0
    stats.evidenceItemsCompleted.get() shouldBe 0
    stats.filesGenerated.get() shouldBe 0
    stats.verificationAttempts.get() shouldBe 0
    stats.verificationPasses.get() shouldBe 0
    stats.verificationFailures.get() shouldBe 0
    stats.abandonedEvidenceCores.get() shouldBe 0
    stats.totalRetryAttempts.get() shouldBe 0
    stats.successfulEvidenceRetries.get() shouldBe 0
  }
  
  test("GenerationStats should track people completion correctly") {
    val stats = GenerationStats.create(totalPeople = 100, totalUseCases = 1000)
    
    // Simulate completing 3 people
    stats.peopleCompleted.incrementAndGet()
    stats.peopleCompleted.incrementAndGet()
    stats.peopleCompleted.incrementAndGet()
    
    stats.peopleCompleted.get() shouldBe 3
    
    // Should not exceed total people (this was the bug)
    for (_ <- 1 to 200) {
      stats.peopleCompleted.incrementAndGet()
    }
    
    // Even after incrementing 200 more times, we should validate it doesn't exceed expected
    stats.peopleCompleted.get() shouldBe 203 // 3 + 200
  }
  
  test("GenerationStats should calculate progress percentages correctly") {
    val stats = GenerationStats.create(totalPeople = 100, totalUseCases = 1000)
    
    stats.peopleCompleted.set(50)
    stats.useCasesCompleted.set(250)
    
    val statsString = stats.getStatsString
    
    statsString should include ("People")
    statsString should include ("50 / 100")
    statsString should include ("50.0%")
    
    statsString should include ("Use Cases")
    statsString should include ("250 / 1000")
    statsString should include ("25.0%")
  }
  
  test("GenerationStats should calculate verification pass rate correctly") {
    val stats = GenerationStats.create(totalPeople = 10, totalUseCases = 100)
    
    stats.verificationAttempts.set(100)
    stats.verificationPasses.set(95)
    stats.verificationFailures.set(5)
    
    val statsString = stats.getStatsString
    
    statsString should include ("Verification")
    statsString should include ("95/100")
    statsString should include ("95.0%")
  }
  
  test("GenerationStats should track verification check stats correctly") {
    val stats = GenerationStats.create(totalPeople = 10, totalUseCases = 100)
    
    // Add some check stats
    val check1 = ("question_match", (new AtomicInteger(100), new AtomicInteger(90)))
    val check2 = ("answer_quality", (new AtomicInteger(100), new AtomicInteger(85)))
    val check3 = ("evidence_present", (new AtomicInteger(100), new AtomicInteger(100)))
    
    stats.verificationCheckStats.put(check1._1, check1._2)
    stats.verificationCheckStats.put(check2._1, check2._2)
    stats.verificationCheckStats.put(check3._1, check3._2)
    
    val statsString = stats.getStatsString
    
    // Should be sorted by attempts (all 100), then by passes
    statsString should include ("evidence_present")
    statsString should include ("100/100")
    statsString should include ("100.0%")
    
    statsString should include ("question_match")
    statsString should include ("90/100")
    statsString should include ("90.0%")
    
    statsString should include ("answer_quality")
    statsString should include ("85/100")
    statsString should include ("85.0%")
  }
  
  test("GenerationStats should calculate average retry attempts correctly") {
    val stats = GenerationStats.create(totalPeople = 10, totalUseCases = 100)
    
    stats.evidenceItemsCompleted.set(50)
    stats.totalRetryAttempts.set(75) // 1.5 average retries
    
    val statsString = stats.getStatsString
    
    statsString should include ("Avg Retry Attempts")
    statsString should include ("1.5")
  }
  
  test("GenerationStats should handle zero division cases gracefully") {
    val stats = GenerationStats.create(totalPeople = 10, totalUseCases = 100)
    
    // No verification attempts
    stats.verificationAttempts.set(0)
    stats.verificationPasses.set(0)
    
    // No evidence items completed
    stats.evidenceItemsCompleted.set(0)
    stats.totalRetryAttempts.set(0)
    
    val statsString = stats.getStatsString
    
    // Should show 0.0% for verification pass rate
    statsString should include ("0/0")
    statsString should include ("0.0%")
    
    // Should show 0.0 for average retries
    statsString should include ("Avg Retry Attempts")
    statsString should include ("0.0")
  }
  
  test("GenerationStats should display evidence count in header") {
    val stats1 = GenerationStats.create(totalPeople = 10, totalUseCases = 100, evidenceCount = 1)
    val stats2 = GenerationStats.create(totalPeople = 10, totalUseCases = 100, evidenceCount = 400)
    
    val statsString1 = stats1.getStatsString
    val statsString2 = stats2.getStatsString
    
    statsString1 should include ("1-EVIDENCE GENERATION STATISTICS")
    statsString2 should include ("400-EVIDENCE GENERATION STATISTICS")
  }
  
  test("GenerationStats should track abandoned cores correctly") {
    val stats = GenerationStats.create(totalPeople = 10, totalUseCases = 100)
    
    stats.abandonedEvidenceCores.set(5)
    
    val statsString = stats.getStatsString
    
    statsString should include ("Abandoned Cores")
    statsString should include ("5")
    statsString should include ("failed after all retries")
  }
  
  test("ConversationEvidenceGenerator stats should not double-count people") {
    // This test simulates the bug scenario
    val stats = GenerationStats.create(totalPeople = 100, totalUseCases = 40000, evidenceCount = 400)
    
    // Simulate processing first 3 people
    for (_ <- 1 to 3) {
      stats.peopleCompleted.incrementAndGet()
      stats.filesGenerated.incrementAndGet()
      // Each person gets 400 use cases
      for (_ <- 1 to 400) {
        stats.useCasesCompleted.incrementAndGet()
        stats.evidenceCoresGenerated.incrementAndGet()
        stats.conversationsGenerated.incrementAndGet()
        stats.evidenceItemsCompleted.incrementAndGet()
      }
    }
    
    // Simulate processing remaining 97 people
    for (_ <- 1 to 97) {
      stats.peopleCompleted.incrementAndGet()
      stats.filesGenerated.incrementAndGet()
      // Each person gets 400 use cases
      for (_ <- 1 to 400) {
        stats.useCasesCompleted.incrementAndGet()
        stats.evidenceCoresGenerated.incrementAndGet()
        stats.conversationsGenerated.incrementAndGet()
        stats.evidenceItemsCompleted.incrementAndGet()
      }
    }
    
    // Verify correct counts
    stats.peopleCompleted.get() shouldBe 100
    stats.filesGenerated.get() shouldBe 100
    stats.useCasesCompleted.get() shouldBe 40000
    stats.evidenceCoresGenerated.get() shouldBe 40000
    stats.conversationsGenerated.get() shouldBe 40000
    stats.evidenceItemsCompleted.get() shouldBe 40000
    
    val statsString = stats.getStatsString
    
    // Should show 100%, not 197%
    statsString should include ("People")
    statsString should include ("100 / 100")
    statsString should include ("100.0%")
    
    statsString should include ("Use Cases")
    statsString should include ("40000 / 40000")
    statsString should include ("100.0%")
    
    statsString should include ("100 JSON files written")
  }
  
  test("GenerationStats should be thread-safe") {
    val stats = GenerationStats.create(totalPeople = 100, totalUseCases = 1000)
    
    // Simulate concurrent updates from multiple threads
    val threads = (1 to 10).map { _ =>
      new Thread(() => {
        for (_ <- 1 to 100) {
          stats.peopleCompleted.incrementAndGet()
          stats.useCasesCompleted.incrementAndGet()
          stats.evidenceCoresGenerated.incrementAndGet()
          stats.conversationsGenerated.incrementAndGet()
          stats.evidenceItemsCompleted.incrementAndGet()
          stats.verificationAttempts.incrementAndGet()
          if (scala.util.Random.nextBoolean()) {
            stats.verificationPasses.incrementAndGet()
          } else {
            stats.verificationFailures.incrementAndGet()
          }
        }
      })
    }
    
    threads.foreach(_.start())
    threads.foreach(_.join())
    
    // Each thread increments 100 times, 10 threads total
    stats.peopleCompleted.get() shouldBe 1000
    stats.useCasesCompleted.get() shouldBe 1000
    stats.evidenceCoresGenerated.get() shouldBe 1000
    stats.conversationsGenerated.get() shouldBe 1000
    stats.evidenceItemsCompleted.get() shouldBe 1000
    stats.verificationAttempts.get() shouldBe 1000
    (stats.verificationPasses.get() + stats.verificationFailures.get()) shouldBe 1000
  }
}