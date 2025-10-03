package com.salesforce.crmmembench

import java.util.concurrent.ForkJoinPool
import scala.collection.parallel.{ForkJoinTaskSupport, ParIterable}
import scala.util.{Failure, Success, Try}

/**
 * Centralized configuration constants for the CRM Memory Benchmark system.
 * All hardcoded values should be moved here for easy configuration.
 */
object Config {
  // Global Configuration
  /** Enable verbose debug logging for evidence generation */
  val DEBUG: Boolean = false
  
  /** Enable mem0 unit tests. When false, mem0 unit tests will be skipped. 
    * NOTE: This flag only affects unit tests, NOT production evaluation runs.
    * Production mem0 evaluations will check for mem0 availability at runtime. */
  val RUN_MEM0_TESTS: Boolean = false
  
  // Evidence Generation Configuration
  object Evidence {

    /** Maximum retry attempts for evidence generation with verification */
    val EVIDENCE_GENERATION_MAX_RETRIES: Int = 5
    
    /** Maximum retry attempts for use case generation */
    val USE_CASE_GENERATION_MAX_RETRIES: Int = 10
    
    /** Maximum retry attempts for conversation generation */
    val CONVERSATION_GENERATION_MAX_RETRIES: Int = 10
    
    /** Number of consecutive correct answers required for evidence verification to pass */
    val VERIFICATION_CONSECUTIVE_PASSES_REQUIRED: Int = 2
    
    /** Log intermediate steps during evidence generation for debugging */
    val LOG_INTERMEDIATE_STEPS: Boolean = true
  }
  
  // Evaluation Configuration
  object Evaluation {
    /** Context sizes to test in large context evaluation */
    val CONTEXT_SIZES: List[Int] = List(1, 2, 3, 4, 5, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)

    /** Maximum retry attempts for getting model answers */
    val MODEL_ANSWER_MAX_RETRIES: Int = 20
    
    /** Maximum retry attempts for answer verification */
    val ANSWER_VERIFICATION_MAX_RETRIES: Int = 20
    
    /** Limit for showing incorrect answers in verification results */
    val INCORRECT_ANSWERS_DISPLAY_LIMIT: Int = 10
    
    /** Enable caching of test cases to disk for faster subsequent runs */
    val USE_CACHED_TEST_CASES: Boolean = true
  }
  
  // Threading Configuration
  object Threading {

    /** Number of threads for evidence item processing */
    val EVIDENCE_ITEM_THREADS: Int = 100

    /** Number of threads for person processing in evidence generation */
    val PERSON_PROCESSING_THREADS: Int = 100
    
    /** Number of threads for use case processing */
    val USE_CASE_PROCESSING_THREADS: Int = 100
    
  }
  
  // Reporting Configuration
  object Reporting {
    /** Interval in seconds for evaluation stats reporting */
    val EVALUATION_STATS_INTERVAL_SECONDS: Int = 
      sys.props.getOrElse("crmmembench.stats.evaluation.interval", "15").toInt
    
    /** Interval in seconds for conversation generation stats reporting */
    val CONVERSATION_STATS_INTERVAL_SECONDS: Int = 
      sys.props.getOrElse("crmmembench.stats.conversation.interval", "10").toInt
    
    /** Attempt threshold for logging retry failures */
    val RETRY_LOGGING_THRESHOLD: Int = 10
  }
  
  // Generation Configuration
  object Generation {
    /** Number of use cases to generate per person */
    val USE_CASES_PER_PERSON: Int = 100
    
    /** Number of people to process. If value is more than number of available people, all people will be processed */
    val PEOPLE_TO_PROCESS: Int = 50
    
    /** Maximum time in hours to process a single person before timeout */
    val PERSON_PROCESSING_TIMEOUT_HOURS: Int = 10
    
    /** Maximum time in minutes to process a single use case before timeout */
    val USE_CASE_TIMEOUT_MINUTES: Int = 120
    
    /** Enable timeout feature for person processing */
    val ENABLE_PERSON_TIMEOUT: Boolean = true
    
    /** Enable timeout feature for use case processing */
    val ENABLE_USE_CASE_TIMEOUT: Boolean = true
    
    /** Expected total evidence items for each evidence count */
    val EXPECTED_EVIDENCE_TOTALS: Map[Int, Int] = Map(
      1 -> 5000,
      2 -> 3000,
      3 -> 2000,
      4 -> 1000,
      5 -> 500,
      6 -> 500
    )
    
    /**
     * Get expected total evidence items for a given evidence count.
     * For ChangingEvidence, which starts at 2, we decrement the count by 1.
     */
    def getExpectedTotal(evidenceCount: Int, isChangingEvidence: Boolean = false): Int = {
      val adjustedCount = if (isChangingEvidence && evidenceCount >= 2) evidenceCount - 1 else evidenceCount
      EXPECTED_EVIDENCE_TOTALS.getOrElse(adjustedCount, 100) // Default to 100 if not found
    }
    
    /**
     * Calculate use cases per person based on evidence count and number of people.
     * Returns the ceiling to ensure we meet or exceed the expected total.
     */
    def calculateUseCasesPerPerson(evidenceCount: Int, peopleCount: Int, isChangingEvidence: Boolean = false): Int = {
      val expectedTotal = getExpectedTotal(evidenceCount, isChangingEvidence)
      Math.ceil(expectedTotal.toDouble / peopleCount).toInt
    }
  }
}

object Utils {
  import com.salesforce.crmmembench.questions.evidence.EvidencePayload
  import io.circe.generic.auto._
  import io.circe.syntax.EncoderOps

  import java.io.{File, PrintWriter}
  import java.nio.file.{Files, Paths}
  import scala.io.Source

  def getCurrentVersion() = "default"

  /**
   * Retry a function with exponential backoff.
   *
   * @param maxRetries Maximum number of retries
   * @param initialDelay Initial delay between retries in milliseconds
   * @param maxDelay Maximum delay between retries in milliseconds
   * @param f The function to retry
   * @tparam T The return type of the function
   * @return The result of the function if successful
   * @throws Exception The last exception encountered after all retries are exhausted
   */
  def retry[T](maxRetries: Int, initialDelay: Long = 1000, maxDelay: Long = 20000)(f: => T): T = {
    def loop(retries: Int, delay: Long): T = {
      Try(f) match {
        case Success(result) => result
        case Failure(exception) if retries > 0 =>
          val attemptNumber = maxRetries - retries + 1
          // Only log failures after attempt threshold
          if (attemptNumber > Config.Reporting.RETRY_LOGGING_THRESHOLD) {
            println(s"Retry attempt ${attemptNumber}/${maxRetries} failed with: ${exception.getMessage}")
          }
          // Sleep with exponential backoff
          Thread.sleep(delay)
          // Calculate next delay with exponential backoff, but cap it at maxDelay
          val nextDelay = Math.min(delay * 2, maxDelay)
          loop(retries - 1, nextDelay)
        case Failure(exception) =>
          println(s"All ${maxRetries} retry attempts failed (only attempts ${Config.Reporting.RETRY_LOGGING_THRESHOLD + 1}+ were logged). Last error: ${exception.getMessage}")
          throw exception
      }
    }

    loop(maxRetries, initialDelay)
  }

  /**
   * Creates a parallel collection with a custom thread pool.
   * Automatically manages thread pool lifecycle and task support configuration.
   *
   * @param collection The collection to parallelize
   * @param threadCount Number of threads to use for parallel processing
   * @param operation The operation to perform on the parallel collection
   * @tparam A The element type of the collection
   * @tparam C The collection type
   * @tparam B The result type of the operation
   * @return The result of the operation
   */
  def withParallelProcessing[A, C[X] <: Iterable[X], B](
    collection: C[A], 
    threadCount: Int
  )(operation: ParIterable[A] => B): B = {
    val customThreadPool = new ForkJoinPool(threadCount)
    val customTaskSupport = new ForkJoinTaskSupport(customThreadPool)
    
    try {
      val collectionPar = collection.par
      collectionPar.tasksupport = customTaskSupport
      operation(collectionPar)
    } finally {
      customThreadPool.shutdown()
    }
  }

  /**
   * Creates a parallel collection with a custom thread pool for simple foreach operations.
   * 
   * @param collection The collection to parallelize
   * @param threadCount Number of threads to use for parallel processing
   * @param operation The foreach operation to perform
   * @tparam A The element type of the collection
   * @tparam C The collection type
   */
  def parallelForeach[A, C[X] <: Iterable[X]](
    collection: C[A], 
    threadCount: Int
  )(operation: A => Unit): Unit = {
    withParallelProcessing(collection, threadCount)(_.foreach(operation))
  }

  /**
   * Creates a parallel collection with a custom thread pool for map operations.
   * 
   * @param collection The collection to parallelize
   * @param threadCount Number of threads to use for parallel processing
   * @param operation The map operation to perform
   * @tparam A The element type of the collection
   * @tparam B The result element type
   * @tparam C The collection type
   * @return A list containing the mapped results
   */
  def parallelMap[A, B, C[X] <: Iterable[X]](
    collection: C[A], 
    threadCount: Int
  )(operation: A => B): List[B] = {
    withParallelProcessing(collection, threadCount)(_.map(operation).toList)
  }


  /**
   * Save evidence payload to a JSON file.
   *
   * @param payload The evidence payload to save
   * @param outputPath The directory path where to save the files
   */
  def saveEvidencePayload(payload: EvidencePayload, outputPath: String): Unit = {
    try {
      Files.createDirectories(Paths.get(outputPath))
      
      val fileName = s"changing_evidence_${System.currentTimeMillis()}.json"
      val filePath = Paths.get(outputPath, fileName)
      
      val jsonString = payload.asJson.spaces2
      
      val writer = new PrintWriter(new File(filePath.toString))
      try {
        writer.write(jsonString)
        println(s"Saved evidence payload to: ${filePath}")
      } finally {
        writer.close()
      }
    } catch {
      case e: Exception =>
        println(s"Error saving evidence payload to $outputPath: ${e.getMessage}")
        throw e
    }
  }

  /**
   * Get the current git checkpoint (commit hash).
   * 
   * @return The git commit hash, or "unknown" if unable to retrieve
   */
  def getGitCheckpoint(): String = {
    try {
      import scala.sys.process._
      val gitHash = "git rev-parse HEAD".!!.trim
      gitHash
    } catch {
      case e: Exception =>
        println(s"Warning: Unable to get git checkpoint: ${e.getMessage}")
        "unknown"
    }
  }

  /**
   * Load personas from the enriched backgrounds directory.
   *
   * @return List of persona strings
   */
  def loadPersonas(): List[String] = {
    try {
      val personasDir = new File("src/main/resources/personas/enriched_backgrounds")
      if (!personasDir.exists() || !personasDir.isDirectory) {
        println(s"Personas directory not found: ${personasDir.getAbsolutePath}")
        return List("Default persona: You are a helpful assistant.")
      }

      val personaFiles = personasDir.listFiles().filter(_.getName.endsWith(".txt"))
      val personas = personaFiles.flatMap { file =>
        try {
          val source = Source.fromFile(file)
          val content = source.mkString.trim
          source.close()
          if (content.nonEmpty) Some(content) else None
        } catch {
          case e: Exception =>
            println(s"Error reading persona file ${file.getName}: ${e.getMessage}")
            None
        }
      }.toList

      if (personas.isEmpty) {
        List("Default persona: You are a helpful assistant.")
      } else {
        personas
      }
    } catch {
      case e: Exception =>
        println(s"Error loading personas: ${e.getMessage}")
        List("Default persona: You are a helpful assistant.")
    }
  }
}
