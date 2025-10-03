package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.EvidenceGenerator
import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Try

/**
 * A test cases generator that caches generated test cases to disk.
 * On first run, it generates test cases using the provided generator and saves them.
 * On subsequent runs, it loads the cached test cases from disk.
 * 
 * @param underlyingGenerator The actual test cases generator to use when cache miss
 * @param cachePath Path relative to src/main/resources/test_cases/ (e.g., "abstention/5_evidence")
 * @param overwrite If true, always regenerate and overwrite existing cache (default: true)
 */
class CachingTestCasesGenerator(
  underlyingGenerator: TestCasesGenerator,
  cachePath: String,
  overwrite: Boolean = true
) extends TestCasesGenerator(underlyingGenerator.evidenceGenerator) {
  
  val resourcesPath = "src/main/resources/test_cases"
  val cacheFile = new File(s"$resourcesPath/$cachePath.json")
  
  override def generatorType: String = s"${underlyingGenerator.generatorType} (cached)"
  
  override def generatorClassType: String = underlyingGenerator.generatorClassType
  
  override def generateTestCases(): List[TestCase] = {
    if (overwrite) {
      println(s"Overwrite flag is set, regenerating test cases...")
      if (cacheFile.exists()) {
        println(s"Deleting existing cache at: ${cacheFile.getPath}")
        cacheFile.delete()
      }
      generateAndCache()
    } else if (cacheFile.exists()) {
      println(s"Loading cached test cases from: ${cacheFile.getPath}")
      loadFromCache() match {
        case Some(testCases) => 
          println(s"Successfully loaded ${testCases.size} test cases from cache")
          testCases
        case None =>
          println("Failed to load from cache, generating fresh test cases...")
          generateAndCache()
      }
    } else {
      println(s"No cache found at: ${cacheFile.getPath}, generating test cases...")
      generateAndCache()
    }
  }
  
  def generateAndCache(): List[TestCase] = {
    val testCases = underlyingGenerator.generateTestCases()
    saveToCache(testCases)
    testCases
  }
  
  def saveToCache(testCases: List[TestCase]): Unit = {
    try {
      // Ensure parent directories exist
      cacheFile.getParentFile.mkdirs()
      
      // Stream write to file to avoid memory issues with large datasets
      val writer = new PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(cacheFile)))
      try {
        writer.write("[")
        testCases.zipWithIndex.foreach { case (testCase, index) =>
          if (index > 0) writer.write(",")
          writer.write("\n  ")
          // Serialize individual test case
          val testCaseJson = TestCaseSerializer.toJsonForSingleTestCase(testCase)
          writer.write(testCaseJson)
          writer.flush() // Flush periodically to avoid buffering too much
        }
        writer.write("\n]")
      } finally {
        writer.close()
      }
      
      println(s"Cached ${testCases.size} test cases to: ${cacheFile.getPath}")
    } catch {
      case e: Exception =>
        println(s"Warning: Failed to cache test cases: ${e.getMessage}")
        e.printStackTrace()
        // Continue without caching - don't fail the evaluation
    }
  }
  
  def loadFromCache(): Option[List[TestCase]] = {
    try {
      val source = Source.fromFile(cacheFile)
      try {
        val json = source.getLines().mkString("\n")
        TestCaseSerializer.fromJson(json)
      } finally {
        source.close()
      }
    } catch {
      case e: Exception =>
        println(s"Error loading cache: ${e.getMessage}")
        None
    }
  }
}

/**
 * Factory methods for CachingTestCasesGenerator
 */
object CachingTestCasesGenerator {
  
  /**
   * Create a caching wrapper around a standard test cases generator.
   * 
   * @param evidenceGenerator The evidence generator
   * @param contextSizes Context sizes to test
   * @param cachePath Path for caching (e.g., "user_facts/3_evidence")
   * @param overwrite If true, always regenerate and overwrite existing cache (default: true)
   * @return A caching test cases generator
   */
  def createCachedStandard(
    evidenceGenerator: EvidenceGenerator,
    contextSizes: List[Int],
    cachePath: String,
    overwrite: Boolean = true
  ): TestCasesGenerator = {
    val standardGenerator = TestCasesGenerator.createStandard(evidenceGenerator, contextSizes)
    new CachingTestCasesGenerator(standardGenerator, cachePath, overwrite)
  }
  
  /**
   * Create a caching wrapper around any test cases generator.
   * 
   * @param generator The underlying generator to wrap
   * @param cachePath Path for caching
   * @param overwrite If true, always regenerate and overwrite existing cache (default: true)
   * @return A caching test cases generator
   */
  def wrap(generator: TestCasesGenerator, cachePath: String, overwrite: Boolean = true): TestCasesGenerator = {
    new CachingTestCasesGenerator(generator, cachePath, overwrite)
  }
}