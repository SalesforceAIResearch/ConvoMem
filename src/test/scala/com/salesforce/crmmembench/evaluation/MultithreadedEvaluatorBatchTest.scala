package com.salesforce.crmmembench.evaluation

import org.scalatest.funsuite.AnyFunSuite
import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Message, Conversation}

class MultithreadedEvaluatorBatchTest extends AnyFunSuite {
  
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
  
  test("Batch processing should distribute test cases evenly across context sizes") {
    // Create test cases with different context sizes
    val testCases = for {
      contextSize <- List(2, 4, 6)
      i <- 1 to 30
    } yield createTestCase(s"tc-${contextSize}-$i", contextSize)
    
    // Create batches
    val batches = BatchUtils.createBalancedBatches(testCases.toList, 10)
    
    // Verify each batch has representation from all context sizes
    batches.foreach { batch =>
      val contextSizes = batch.map(_.conversationCount).distinct.sorted
      assert(contextSizes == List(2, 4, 6), s"Batch should have all context sizes, got: $contextSizes")
    }
    
    // Verify batch sizes are balanced
    val stats = BatchUtils.getBatchStatistics(batches)
    assert(stats.maxBatchSize - stats.minBatchSize <= 1)
  }
  
  test("Batch distribution with real-world scenario") {
    // Simulate 437 evidence items with 12 context sizes
    val contextSizes = List(2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)
    val testCases = (for {
      evidenceId <- 1 to 437
      contextSize <- contextSizes
    } yield createTestCase(s"e${evidenceId}-c${contextSize}", contextSize)).toList
    
    val batches = BatchUtils.createBalancedBatches(testCases, 10)
    
    // Verify total preservation
    assert(batches.flatten.size == testCases.size)
    
    // Verify each batch has balanced representation
    val stats = BatchUtils.getBatchStatistics(batches)
    
    // Each context size should appear in each batch
    stats.contextDistribution.foreach { batchDist =>
      assert(batchDist.size == contextSizes.size, "Each batch should have all context sizes")
    }
    
    // Verify consistency
    val consistency = stats.contextSizeConsistency
    consistency.values.foreach { isConsistent =>
      assert(isConsistent, "Context size distribution should be consistent across batches")
    }
  }
}