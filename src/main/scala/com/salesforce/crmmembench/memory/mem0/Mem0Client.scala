package com.salesforce.crmmembench.memory.mem0

import com.salesforce.crmmembench.Config
import com.salesforce.crmmembench.questions.evidence.Conversation
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3.{SttpBackendOptions, _}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._

case class Mem0Memory(
  messages: List[Map[String, String]],
  user_id: String,
  run_id: String
)

case class SearchRequest(
  query: String,
  user_id: Option[String] = None
)


class Mem0Client(baseUrl: String = "http://localhost:8888") {
  // Use connection pooling for better concurrency
  val backend = HttpURLConnectionBackend(
    options = SttpBackendOptions.connectionTimeout(30.seconds)
  )
  val maxRetries = 20
  val baseDelayMs = 500  // Increased from 100ms - mem0 needs more time
  val maxDelayMs = 10000
  val consecutiveFailures = new AtomicInteger(0)
  
  /**
   * Initialize the Mem0 client and ensure Docker containers are running.
   */
  def initialize(): Unit = {
    Mem0DockerManager.initialize()
  }
  
  /**
   * Execute a request with exponential backoff retry logic.
   * After 10 consecutive failures, triggers Docker restart.
   */
  def executeWithRetry[T](operation: () => T, operationName: String): T = {
    var lastException: Exception = null
    
    for (attempt <- 1 to maxRetries) {
      try {
        val result = operation()
        // Reset failure counter on success
        consecutiveFailures.set(0)
        return result
      } catch {
        case e: Exception =>
          lastException = e
          val isTransactionError = e.getMessage != null && e.getMessage.contains("current transaction is aborted")
          val isConnectionError = e.getMessage != null && e.getMessage.contains("Exception when sending request")
          val isConnectionRefused = e.getCause != null && e.getCause.getMessage != null && 
                                   (e.getCause.getMessage.contains("Connection refused") || 
                                    e.getCause.getMessage.contains("connect to"))
          
          // Print detailed error information only in debug mode or after attempt 5
          val shouldPrint = Config.DEBUG || attempt >= 6
          
          if (shouldPrint) {
            println(s"⚠️  Attempt $attempt/$maxRetries failed for $operationName:")
            if (isConnectionRefused) {
              println(s"   🔌 Connection refused - mem0 service not accessible at $baseUrl")
            } else if (isTransactionError) {
              println(s"   🗄️  Database transaction error")
            } else {
              println(s"   Error type: ${e.getClass.getSimpleName}")
              println(s"   Message: ${e.getMessage}")
              if (e.getCause != null) {
                println(s"   Cause: ${e.getCause.getClass.getSimpleName} - ${e.getCause.getMessage}")
              }
            }
          }
          
          if (attempt < maxRetries) {
            // Calculate exponential backoff with jitter
            val delay = Math.min(
              baseDelayMs * Math.pow(2, attempt - 1).toInt + scala.util.Random.nextInt(100),
              maxDelayMs
            ).toLong
            
            if (shouldPrint) {
              println(s"   Retrying in ${delay}ms...")
            }
            Thread.sleep(delay)
            
            // If it's a transaction error, give PostgreSQL more time to recover
            if (isTransactionError) {
              Thread.sleep(1000)
            }
            
            // Just log connection issues, don't try to fix them automatically
            if (isConnectionRefused && attempt <= 2 && shouldPrint) {
              println("   ⚠️  Cannot connect to mem0. Make sure Docker containers are running.")
            }
          }
      }
    }
    
    // All retries failed for this operation
    println(s"❌ All $maxRetries attempts failed for $operationName.")
    
    // If this operation failed after all retries, try emergency restart
    println("🚨 Operation failed after all retries. Requesting emergency restart...")
    
    // Use a synchronized block to coordinate restart attempts
    this.synchronized {
      try {
        Mem0DockerManager.emergencyRestart()
        // Reset the global failure counter after restart
        consecutiveFailures.set(0)
        
        // Give containers time to stabilize after restart
        Thread.sleep(10000)
        
        // Try one more time after restart
        println(s"🔄 Retrying $operationName after emergency restart...")
        try {
          val result = operation()
          println(s"✅ $operationName succeeded after emergency restart!")
          return result
        } catch {
          case e: Exception =>
            println(s"❌ $operationName failed even after emergency restart: ${e.getMessage}")
            throw lastException
        }
      } catch {
        case e: Exception =>
          println(s"❌ Failed to perform emergency restart: ${e.getMessage}")
          throw lastException
      }
    }
  }
  
  def postMemory(conversation: Conversation, userId: String): String = {
    executeWithRetry(() => {
      val messages = conversation.messages.map { msg =>
        Map("role" -> msg.speaker, "content" -> msg.text)
      }
      
      val runId = conversation.id.getOrElse(throw new IllegalArgumentException("Conversation must have an id"))
      val memory = Mem0Memory(messages, userId, runId)
      val jsonBody = memory.asJson.noSpaces
      
      val request = basicRequest
        .post(uri"$baseUrl/memories")
        .header("Content-Type", "application/json")
        .body(jsonBody)
      
      val response = request.send(backend)
      response.body match {
        case Right(body) => body
        case Left(error) => throw new Exception(s"Failed to post memory: $error")
      }
    }, "postMemory")
  }
  
  def getMemories(userId: String): String = {
    executeWithRetry(() => {
      val request = basicRequest
        .get(uri"$baseUrl/memories?user_id=$userId")
        .header("Accept", "application/json")
      
      val response = request.send(backend)
      response.body match {
        case Right(body) => body
        case Left(error) => throw new Exception(s"Failed to get memories: $error")
      }
    }, "getMemories")
  }
  
  def deleteMemory(memoryId: String): String = {
    executeWithRetry(() => {
      val request = basicRequest
        .delete(uri"$baseUrl/memories/$memoryId")
        .header("Accept", "application/json")
      
      val response = request.send(backend)
      response.body match {
        case Right(body) => body
        case Left(error) => throw new Exception(s"Failed to delete memory: $error")
      }
    }, "deleteMemory")
  }
  
  def deleteAllMemories(userId: String): String = {
    executeWithRetry(() => {
      val request = basicRequest
        .delete(uri"$baseUrl/memories?user_id=$userId")
        .header("Accept", "application/json")
      
      val response = request.send(backend)
      response.body match {
        case Right(body) => body
        case Left(error) => throw new Exception(s"Failed to delete all memories: $error")
      }
    }, "deleteAllMemories")
  }
  
  def searchMemories(query: String, userId: Option[String] = None): String = {
    executeWithRetry(() => {
      val searchRequest = SearchRequest(query, userId)
      val jsonBody = searchRequest.asJson.noSpaces
      
      val request = basicRequest
        .post(uri"$baseUrl/search")
        .header("Content-Type", "application/json")
        .body(jsonBody)
      
      val response = request.send(backend)
      response.body match {
        case Right(body) => body
        case Left(error) => throw new Exception(s"Failed to search memories: $error")
      }
    }, "searchMemories")
  }
  
  def reset(): String = {
    executeWithRetry(() => {
      val request = basicRequest
        .post(uri"$baseUrl/reset")
        .header("Accept", "application/json")
      
      val response = request.send(backend)
      response.body match {
        case Right(body) => body
        case Left(error) => throw new Exception(s"Failed to reset memories: $error")
      }
    }, "reset")
  }
}