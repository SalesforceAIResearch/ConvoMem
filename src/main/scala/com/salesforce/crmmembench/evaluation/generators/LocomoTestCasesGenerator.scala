package com.salesforce.crmmembench.evaluation.generators

import com.salesforce.crmmembench.evaluation.{TestCase, TestCaseSerializer, TestCasesGenerator}
import com.salesforce.crmmembench.questions.evidence.generation._

import java.io.File
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * Category definition for Locomo benchmark questions.
 * 
 * @param id Unique identifier used for file paths and results
 * @param name Human-readable name for logging and display
 * @param description Detailed description of what this category tests
 * @param folderName Name of the folder containing the test case files
 */
case class LocomoCategory(
  id: String,
  name: String,
  description: String,
  folderName: String
)

/**
 * Singleton objects for each Locomo category.
 * These provide convenient access to category definitions.
 */
object LocomoCategories {
  
  val BasicFacts = LocomoCategory(
    id = "locomo_basic_facts",
    name = "Locomo Basic Facts",
    description = "Basic fact recall - What/Who questions",
    folderName = "category_1_basic_facts"
  )
  
  val Temporal = LocomoCategory(
    id = "locomo_temporal",
    name = "Locomo Temporal",
    description = "Temporal questions - When questions",
    folderName = "category_2_temporal"
  )
  
  val Reasoning = LocomoCategory(
    id = "locomo_reasoning",
    name = "Locomo Reasoning",
    description = "Reasoning and inference - Counterfactual questions",
    folderName = "category_3_reasoning"
  )
  
  val MultiSession = LocomoCategory(
    id = "locomo_multi_session",
    name = "Locomo Multi-Session",
    description = "Cross-session facts - Information from multiple sessions",
    folderName = "category_4_multi_session"
  )
  
  val Abstention = LocomoCategory(
    id = "locomo_abstention",
    name = "Locomo Abstention",
    description = "Adversarial/Abstention - Questions with no valid answer",
    folderName = "category_5_abstention"
  )
  
  /**
   * All categories in order.
   */
  val all: List[LocomoCategory] = List(
    BasicFacts,
    Temporal,
    Reasoning,
    MultiSession,
    Abstention
  )
  
  /**
   * Get category by numeric ID (1-5) for backward compatibility.
   */
  def fromNumber(num: Int): Option[LocomoCategory] = num match {
    case 1 => Some(BasicFacts)
    case 2 => Some(Temporal)
    case 3 => Some(Reasoning)
    case 4 => Some(MultiSession)
    case 5 => Some(Abstention)
    case _ => None
  }
  
  /**
   * Get category by string ID.
   */
  def fromId(id: String): Option[LocomoCategory] = 
    all.find(_.id == id)
}

/**
 * Test cases generator for Locomo benchmark data.
 * 
 * Loads pre-converted TestCases from JSON files that were created by
 * convert_locomo_to_testcases.py. Each dataset contains multiple questions
 * referencing the same conversation sessions.
 * 
 * @param category The Locomo category to load
 * @param datasetIndices Which datasets to load (0-9), defaults to all
 */
class LocomoTestCasesGenerator(
  val category: LocomoCategory,
  val datasetIndices: List[Int] = (0 to 9).toList
) extends TestCasesGenerator(null) { // null since we don't use EvidenceGenerator
  
  require(datasetIndices.nonEmpty, "Must specify at least one dataset index")
  require(datasetIndices.forall(i => i >= 0 && i <= 9), "Dataset indices must be between 0 and 9")
  
  private val resourcesPath = "src/main/resources/test_cases/locomo"
  
  override def generateTestCases(): List[TestCase] = {
    println(s"Loading ${category.name} test cases...")
    val testCases = loadTestCasesFromFiles()
    println(s"Loaded ${testCases.size} test cases with ${testCases.flatMap(_.evidenceItems).size} total questions")
    testCases
  }
  
  private def loadTestCasesFromFiles(): List[TestCase] = {
    datasetIndices.flatMap { idx =>
      val path = s"$resourcesPath/${category.folderName}/dataset_$idx.json"
      loadTestCaseFromFile(path) match {
        case Some(testCase) =>
          println(s"  Loaded dataset_$idx: ${testCase.evidenceItems.size} questions, ${testCase.conversations.size} conversations")
          Some(testCase)
        case None =>
          println(s"  Warning: Could not load dataset_$idx from $path")
          None
      }
    }
  }
  
  private def loadTestCaseFromFile(path: String): Option[TestCase] = {
    val file = new File(path)
    if (!file.exists()) {
      return None
    }
    
    Try {
      val source = Source.fromFile(file)
      try {
        val json = source.getLines().mkString("\n")
        // Parse single TestCase from JSON
        TestCaseSerializer.fromJson(s"[$json]") match {
          case Some(testCases) if testCases.nonEmpty => Some(testCases.head)
          case _ => None
        }
      } finally {
        source.close()
      }
    } match {
      case Success(result) => result
      case Failure(e) =>
        println(s"Error loading test case from $path: ${e.getMessage}")
        None
    }
  }
  
  override def generatorType: String = s"${category.name} (${category.description})"
  
  override def generatorClassType: String = category.id
  
  // Override getEvidenceCount to return 0 since we don't have a traditional evidence generator
  override def getEvidenceCount(): Int = 0
}

/**
 * Singleton generators for each Locomo category.
 * These provide convenient pre-configured generators.
 */
object LocomoBasicFactsGenerator extends LocomoTestCasesGenerator(LocomoCategories.BasicFacts)
object LocomoTemporalGenerator extends LocomoTestCasesGenerator(LocomoCategories.Temporal)
object LocomoReasoningGenerator extends LocomoTestCasesGenerator(LocomoCategories.Reasoning)
object LocomoMultiSessionGenerator extends LocomoTestCasesGenerator(LocomoCategories.MultiSession)
object LocomoAbstentionGenerator extends LocomoTestCasesGenerator(LocomoCategories.Abstention)

/**
 * Factory object for creating Locomo test case generators.
 */
object LocomoTestCasesGenerator {
  
  /**
   * Create a generator for a specific category.
   * 
   * @param category The category to generate test cases for
   * @return TestCasesGenerator for that category
   */
  def apply(category: LocomoCategory): LocomoTestCasesGenerator = 
    new LocomoTestCasesGenerator(category)
  
  /**
   * Create a generator for a specific category with specific datasets.
   * 
   * @param category The category to generate test cases for
   * @param datasetIndices Which datasets to load (0-9)
   * @return TestCasesGenerator for that category and datasets
   */
  def apply(category: LocomoCategory, datasetIndices: List[Int]): LocomoTestCasesGenerator = 
    new LocomoTestCasesGenerator(category, datasetIndices)
  
  /**
   * Get all singleton generators.
   * 
   * @return List of all pre-configured generators
   */
  def allGenerators: List[LocomoTestCasesGenerator] = List(
    LocomoBasicFactsGenerator,
    LocomoTemporalGenerator,
    LocomoReasoningGenerator,
    LocomoMultiSessionGenerator,
    LocomoAbstentionGenerator
  )
}