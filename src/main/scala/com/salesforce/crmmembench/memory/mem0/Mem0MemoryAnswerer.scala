package com.salesforce.crmmembench.memory.mem0

import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel}
import com.salesforce.crmmembench.evaluation.memory.{AnswerResult, MemoryAnswerer, MemoryPromptUtils}
import com.salesforce.crmmembench.questions.evidence.Conversation
import com.salesforce.crmmembench.{Config, Utils}
import io.circe.parser._

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
 * Mem0-based memory answerer that uses external memory service for question answering.
 * 
 * This implementation:
 * - Stores conversations in mem0 memory service (expensive indexing operation)
 * - Uses mem0's search functionality to retrieve relevant memories
 * - Generates answers based on retrieved memories
 * - Uses unique user IDs per instance for thread isolation
 * - Supports bulk conversation loading to minimize API calls
 * 
 * This implementation is stateful and NOT thread-safe beyond the user isolation.
 * Create separate instances for concurrent use.
 */
class Mem0MemoryAnswerer(
  model: LLMModel = Gemini.flash,
  val mem0Client: Mem0Client = new Mem0Client(),
  userIdPrefix: String = "crm-benchmark"
) extends MemoryAnswerer {

  // Generate unique user ID for this instance to ensure thread isolation
  val userId = s"$userIdPrefix-${UUID.randomUUID()}"
  
  // Track if we've stored memories for cleanup
  val hasStoredMemories = new AtomicBoolean(false)
  
  // Track loaded conversations for potential reloading
  val loadedConversations = ListBuffer[Conversation]()
  
  // Track when we last indexed conversations
  var lastIndexTime = 0L
  
  // Track if we've initialized this instance
  val initialized = new AtomicBoolean(false)
  
  override def initialize(): Unit = {
    if (initialized.compareAndSet(false, true)) {
      // Only initialize once per instance
      mem0Client.initialize()
    }
  }
  
  override def addConversation(conversation: Conversation): Unit = {
    require(conversation.id.isDefined, "Conversation must have an id")
    
    // Ensure initialized before use
    if (!initialized.get()) {
      initialize()
    }
    
    try {
      mem0Client.postMemory(conversation, userId)
      loadedConversations += conversation
      hasStoredMemories.set(true)
      
      // Update index timestamp
      lastIndexTime = System.currentTimeMillis()
    } catch {
      case e: Exception =>
        println(s"      Failed to add conversation to mem0: ${e.getMessage}")
        throw e
    }
  }
  
  override def addConversations(conversations: List[Conversation]): Unit = {
    // Ensure initialized before use
    if (!initialized.get()) {
      initialize()
    }
    
    // For mem0, we still need to add one by one as there's no bulk endpoint
    // But we can at least batch the indexing delay
    val totalConversations = conversations.size
    val storedConvIds = ListBuffer[String]()
    
    conversations.zipWithIndex.foreach { case (conversation, index) =>
      // Conversation must already have an id
      require(conversation.id.isDefined, s"Conversation at index $index must have an id")
      
      try {
        // For small conversation counts, print each one. For larger counts, print every 10th
        if (Config.DEBUG && (totalConversations <= 10 || index % 10 == 0)) {
          println(s"      📝 Mem0: Storing conversation ${index + 1}/$totalConversations with ID: ${conversation.id.getOrElse("NO-ID")}")
        }
        mem0Client.postMemory(conversation, userId)
        loadedConversations += conversation
        storedConvIds += conversation.id.get
      } catch {
        case e: Exception =>
          println(s"      Failed to add conversation to mem0: ${e.getMessage}")
          throw e
      }
    }
    
    if (conversations.nonEmpty) {
      hasStoredMemories.set(true)
      
      // Enhanced indexing wait strategy based on conversation count
      val baseDelay = 1000
      val perConvDelay = if (totalConversations <= 10) 200 else 100
      val maxDelay = 100000
      val calculatedDelay = Math.min(baseDelay + (totalConversations * perConvDelay), maxDelay)
      
      if (Config.DEBUG) {
        println(s"      ⏳ Waiting ${calculatedDelay}ms for mem0 to index ${totalConversations} conversations...")
      }
      Thread.sleep(calculatedDelay)
      
      // Verify indexing is complete by checking if we can retrieve memories
      if (totalConversations > 0) {
        verifyIndexingComplete(storedConvIds.toList)
      }
      
      // Update index timestamp after successful loading
      lastIndexTime = System.currentTimeMillis()
      if (Config.DEBUG) {
        println(s"      ✅ Indexed ${totalConversations} conversations at ${new java.util.Date(lastIndexTime)}")
      }
    }
  }
  
  /**
   * Verify that mem0 has finished indexing by attempting to retrieve memories.
   * This helps ensure consistency when using multiple threads.
   */
  def verifyIndexingComplete(convIds: List[String]): Unit = {
    val maxVerificationAttempts = 5
    val verificationDelay = 10000
    
    for (attempt <- 1 to maxVerificationAttempts) {
      try {
        // Try to retrieve memories to ensure they're indexed
        val memories = mem0Client.getMemories(userId)
        
        // If we get a response without error, indexing is likely complete
        if (Config.DEBUG) {
          println(s"      ✓ Mem0 indexing verified on attempt $attempt")
        }
        return
      } catch {
        case e: Exception =>
          if (attempt < maxVerificationAttempts) {
            if (Config.DEBUG) {
              println(s"      ⏳ Indexing not yet complete, waiting... (attempt $attempt/$maxVerificationAttempts)")
            }
            Thread.sleep(verificationDelay)
          } else {
            // On final attempt, just log but don't fail - mem0 might still work
            if (Config.DEBUG) {
              println(s"      ⚠️  Could not verify indexing completion, proceeding anyway")
            }
          }
      }
    }
  }

  override def answerQuestion(question: String, testCaseId: String): AnswerResult = {
    // Ensure initialized before use
    if (!initialized.get()) {
      initialize()
    }
    
    // Check if restart happened after our last index
    val restartTime = Mem0DockerManager.getLastRestartTime()
    
    if (restartTime > lastIndexTime && loadedConversations.nonEmpty) {
      println(s"      ⚠️  Restart detected (${new java.util.Date(restartTime)}) after indexing (${new java.util.Date(lastIndexTime)})")
      reloadConversations()
    } else if (restartTime > lastIndexTime && loadedConversations.isEmpty) {
      // No conversations to reload, just update timestamp
      lastIndexTime = System.currentTimeMillis()
      hasStoredMemories.set(false)
    }
    
    if (!hasStoredMemories.get()) {
      println("      No conversations loaded in mem0")
      return AnswerResult(None, List.empty)
    }
    
    // Brief pause to ensure any recent indexing operations are complete
    // This is especially important when multiple threads are working concurrently
    Thread.sleep(200)
    
    Try {
      // Search for relevant memories based on the question
      val searchResult = mem0Client.searchMemories(question, Some(userId))
      
      // Parse search results to extract memories and their run_ids
      val (memories, runIds) = extractMemoriesAndRunIdsFromSearchResult(searchResult)
      
      // Debug: only print if we found something
      if (Config.DEBUG && memories.nonEmpty) {
        println(s"      🔍 Raw search response (first 1000 chars): ${searchResult.take(1000)}")
        println(s"      📋 Found ${memories.size} memories")
        println(s"      🆔 Found ${runIds.size} run_ids: ${runIds.take(5).mkString(", ")}${if(runIds.size > 5) "..." else ""}")
      }
      
      // Generate answer using retrieved memories
      val responseOpt = Utils.retry(Config.Evaluation.MODEL_ANSWER_MAX_RETRIES) {
        val prompt = buildPromptWithMemories(question, memories)
        
        if (Config.DEBUG && memories.nonEmpty) {
          println(s"      🤖 Generating answer with ${memories.size} memories")
          if (memories.size > 10) {
            println(s"      📄 First memory: ${memories.head.take(100)}...")
            println(s"      📄 Last memory: ${memories.last.take(100)}...")
          }
        }
        
        model.generateContent(prompt) match {
          case Success(response) => response
          case Failure(exception) => throw exception
        }
      }
      AnswerResult(
        answer = Some(responseOpt.content),
        retrievedConversationIds = runIds.toList,
        inputTokens = responseOpt.tokenUsage.map(_.inputTokens),
        outputTokens = responseOpt.tokenUsage.map(_.outputTokens),
        cost = Some(responseOpt.cost),
        cachedInputTokens = None,
        memorySystemResponses = List(searchResult)
      )
    } match {
      case Success(result) => result
      case Failure(exception) =>
        println(s"      Failed to get mem0-based answer: ${exception.getMessage}")
        AnswerResult(None, List.empty)
    }
  }

  override def getMemoryType: String = "mem0"
  

  override def clearMemory(): Unit = {
    if (hasStoredMemories.get()) {
      try {
        mem0Client.deleteAllMemories(userId)
        loadedConversations.clear()
        hasStoredMemories.set(false)
      } catch {
        case e: Exception => 
          println(s"      Warning: Failed to clear memories for user $userId: ${e.getMessage}")
      }
    }
  }

  override def cleanup(): Unit = {
    clearMemory()
  }
  
  /**
   * Get the unique user ID for this instance (useful for debugging)
   */
  def getUserId: String = userId

  def extractMemoriesAndRunIdsFromSearchResult(searchResult: String): (List[String], Set[String]) = {
    parse(searchResult) match {
      case Right(json) =>
        // Try different possible field names for the memory array
        val memoriesArray: List[io.circe.Json] = json.hcursor.downField("memories").as[List[io.circe.Json]].toOption.getOrElse {
          json.hcursor.downField("results").as[List[io.circe.Json]].toOption.getOrElse {
            // Maybe it's just a plain array at the root?
            json.as[List[io.circe.Json]].toOption.getOrElse {
              if (Config.DEBUG) {
                println(s"      ⚠️  Could not find memories array in expected locations")
                println(s"      ⚠️  Top-level fields: ${json.asObject.map(_.keys.mkString(", ")).getOrElse("not an object")}")
              }
              List.empty[io.circe.Json]
            }
          }
        }
        
        val memories = memoriesArray.flatMap { memoryJson =>
          val memoryOpt = memoryJson.hcursor.downField("memory").as[String].toOption
          if (Config.DEBUG && memoriesArray.size <= 5 && memoryOpt.isDefined) {
            println(s"      📝 Memory content: ${memoryOpt.get.take(200)}...")
          }
          memoryOpt
        }
        
        val runIds = memoriesArray.flatMap { memoryJson =>
          val runIdOpt = memoryJson.hcursor.downField("run_id").as[String].toOption
          if (Config.DEBUG && memoriesArray.nonEmpty) {
            // Always show what fields are available in each memory entry
            val fields = memoryJson.asObject.map(_.keys.toList.sorted).getOrElse(List.empty)
            println(s"      🔍 Memory entry fields: ${fields.mkString(", ")}")
            if (runIdOpt.isDefined) {
              println(s"      ✅ Found run_id: ${runIdOpt.get}")
            } else {
              println(s"      ❌ No run_id field found in memory entry")
            }
          }
          runIdOpt
        }.toSet
        
        // Only print debug info if we found something
        if (Config.DEBUG && memories.nonEmpty) {
          println(s"      🔍 Raw search result JSON: ${json.spaces2.take(500)}...")
          println(s"      📊 Found ${memories.size} memories with ${runIds.size} run_ids")
        }
        
        (memories, runIds)
      case Left(e) => 
        if (Config.DEBUG) {
          println(s"      Failed to parse search results: ${e.getMessage}")
        }
        (List.empty, Set.empty)
    }
  }

  def buildPromptWithMemories(question: String, memories: List[String]): String = {
    // Use the common prompt utility with judge criteria
    MemoryPromptUtils.buildMemoryBasedPrompt(question, memories)
  }
  
  /**
   * Reload all conversations after a restart has been detected.
   */
  def reloadConversations(): Unit = {
    println(s"      📝 Reloading ${loadedConversations.size} conversations after mem0 restart...")
    
    val conversationsToReload = loadedConversations.toList
    loadedConversations.clear()
    hasStoredMemories.set(false)
    
    try {
      // Re-add all conversations
      addConversations(conversationsToReload)
      println(s"      ✅ Successfully reloaded ${conversationsToReload.size} conversations")
    } catch {
      case e: Exception =>
        println(s"      ❌ Failed to reload conversations: ${e.getMessage}")
        // Don't throw - just log the error and continue with empty memory
        hasStoredMemories.set(false)
    }
  }
}