package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Message, Conversation}

class BatchUtilsTest extends AnyFunSuite {
  
  // Helper to create test evidence item
  def createEvidenceItem(id: String): EvidenceItem = {
    EvidenceItem(
      question = s"Question $id",
      answer = s"Answer $id",
      message_evidences = List(Message("assistant", s"Evidence $id")),
      conversations = List.empty,
      category = "test",
      scenario_description = Some(s"Scenario $id")
    )
  }
  
  // Helper to create test case
  def createTestCase(id: String, contextSize: Int): TestCase = {
    val conversations = (1 to contextSize).map { i =>
      Conversation(
        id = Some(s"conv-$id-$i"),
        messages = List(Message("user", s"Message $i")),
        containsEvidence = Some(false)
      )
    }.toList
    
    TestCase(List(createEvidenceItem(id)), conversations)
  }
  
  test("Empty test cases should return empty batches") {
    val batches = BatchUtils.createBalancedBatches(List.empty, 5)
    assert(batches.size == 5)
    assert(batches.forall(_.isEmpty))
  }
  
  test("Single test case should go to first batch") {
    val testCase = createTestCase("1", 10)
    val batches = BatchUtils.createBalancedBatches(List(testCase), 5)
    
    assert(batches.size == 5)
    assert(batches(0).size == 1)
    assert(batches(0).head == testCase)
    assert(batches.drop(1).forall(_.isEmpty))
  }
  
  test("Test cases should be distributed evenly when divisible") {
    // 30 test cases, 10 batches = 3 per batch
    val testCases = (1 to 30).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    
    assert(batches.size == 10)
    assert(batches.forall(_.size == 3))
    assert(BatchUtils.validateBatches(testCases, batches))
  }
  
  test("Test cases should be distributed with remainder handling") {
    // 14 test cases, 10 batches = 4 batches with 2, 6 batches with 1
    val testCases = (1 to 14).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    
    assert(batches.size == 10)
    val batchSizes = batches.map(_.size).sorted.reverse
    assert(batchSizes.take(4).forall(_ == 2))
    assert(batchSizes.drop(4).forall(_ == 1))
    assert(BatchUtils.validateBatches(testCases, batches))
  }
  
  test("Multiple context sizes should be distributed proportionally") {
    // Create test cases with different context sizes
    val contextSize2 = (1 to 20).map(i => createTestCase(s"c2-$i", 2)).toList
    val contextSize10 = (1 to 10).map(i => createTestCase(s"c10-$i", 10)).toList
    val contextSize50 = (1 to 5).map(i => createTestCase(s"c50-$i", 50)).toList
    
    val allTestCases = contextSize2 ++ contextSize10 ++ contextSize50
    val batches = BatchUtils.createBalancedBatches(allTestCases, 5)
    
    assert(batches.size == 5)
    assert(BatchUtils.validateBatches(allTestCases, batches))
    
    // Each batch should have representation from each context size
    batches.foreach { batch =>
      val contextSizes = batch.map(_.conversationCount).distinct.sorted
      assert(contextSizes == List(2, 10, 50))
    }
    
    // Check distribution is balanced
    val stats = BatchUtils.getBatchStatistics(batches)
    assert(stats.isBalanced)
    assert(stats.contextSizeConsistency.values.forall(_ == true))
  }
  
  test("Batch statistics should accurately reflect distribution") {
    val testCases = (1 to 23).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    val stats = BatchUtils.getBatchStatistics(batches)
    
    assert(stats.numBatches == 10)
    assert(stats.minBatchSize == 2)
    assert(stats.maxBatchSize == 3)
    assert(stats.isBalanced)
  }
  
  test("Context size consistency check") {
    // Create uneven distribution across context sizes
    val context2Cases = (1 to 13).map(i => createTestCase(s"c2-$i", 2)).toList
    val context4Cases = (1 to 7).map(i => createTestCase(s"c4-$i", 4)).toList
    
    val allTestCases = context2Cases ++ context4Cases
    val batches = BatchUtils.createBalancedBatches(allTestCases, 5)
    val stats = BatchUtils.getBatchStatistics(batches)
    
    // Context 2: 13 cases / 5 batches = 2 remainder 3, so 3 batches get 3, 2 batches get 2
    // Context 4: 7 cases / 5 batches = 1 remainder 2, so 2 batches get 2, 3 batches get 1
    val contextConsistency = stats.contextSizeConsistency
    assert(contextConsistency(2) == true) // max 3 - min 2 = 1
    assert(contextConsistency(4) == true) // max 2 - min 1 = 1
  }
  
  test("Large number of context sizes") {
    val testCases = (for {
      contextSize <- List(2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)
      i <- 1 to 10
    } yield createTestCase(s"c${contextSize}-$i", contextSize)).toList
    
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    
    assert(batches.size == 10)
    assert(BatchUtils.validateBatches(testCases, batches))
    
    // Each batch should have exactly 12 test cases (120 total / 10 batches)
    assert(batches.forall(_.size == 12))
    
    // Each batch should have one test case from each context size
    batches.foreach { batch =>
      val contextSizes = batch.map(_.conversationCount).sorted
      assert(contextSizes == List(2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300))
    }
  }
  
  test("validateBatches should detect missing test cases") {
    val original = (1 to 10).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = List(
      original.take(5),
      original.slice(5, 9) // Missing one test case
    )
    
    assert(!BatchUtils.validateBatches(original, batches))
  }
  
  test("validateBatches should detect duplicate test cases") {
    val original = (1 to 10).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = List(
      original.take(6),
      original.slice(5, 10) // Duplicate at index 5
    )
    
    assert(!BatchUtils.validateBatches(original, batches))
  }
  
  test("validateBatches should detect extra test cases") {
    val original = (1 to 10).map(i => createTestCase(s"tc-$i", 10)).toList
    val extra = createTestCase("extra", 10)
    val batches = List(
      original.take(5) :+ extra,
      original.drop(5)
    )
    
    assert(!BatchUtils.validateBatches(original, batches))
  }
  
  test("Edge case: more batches than test cases") {
    val testCases = (1 to 3).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    
    assert(batches.size == 10)
    assert(batches.take(3).forall(_.size == 1))
    assert(batches.drop(3).forall(_.isEmpty))
    assert(BatchUtils.validateBatches(testCases, batches))
  }
  
  test("Edge case: single batch requested") {
    val testCases = (1 to 20).map(i => createTestCase(s"tc-$i", 10)).toList
    val batches = BatchUtils.createBalancedBatches(testCases, 1)
    
    assert(batches.size == 1)
    assert(batches.head.size == 20)
    assert(BatchUtils.validateBatches(testCases, batches))
  }
  
  test("Test cases within each batch should be sorted by conversation count descending") {
    // Create test cases with different context sizes
    val testCases = List(
      createTestCase("small-1", 2),
      createTestCase("small-2", 2),
      createTestCase("medium-1", 10),
      createTestCase("medium-2", 10),
      createTestCase("large-1", 50),
      createTestCase("large-2", 50)
    )
    
    val batches = BatchUtils.createBalancedBatches(testCases, 2)
    
    // Each batch should have test cases sorted by conversation count in descending order
    batches.foreach { batch =>
      val conversationCounts = batch.map(_.conversationCount)
      assert(conversationCounts == conversationCounts.sorted.reverse,
        s"Batch should be sorted by conversation count descending, got: $conversationCounts")
    }
  }
  
  test("Real-world scenario: 437 evidence items, 12 context sizes") {
    // Simulate the scenario mentioned in CLAUDE.md
    val contextSizes = List(2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)
    val testCases = (for {
      evidenceId <- 1 to 437
      contextSize <- contextSizes
    } yield createTestCase(s"e${evidenceId}-c${contextSize}", contextSize)).toList
    
    assert(testCases.size == 437 * 12) // 5244 total
    
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    
    assert(batches.size == 10)
    assert(BatchUtils.validateBatches(testCases, batches))
    
    val stats = BatchUtils.getBatchStatistics(batches)
    
    // Each context size has 437 items to distribute across 10 batches
    // 437 / 10 = 43.7, so 7 batches get 44 items, 3 batches get 43 items
    // With 12 context sizes: 7 batches get 44*12=528, 3 batches get 43*12=516
    assert(stats.minBatchSize == 516)
    assert(stats.maxBatchSize == 528)
    
    // Each context size should appear roughly 437 times per batch (±1)
    stats.contextDistribution.foreach { batchDist =>
      contextSizes.foreach { contextSize =>
        val count = batchDist.getOrElse(contextSize, 0)
        assert(count >= 43 && count <= 44) // 437/10 = 43.7
      }
    }
    
    // Verify each batch is sorted by conversation count descending
    batches.foreach { batch =>
      val conversationCounts = batch.map(_.conversationCount)
      assert(conversationCounts == conversationCounts.sorted.reverse,
        s"Batch should be sorted by conversation count descending")
    }
  }
}