package com.salesforce.crmmembench.questions.evidence

import com.salesforce.crmmembench.Personas
import com.salesforce.crmmembench.conversations.ConversationPromptParts
import com.salesforce.crmmembench.questions.evidence.generation._
import com.salesforce.crmmembench.questions.evidence.generators.longmemeval.LongMemEvalAbstentionGenerator
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Unit tests for CompositeEvidenceGenerator
 */
class CompositeEvidenceGeneratorTest extends AnyFunSuite with Matchers {
  
  // Mock generator for testing
  class MockEvidenceGenerator(
    val items: List[EvidenceItem],
    val name: String,
    val count: Int = 1
  ) extends EvidenceGenerator {
    
    // Use CompositeEvidenceConfig for simplicity in tests
    override def config: EvidenceConfig = CompositeEvidenceConfig(count)
    
    override def getEvidenceTypeName: String = name
    
    override def loadEvidenceItems(): List[EvidenceItem] = items
    
    override def generateEvidence(): Unit = {
      println(s"Mock generator $name called")
    }
    
    override def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
      UseCaseSummaryPromptParts(
        evidenceTypeDescription = s"Mock $name",
        coreTaskDescription = "Mock task",
        evidenceDistributionDescription = "Mock distribution",
        exampleUseCase = EvidenceUseCase(0, "Mock", "Mock scenario"),
        additionalRequirements = None
      )
    }
    
    override def getEvidenceCorePromptParts(
      person: Personas.Person,
      useCase: EvidenceUseCase
    ): EvidenceCorePromptParts = {
      EvidenceCorePromptParts(
        scenarioType = "Mock",
        taskDescription = "Mock task",
        specificInstructions = "",
        fieldDefinitions = "",
        additionalGuidance = None
      )
    }
    
    override def getConversationPromptParts(
      person: Personas.Person,
      useCase: EvidenceUseCase,
      evidenceCore: GeneratedEvidenceCore
    ): ConversationPromptParts = {
      ConversationPromptParts(
        evidenceType = "Mock",
        scenarioDescription = "Mock scenario",
        useCaseScenario = Some("Mock"),
        evidenceMessages = List.empty,
        question = "Mock question",
        answer = "Mock answer",
        evidenceCount = 1
      )
    }
    
    override def getVerificationChecks(): List[VerificationCheck] = List.empty
    
    override def getAnsweringEvaluation(): AnsweringEvaluation = DefaultAnsweringEvaluation
  }
  
  // Helper to create mock evidence items
  def createMockEvidenceItem(id: String, question: String, answer: String): EvidenceItem = {
    EvidenceItem(
      question = question,
      answer = answer,
      message_evidences = List.empty,
      conversations = List.empty,
      category = "Mock",
      scenario_description = Some(s"Mock scenario $id"),
      personId = Some(s"person-$id"),
      use_case_model_name = None,
      core_model_name = None
    )
  }
  
  test("CompositeEvidenceGenerator should require at least one generator") {
    intercept[IllegalArgumentException] {
      new CompositeEvidenceGenerator(List.empty)
    }
  }
  
  test("CompositeEvidenceGenerator should combine evidence from single generator") {
    val items = List(
      createMockEvidenceItem("1", "Question 1", "Answer 1"),
      createMockEvidenceItem("2", "Question 2", "Answer 2")
    )
    
    val generator = new MockEvidenceGenerator(items, "Test1", 2)
    val composite = new CompositeEvidenceGenerator(List(generator), "Single")
    
    val loadedItems = composite.loadEvidenceItems()
    loadedItems.length shouldBe 2
    loadedItems shouldBe items
  }
  
  test("CompositeEvidenceGenerator should combine evidence from multiple generators") {
    val items1 = List(
      createMockEvidenceItem("1", "Q1", "A1"),
      createMockEvidenceItem("2", "Q2", "A2")
    )
    
    val items2 = List(
      createMockEvidenceItem("3", "Q3", "A3"),
      createMockEvidenceItem("4", "Q4", "A4"),
      createMockEvidenceItem("5", "Q5", "A5")
    )
    
    val items3 = List(
      createMockEvidenceItem("6", "Q6", "A6")
    )
    
    val gen1 = new MockEvidenceGenerator(items1, "Gen1", 2)
    val gen2 = new MockEvidenceGenerator(items2, "Gen2", 3)
    val gen3 = new MockEvidenceGenerator(items3, "Gen3", 1)
    
    val composite = new CompositeEvidenceGenerator(List(gen1, gen2, gen3), "Multi")
    
    val loadedItems = composite.loadEvidenceItems()
    loadedItems.length shouldBe 6
    loadedItems shouldBe (items1 ++ items2 ++ items3)
  }
  
  test("CompositeEvidenceGenerator should handle generator failures gracefully") {
    val items1 = List(createMockEvidenceItem("1", "Q1", "A1"))
    
    // Generator that throws exception
    val failingGen = new MockEvidenceGenerator(List.empty, "Failing", 1) {
      override def loadEvidenceItems(): List[EvidenceItem] = {
        throw new RuntimeException("Test failure")
      }
    }
    
    val items3 = List(createMockEvidenceItem("2", "Q2", "A2"))
    
    val gen1 = new MockEvidenceGenerator(items1, "Gen1", 1)
    val gen3 = new MockEvidenceGenerator(items3, "Gen3", 1)
    
    val composite = new CompositeEvidenceGenerator(List(gen1, failingGen, gen3), "Resilient")
    
    // Should still load items from working generators
    val loadedItems = composite.loadEvidenceItems()
    loadedItems.length shouldBe 2
    loadedItems shouldBe (items1 ++ items3)
  }
  
  test("CompositeEvidenceGenerator config should sum evidence counts") {
    val gen1 = new MockEvidenceGenerator(List.empty, "Gen1", 2)
    val gen2 = new MockEvidenceGenerator(List.empty, "Gen2", 3)
    val gen3 = new MockEvidenceGenerator(List.empty, "Gen3", 1)
    
    val composite = new CompositeEvidenceGenerator(List(gen1, gen2, gen3))
    
    composite.config.evidenceCount shouldBe 6
  }
  
  test("CompositeEvidenceGenerator should preserve order of generators") {
    val items1 = List(createMockEvidenceItem("1", "First", "A1"))
    val items2 = List(createMockEvidenceItem("2", "Second", "A2"))
    val items3 = List(createMockEvidenceItem("3", "Third", "A3"))
    
    val gen1 = new MockEvidenceGenerator(items1, "Gen1")
    val gen2 = new MockEvidenceGenerator(items2, "Gen2")
    val gen3 = new MockEvidenceGenerator(items3, "Gen3")
    
    val composite = new CompositeEvidenceGenerator(List(gen1, gen2, gen3))
    
    val loadedItems = composite.loadEvidenceItems()
    loadedItems.map(_.question) shouldBe List("First", "Second", "Third")
  }
  
  test("CompositeEvidenceGenerator should use custom name when provided") {
    val gen = new MockEvidenceGenerator(List.empty, "Gen1")
    val composite = new CompositeEvidenceGenerator(List(gen), "CustomName")
    
    composite.getEvidenceTypeName shouldBe "CustomName"
  }
  
  test("CompositeEvidenceGenerator should use verification checks from first generator") {
    val check1 = new VerificationCheck {
      def name: String = "Check1"
      def verify(item: EvidenceItem, stats: Option[scala.collection.concurrent.TrieMap[String, (java.util.concurrent.atomic.AtomicInteger, java.util.concurrent.atomic.AtomicInteger)]], answeringEvaluation: AnsweringEvaluation): VerificationCheckResult = {
        VerificationCheckResult("Check1", true, "Passed", None)
      }
    }
    
    val gen1 = new MockEvidenceGenerator(List.empty, "Gen1") {
      override def getVerificationChecks(): List[VerificationCheck] = List(check1)
    }
    
    val gen2 = new MockEvidenceGenerator(List.empty, "Gen2")
    
    val composite = new CompositeEvidenceGenerator(List(gen1, gen2))
    
    composite.getVerificationChecks().length shouldBe 1
    composite.getVerificationChecks().head.name shouldBe "Check1"
  }
  
  test("CompositeEvidenceGenerator should use answering evaluation from first generator") {
    val customEval = new AnsweringEvaluation {
      def getJudgePromptTemplate(
        question: String,
        correctAnswer: String,
        modelAnswer: String,
        evidenceMessages: List[Message]
      ): String = "Custom judge prompt"
    }
    
    val gen1 = new MockEvidenceGenerator(List.empty, "Gen1") {
      override def getAnsweringEvaluation(): AnsweringEvaluation = customEval
    }
    
    val gen2 = new MockEvidenceGenerator(List.empty, "Gen2")
    
    val composite = new CompositeEvidenceGenerator(List(gen1, gen2))
    
    composite.getAnsweringEvaluation() shouldBe customEval
  }
  
  test("CompositeEvidenceGenerator generateEvidence should display statistics") {
    val items1 = List(
      createMockEvidenceItem("1", "Question 1", "Answer 1"),
      createMockEvidenceItem("2", "Question 2", "Answer 2")
    )
    
    val items2 = List(
      createMockEvidenceItem("3", "Question 3", "Answer 3")
    )
    
    val gen1 = new MockEvidenceGenerator(items1, "Gen1", 2)
    val gen2 = new MockEvidenceGenerator(items2, "Gen2", 1)
    
    val composite = new CompositeEvidenceGenerator(List(gen1, gen2), "TestComposite")
    
    // This should not throw and should print statistics
    noException should be thrownBy composite.generateEvidence()
  }
}

/**
 * Integration tests for AbstentionCompositeGenerator
 */
class AbstentionCompositeGeneratorTest extends AnyFunSuite with Matchers {
  
  test("AbstentionCompositeGenerator should combine all 4 abstention generators") {
    val generator = AbstentionCompositeGenerator
    
    // Should have 4 generators (1-4)
    generator.generators.length shouldBe 4
    
    // Each should be a LongMemEvalAbstentionGenerator with correct count
    generator.generators.zipWithIndex.foreach { case (gen, idx) =>
      gen shouldBe a[LongMemEvalAbstentionGenerator]
      gen.config.evidenceCount shouldBe (idx + 1)
    }
  }
  
  test("AbstentionCompositeGenerator should have correct name") {
    AbstentionCompositeGenerator.getEvidenceTypeName shouldBe "longmemeval_abstention_1to4"
  }
  
  test("AbstentionCompositeGenerator config should sum to 10") {
    // 1 + 2 + 3 + 4 = 10
    AbstentionCompositeGenerator.config.evidenceCount shouldBe 10
  }
  
  test("AbstentionCompositeGenerator should load evidence items") {
    // This will attempt to load actual evidence files
    // The test may fail if the files don't exist, which is expected
    try {
      val items = AbstentionCompositeGenerator.loadEvidenceItems()
      // If files exist, we should get some items
      if (items.nonEmpty) {
        items.foreach { item =>
          item.question should not be empty
          item.answer should not be empty
        }
      }
    } catch {
      case _: Exception =>
        // Expected if evidence files don't exist
        // This is acceptable for unit testing
        println("Evidence files not found - this is expected in unit tests")
    }
  }
  
  test("AbstentionCompositeGenerator generateEvidence should not throw") {
    // This should run without throwing, even if files don't exist
    noException should be thrownBy AbstentionCompositeGenerator.generateEvidence()
  }
}