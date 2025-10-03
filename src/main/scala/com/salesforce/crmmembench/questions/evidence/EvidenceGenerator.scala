package com.salesforce.crmmembench.questions.evidence

import com.salesforce.crmmembench.GeneralPrompts.PROJECT_BACKGROUND
import com.salesforce.crmmembench.LLM_endpoints.Claude.sonnetJson
import com.salesforce.crmmembench.LLM_endpoints.{Gemini, LLMModel, OpenAI, RandomModelSelector}
import com.salesforce.crmmembench._
import com.salesforce.crmmembench.conversations.{ConversationGenerator, ConversationPromptParts, ConversationValidator}
import com.salesforce.crmmembench.questions.evidence.generation._
import io.circe.generic.auto._
import io.circe.parser.decode

import java.util.UUID

/**
 * Base trait for evidence generation that supports variable evidence counts.
 * This replaces the separate single evidence and multi-session evidence generators.
 * 
 * Implementing classes must provide:
 * - config: The evidence configuration
 * - getEvidenceTypeName: The name of the evidence type
 * - getUseCaseSummaryPromptParts: Prompt parts for use case generation
 * - getEvidenceCorePromptParts: Prompt parts for evidence core generation
 * - getConversationPromptParts: Prompt parts for conversation generation
 */
trait EvidenceGenerator {
  
  /**
   * The evidence configuration for this generator.
   * Must be provided by implementing classes.
   */
  def config: EvidenceConfig
  
  /**
   * Get the evidence count for this generator.
   * Returns 1 by default, subclasses should override if they generate more.
   */
  def getEvidenceCount(): Int = config.evidenceCount

  // Conversation generator for this evidence generator instance (will be initialized after llmModel is set)
  lazy val conversationGenerator = new ConversationGenerator(llmModel)
  
  /**
   * Flag to run in short mode for testing.
   * When true, only processes 3 personas with 2 use cases each.
   */
  val runShort: Boolean = false
  
  /**
   * Number of use cases to generate per person.
   * Can be overridden by subclasses to customize the number of use cases.
   * If not overridden, will be calculated based on evidence count and expected totals.
   */
  lazy val useCasesPerPersonConfig: Int = {
    // Check if this is ChangingEvidence by looking at the evidence type name
    val isChangingEvidence = getEvidenceTypeName.toLowerCase.contains("changing")
    // Calculate based on the evidence count and number of people
    Config.Generation.calculateUseCasesPerPerson(
      getEvidenceCount(), 
      targetPeopleCount, 
      isChangingEvidence
    )
  }
  
  /**
   * Number of people to process.
   * Can be overridden by subclasses to customize the number of personas.
   * In short mode, always processes 3 personas regardless of this setting.
   */
  val targetPeopleCount: Int = Config.Generation.PEOPLE_TO_PROCESS
  
  /**
   * The LLM model to use for generation.
   * Can be overridden by subclasses to use different models.
   * Default is Gemini Flash with JSON mode for efficiency.
   */
  val llmModel: LLMModel = Gemini.proJson //new RandomModelSelector(List(Gemini.proJson, OpenAI.gpt4oJson/*, sonnetJson*/))


  /**
   * Count how many evidence items have already been generated for each person.
   * Returns a map of personId to evidence item count.
   */
  def countExistingEvidencePerPerson(): Map[String, Int] = {
    try {
      val evidenceItems = loadEvidenceItems()
      evidenceItems.groupBy(_.personId.getOrElse("unknown"))
        .map { case (personId, items) => (personId, items.length) }
    } catch {
      case _: Exception => 
        // If no evidence exists yet, return empty map
        Map.empty[String, Int]
    }
  }
  
  /**
   * Select people to process based on existing evidence counts.
   * Prioritizes people with fewer existing evidence items.
   */
  def selectPeopleByExistingEvidence(
    allPersons: List[Personas.Person], 
    peopleCount: Int,
    existingCounts: Map[String, Int]
  ): List[Personas.Person] = {
    // Sort people by their existing evidence count (ascending)
    val sortedPersons = allPersons.sortBy { person =>
      existingCounts.getOrElse(person.id, 0)
    }
    
    // Take the requested number of people with the least evidence
    sortedPersons.take(peopleCount)
  }

  /**
   * Generate evidence conversations for all personas.
   */
  def generateEvidence(): Unit = {
    println("="*80)
    println(s"${config.evidenceCount.toString.toUpperCase}-EVIDENCE CONVERSATION GENERATION")
    println("="*80)

    // Load all personas
    val allPersons = Personas.loadPersonas(stage = "enriched_backgrounds").roles
    
    // Count existing evidence per person (skip for short runs)
    val existingEvidenceCounts = if (runShort) {
      Map.empty[String, Int] // Ignore existing evidence for short runs
    } else {
      countExistingEvidencePerPerson()
    }
    
    // Determine target use cases per person
    val targetUseCasesPerPerson = if (runShort) 20 else useCasesPerPersonConfig
    
    // Select people based on existing evidence counts
    val effectiveTargetPeopleCount = if (runShort) 3 else targetPeopleCount
    val personsToProcess = if (runShort) {
      // For short runs, just take the first 3 people
      allPersons.take(effectiveTargetPeopleCount)
    } else {
      selectPeopleByExistingEvidence(allPersons, effectiveTargetPeopleCount, existingEvidenceCounts)
    }
    
    // Calculate evidence items needed per person
    val evidenceNeededPerPerson = if (runShort) {
      // For short runs, always generate 20 items per person
      personsToProcess.map { person => (person, 20) }
    } else {
      personsToProcess.map { person =>
        val existing = existingEvidenceCounts.getOrElse(person.id, 0)
        val needed = Math.max(0, targetUseCasesPerPerson - existing)
        (person, needed)
      }.filter(_._2 > 0) // Only process people who need more evidence
    }
    
    if (evidenceNeededPerPerson.isEmpty && !runShort) {
      println(s"✅ All selected people already have ${targetUseCasesPerPerson} or more evidence items. Nothing to generate.")
      return
    }
    
    val totalUseCases = evidenceNeededPerPerson.map(_._2).sum
    
    if (runShort) {
      println(s"🚀 Running in SHORT mode: processing ${effectiveTargetPeopleCount} personas, 20 use cases each")
      println(s"📁 Output will be saved to: short_runs/... (not the default directory)")
    } else {
      // Print selection summary
      println(s"📊 Loaded ${allPersons.length} personas")
      println(s"📊 Processing ${evidenceNeededPerPerson.length} personas who need more evidence (target: ${targetUseCasesPerPerson} items each)")
      
      // Show existing evidence summary
      if (existingEvidenceCounts.nonEmpty) {
        println("\n📈 Existing evidence summary:")
        evidenceNeededPerPerson.take(5).foreach { case (person, needed) =>
          val existing = existingEvidenceCounts.getOrElse(person.id, 0)
          println(s"   - ${person.getPrimitiveRoleName}: ${existing} existing, ${needed} needed")
        }
        if (evidenceNeededPerPerson.length > 5) {
          println(s"   ... and ${evidenceNeededPerPerson.length - 5} more people")
        }
      }
    }

    val stats = GenerationStats.create(evidenceNeededPerPerson.length, totalUseCases, config.evidenceCount)

    println(f"\nGenerating evidence conversations (${config.evidenceCount} evidence each)...")

    // Start periodic stats reporting
    val statsReporter = stats.startPeriodicReporting(intervalSeconds = Config.Reporting.CONVERSATION_STATS_INTERVAL_SECONDS)

    try {
      // Separate the first few people to process for early validation
      val (firstBatch, remaining) = evidenceNeededPerPerson.splitAt(3)
      
      // Generate evidence for the first 3 people to catch issues faster
      Utils.parallelForeach(firstBatch, threadCount = Config.Threading.PERSON_PROCESSING_THREADS) { case (person, neededCount) =>
        if (Config.DEBUG) {
          println(s"🔄 Starting evidence generation for person: ${person.getPrimitiveRoleName} (need ${neededCount} items)")
        }
        generateEvidenceForPerson(person, stats, neededCount)
        
        // Check if we have any successes at all after first few people
        if (stats.peopleCompleted.get() >= 2 && stats.evidenceItemsCompleted.get() == 0) {
          throw new RuntimeException(s"🚨 No evidence generated after processing ${stats.peopleCompleted.get()} people. This indicates a systematic problem. Stopping to prevent waste of time.")
        }
      }
      
      // If first batch worked, continue with the rest in parallel
      if (remaining.nonEmpty) {
        println(s"✅ First batch processed successfully, continuing with remaining ${remaining.length} people in parallel...")
        Utils.parallelForeach(remaining, threadCount = Config.Threading.PERSON_PROCESSING_THREADS) { case (person, neededCount) =>
          generateEvidenceForPerson(person, stats, neededCount)
        }
      }
    } finally {
      // Final statistics
      println("\n" + "="*80)
      println("GENERATION COMPLETE")
      println("="*80)
      stats.printFinalStatsAndCancel(statsReporter)
    }
  }

  /**
   * Generate evidence conversations for a specific person.
   * Made public for debugging and testing individual phases.
   * 
   * @param person The person to generate evidence for
   * @param stats The generation statistics tracker
   * @param targetUseCases The number of use cases to generate for this person
   */
  def generateEvidenceForPerson(person: Personas.Person, stats: GenerationStats, targetUseCases: Int = -1): Unit = {
    // Track start time for timeout
    val startTime = System.currentTimeMillis()
    val timeoutMs = Config.Generation.PERSON_PROCESSING_TIMEOUT_HOURS * 3600 * 1000L
    
    // Create intermediate steps collector if logging is enabled
    val collector = if (Config.Evidence.LOG_INTERMEDIATE_STEPS) {
      Some(new IntermediateStepsCollector())
    } else None
    
    // Determine the actual number of use cases to generate
    val actualTargetUseCases = if (targetUseCases > 0) targetUseCases else useCasesPerPersonConfig
    
    // Generate use cases with scenario descriptions
    val useCases = generateUseCases(person, actualTargetUseCases)
    
    // Collect use cases if logging enabled
    collector.foreach(_.setUseCases(useCases))

    stats.useCasesCompleted.addAndGet(useCases.length)

    // Track if we've timed out
    val timedOut = new java.util.concurrent.atomic.AtomicBoolean(false)
    
    // Generate evidence conversations from use cases
    if (Config.DEBUG) {
      println(s"  Processing ${useCases.length} use cases for ${person.getPrimitiveRoleName}...")
    }
    val evidenceItems = Utils.parallelMap(useCases, threadCount = Config.Threading.USE_CASE_PROCESSING_THREADS) { useCase =>
      // Check for timeout before processing each use case
      if (Config.Generation.ENABLE_PERSON_TIMEOUT && 
          (System.currentTimeMillis() - startTime > timeoutMs || timedOut.get())) {
        timedOut.set(true)
        None // Skip remaining use cases
      } else {
        if (Config.DEBUG) {
          println(s"    Starting use case ${useCase.id} for ${person.getPrimitiveRoleName}")
        }
        generateEvidenceFromUseCase(person, useCase, stats, collector) match {
          case Some(evidenceItem) =>
            // Update stats for successful generation - evidenceItemsCompleted is updated inside generateEvidenceFromUseCase
            if (Config.DEBUG) {
              println(s"    ✅ Generated evidence for use case ${useCase.id} for ${person.getPrimitiveRoleName}")
            }
            Some(evidenceItem)
          case None =>
            // Skip difficult use case
            if (Config.DEBUG) {
              println(s"    ❌ Skipping difficult use case ${useCase.id} for ${person.getPrimitiveRoleName} - failed after all retries")
            }
            None
        }
      }
    }.flatten // Remove None values
    
    // Log timeout if it occurred
    if (timedOut.get()) {
      println(s"⏱️  Person ${person.getPrimitiveRoleName} timed out after ${Config.Generation.PERSON_PROCESSING_TIMEOUT_HOURS} hours")
      println(s"   Completed ${evidenceItems.length}/${useCases.length} use cases before timeout")
      stats.peopleTimedOut.incrementAndGet()
    }
    
    // Check if too many use cases failed - this indicates a systematic problem
    val failureRate = (useCases.length - evidenceItems.length).toDouble / useCases.length
    if (Config.DEBUG) {
      println(s"  Result for ${person.getPrimitiveRoleName}: ${evidenceItems.length}/${useCases.length} use cases succeeded (${((1-failureRate) * 100).toInt}% success rate)")
    }
    
    // Only throw errors if we didn't timeout (timeouts are expected behavior)
    if (!timedOut.get()) {
      if (evidenceItems.isEmpty && useCases.nonEmpty) {
        throw new RuntimeException(s"🚨 ALL ${useCases.length} use cases failed for ${person.getPrimitiveRoleName}. This indicates a systematic problem (API issues, schema problems, etc.). Check the error messages above.")
      } else if (failureRate > 0.96) {
        throw new RuntimeException(s"🚨 Critical failure rate for ${person.getPrimitiveRoleName}: ${(failureRate * 100).toInt}% of use cases failed (${useCases.length - evidenceItems.length}/${useCases.length}). This indicates a systematic problem.")
      } else if (failureRate > 0.7) {
        println(s"⚠️  WARNING: High failure rate for ${person.getPrimitiveRoleName}: ${(failureRate * 100).toInt}% of use cases failed (${useCases.length - evidenceItems.length}/${useCases.length})")
      }
    }

    // Log all intermediate steps if enabled
    collector.foreach { c =>
      IntermediateStepsLogger.logPersonAllSteps(person, c.getAllSteps(), getEvidenceTypeName)
    }
    
    // Save to file if we have any evidence items - use short_runs directory if running in short mode
    if (evidenceItems.nonEmpty) {
      val outputPath = if (runShort) {
        config.resourcePath.replace("src/main/resources", "short_runs")
      } else {
        config.resourcePath
      }
      EvidencePersistence.saveEvidenceToFile(person, evidenceItems, outputPath)
      stats.filesGenerated.incrementAndGet()
    }

    // Update stats for person completion
    stats.peopleCompleted.incrementAndGet()
  }

  /**
   * Get implementation-specific prompt parts for use case summary generation.
   */
  def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts

  /**
   * Get the summary prompt for generating use cases (implementation specific).
   * This method builds the complete prompt from the base structure and implementation-specific parts.
   * 
   * @param person The person to generate the prompt for
   * @param targetCount The number of use cases to generate (defaults to useCasesPerPersonConfig)
   */
  def getUseCaseSummaryPrompt(person: Personas.Person, targetCount: Int = -1): String = {
    val parts = getUseCaseSummaryPromptParts(person)
    val actualTargetCount = if (targetCount > 0) targetCount else useCasesPerPersonConfig
    EvidencePromptTemplates.buildUseCaseSummaryPrompt(person, parts, actualTargetCount)
  }

  /**
   * Generate use cases with scenario descriptions.
   * Made public for debugging and testing individual phases.
   * If we get fewer use cases than expected, we'll recursively generate more.
   * If we get more than expected, we'll just take what we need.
   * 
   * @param person The person to generate use cases for
   * @param targetCount The number of use cases to generate (defaults to useCasesPerPersonConfig)
   */
  def generateUseCases(person: Personas.Person, targetCount: Int = -1): List[EvidenceUseCase] = {
    val actualTargetCount = if (targetCount > 0) targetCount else useCasesPerPersonConfig
    generateUseCasesRecursive(person, Nil, actualTargetCount)
  }
  
  /**
   * Recursively generate use cases until we have enough.
   * Accumulates use cases across multiple generation attempts if needed.
   */
  def generateUseCasesRecursive(
    person: Personas.Person, 
    accumulatedUseCases: List[EvidenceUseCase],
    neededCount: Int
  ): List[EvidenceUseCase] = {
    // If we already have enough, return what we need
    if (accumulatedUseCases.length >= neededCount) {
      return accumulatedUseCases.take(neededCount)
    }
    
    val summaryPrompt = PROJECT_BACKGROUND + "\n" + getUseCaseSummaryPrompt(person, neededCount)
    
    val newUseCases = Utils.retry(Config.Evidence.USE_CASE_GENERATION_MAX_RETRIES) {
      val response = llmModel.generateContent(summaryPrompt).get
      
      // Parse the generated use cases using the unified format
      decode[EvidenceUseCases](response.content) match {
        case Right(payload) =>
          if (Config.DEBUG) {
            println(s"Generated ${payload.use_cases.length} use cases (expected: $neededCount)")
          }
          // Add model name to each use case
          payload.use_cases.map(_.copy(model_name = Some(response.modelName)))
          
        case Left(error) =>
          throw new RuntimeException(s"Failed to decode use cases JSON: $error")
      }
    }
    
    val combinedUseCases = accumulatedUseCases ++ newUseCases
    
    // If we still don't have enough, recursively generate more
    if (combinedUseCases.length < neededCount) {
      if (Config.DEBUG) {
        println(s"Generated ${newUseCases.length} use cases, total: ${combinedUseCases.length}. Need ${neededCount - combinedUseCases.length} more.")
      }
      generateUseCasesRecursive(person, combinedUseCases, neededCount)
    } else {
      // We have enough, take what we need
      val result = combinedUseCases.take(neededCount)
      
      // In runShort mode, only take first 2 use cases
      if (runShort) result.take(2) else result
    }
  }

  /**
   * Get implementation-specific prompt parts for evidence core generation.
   */
  def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts

  /**
   * Get the prompt for evidence core generation (step 1: question, answer, evidence messages).
   * This method builds the complete prompt from the base structure and implementation-specific parts.
   */
  def getEvidenceCorePrompt(person: Personas.Person, useCase: EvidenceUseCase): String = {
    val parts = getEvidenceCorePromptParts(person, useCase)
    val expectedSpeaker = getExpectedSpeaker()
    EvidencePromptTemplates.buildEvidenceCorePrompt(person, useCase, parts, config.evidenceCount, expectedSpeaker)
  }

  /**
   * Get conversation prompt parts for the scenario-specific conversation generation.
   */
  def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts

  /**
   * Get the evidence type name for logging purposes.
   */
  def getEvidenceTypeName: String
  
  /**
   * Define the verification checks to perform for this generator.
   * Override in subclasses to customize verification.
   * 
   * Default: verify with evidence and verify without evidence
   * Empty list: no verification
   * 
   * @return List of verification checks to perform
   */
  def getVerificationChecks(): List[VerificationCheck] = {
    VerificationConstants.STANDARD_CHECKS
  }
  
  /**
   * Get the answering evaluation strategy for this evidence type.
   * 
   * Different evidence types may have different evaluation needs:
   * - Factual evidence: Exact answer matching
   * - Rubric-based evidence: Evaluation against criteria
   * - Temporal evidence: Flexible matching
   * 
   * @return The answering evaluation strategy
   */
  def getAnsweringEvaluation(): AnsweringEvaluation = {
    DefaultAnsweringEvaluation
  }

  /**
   * Load evidence items specific to this generator type.
   * Each implementation loads its own evidence based on the config.
   */
  def loadEvidenceItems(): List[EvidenceItem] = {
    EvidencePersistence.loadEvidenceItems(config.resourcePath)
  }


  /**
   * Generate only evidence core for a use case (Phase 2).
   * Useful for debugging and testing the core generation independently.
   * Includes verification that all messages have the correct speaker.
   */
  def generateEvidenceCore(person: Personas.Person, useCase: EvidenceUseCase): GeneratedEvidenceCore = {
    val evidenceCorePrompt = getEvidenceCorePrompt(person, useCase)
    val (evidenceCore, modelName) = conversationGenerator.generateStructured[GeneratedEvidenceCore](evidenceCorePrompt)
    
    // Validate the core using CoreValidator
    val validationResult = CoreValidator.validateCore(evidenceCore, getEvidenceCount())
    if (!validationResult.isValid) {
      val errorDetails = validationResult.errors.mkString("\n  - ")
      throw new RuntimeException(
        s"Generated evidence core failed validation:\n  - $errorDetails"
      )
    }
    
    // Verify that all messages have valid speaker values
    val invalidSpeakers = evidenceCore.message_evidences.filterNot(msg => 
      msg.speaker == "User" || msg.speaker == "Assistant"
    )
    if (invalidSpeakers.nonEmpty) {
      throw new RuntimeException(
        s"Invalid speaker values found: ${invalidSpeakers.map(_.speaker).distinct.mkString(", ")}. " +
        "Only 'User' and 'Assistant' are valid speaker values."
      )
    }
    
    // Verify that all messages have the same speaker
    val speakers = evidenceCore.message_evidences.map(_.speaker).distinct
    if (speakers.length > 1) {
      throw new RuntimeException(
        s"Mixed speakers found in evidence messages: ${speakers.mkString(", ")}. " +
        "All messages must be from the same speaker (either all 'User' or all 'Assistant')."
      )
    }
    
    // Determine expected speaker based on evidence type
    val expectedSpeaker = getExpectedSpeaker()
    
    // Verify that messages are from the expected speaker
    if (speakers.nonEmpty && speakers.head != expectedSpeaker) {
      throw new RuntimeException(
        s"Incorrect speaker in evidence messages. Expected all messages to be from '$expectedSpeaker' " +
        s"but found messages from '${speakers.head}'. Check the evidence type requirements."
      )
    }
    
    evidenceCore.copy(model_name = Some(modelName))
  }
  
  /**
   * Get the expected speaker for this evidence type.
   * Override in subclasses that expect Assistant messages.
   * Default is "User" for most evidence types.
   */
  def getExpectedSpeaker(): String = "User"
  
  /**
   * Generate conversations from an evidence core (Phase 3).
   * Useful for debugging and testing conversation generation independently.
   */
  def generateConversationsFromCore(
    person: Personas.Person, 
    useCase: EvidenceUseCase, 
    evidenceCore: GeneratedEvidenceCore,
    stats: Option[GenerationStats] = None
  ): List[Conversation] = {
    val conversationPromptParts = getConversationPromptParts(person, useCase, evidenceCore)
    val (generatedConversations, modelName) = conversationGenerator.generateConversations(person, conversationPromptParts)

    // Track validation attempt
    stats.foreach(_.conversationValidationAttempts.incrementAndGet())

    // Validate that all evidence messages are present in conversations
    val validationResult = ConversationValidator.validateConversations(evidenceCore, generatedConversations.conversations)
    if (!validationResult.isValid) {
      // Track validation failure and categories
      stats.foreach { s =>
        s.conversationValidationFailures.incrementAndGet()
        s.trackValidationFailureCategories(validationResult.failureCategories)
      }
      
      val errorDetails = validationResult.errors.mkString("\n  - ")
      throw new RuntimeException(
        s"Generated conversations failed validation. Evidence messages not properly distributed:\n  - $errorDetails"
      )
    }

    // Add IDs, containsEvidence flag, and model_name to conversations
    generatedConversations.conversations.map { conversation =>
      conversation.copy(
        id = Some(UUID.randomUUID().toString),
        containsEvidence = Some(true),
        model_name = Some(modelName)
      )
    }
  }

  /**
   * Generate an evidence conversation from a use case using the two-step process.
   * Step 1: Generate evidence core (question, answer, evidence messages)
   * Step 2: Generate conversations based on evidence core and full context
   * Returns None if the use case is too difficult to generate after all retries.
   * 
   * This is the common implementation that was previously duplicated across all generators.
   */
  def generateEvidenceFromUseCase(
    person: Personas.Person, 
    useCase: EvidenceUseCase, 
    stats: GenerationStats,
    collector: Option[IntermediateStepsCollector] = None
  ): Option[EvidenceItem] = {
    val evidenceTypeName = getEvidenceTypeName
    var attemptCount = 0
    var coreGenerated = false
    var conversationsGenerated = false
    
    // Track start time for use case timeout
    val useCaseStartTime = System.currentTimeMillis()
    val useCaseTimeoutMs = Config.Generation.USE_CASE_TIMEOUT_MINUTES * 60 * 1000L
    
    try {
      Some(Utils.retry(Config.Evidence.EVIDENCE_GENERATION_MAX_RETRIES) {
      attemptCount += 1
      
      // Check for use case timeout before each retry
      if (Config.Generation.ENABLE_USE_CASE_TIMEOUT && 
          System.currentTimeMillis() - useCaseStartTime > useCaseTimeoutMs) {
        stats.useCasesTimedOut.incrementAndGet()
        throw new java.util.concurrent.TimeoutException(
          s"Use case ${useCase.id} timed out after ${Config.Generation.USE_CASE_TIMEOUT_MINUTES} minutes"
        )
      }
      
      // Step 1: Generate evidence core (question, answer, evidence messages)
      val evidenceCore = generateEvidenceCore(person, useCase)
      
      // Collect evidence core if logging enabled
      collector.foreach(_.addEvidenceCore(useCase.id, evidenceCore))
      
      // Track successful evidence core generation only on first generation
      if (!coreGenerated) {
        stats.evidenceCoresGenerated.incrementAndGet()
        coreGenerated = true
      }
      
      // Step 2: Generate conversations with its own retry loop
      val conversations = try {
        Utils.retry(Config.Evidence.CONVERSATION_GENERATION_MAX_RETRIES) {
          val convs = generateConversationsFromCore(person, useCase, evidenceCore, Some(stats))
          
          // Track successful conversation generation only on first generation
          if (!conversationsGenerated) {
            stats.conversationsGenerated.incrementAndGet()
            conversationsGenerated = true
          }
          
          convs
        }
      } catch {
        case e: Exception =>
          // If conversation generation fails after all retries, throw to trigger evidence retry
          throw new RuntimeException(s"Conversation generation failed after ${Config.Evidence.CONVERSATION_GENERATION_MAX_RETRIES} attempts: ${e.getMessage}", e)
      }
      
      // Create the complete EvidenceItem by combining both responses with usecase metadata
      val evidenceItem = EvidenceItem(
        question = evidenceCore.question,
        answer = evidenceCore.answer,
        message_evidences = evidenceCore.message_evidences,
        conversations = conversations,
        category = useCase.category,
        scenario_description = Some(useCase.scenario_description),
        personId = Some(person.id),
        use_case_model_name = useCase.model_name,
        core_model_name = evidenceCore.model_name
      )
      
      // Collect evidence item if logging enabled
      collector.foreach(_.addEvidenceItem(useCase.id, evidenceItem, attemptCount))
      

      // Get the verification checks and answering evaluation for this generator
      val verificationChecks = getVerificationChecks()
      val answeringEvaluation = getAnsweringEvaluation()
      
      // Execute all verification checks
      val compositeResult = VerificationExecutor.execute(
        evidenceItem, 
        verificationChecks, 
        Some(stats.verificationCheckStats),
        answeringEvaluation
      )
      val verificationResult = compositeResult.toVerificationResult
      
      // Track verification attempt
      stats.verificationAttempts.incrementAndGet()
      
      if (verificationResult.passed) {
        // Verification passed, return the evidence item
        stats.trackEvidenceItemModels(evidenceItem)
        stats.verificationPasses.incrementAndGet()
        stats.evidenceItemsCompleted.incrementAndGet()
        
        // Track retry statistics for successful evidence
        stats.totalRetryAttempts.addAndGet(attemptCount)  // Track total attempts
        if (attemptCount > 1) {
          stats.successfulEvidenceRetries.incrementAndGet()
        }
        
        if (attemptCount > 1 && Config.DEBUG) {
          println(s"    ✅ $evidenceTypeName evidence verified on attempt $attemptCount for ${person.getPrimitiveRoleName}")
        }
        evidenceItem
      } else {
        // Verification failed, retry with a different generation
        stats.verificationFailures.incrementAndGet()
        if (attemptCount > Config.Reporting.RETRY_LOGGING_THRESHOLD && Config.DEBUG) {
          val failureReason = verificationResult.failureReason.getOrElse(s"Expected: '${evidenceItem.answer}', Got: '${verificationResult.lastModelAnswer}'")
          println(s"    ❌ $evidenceTypeName evidence verification failed on attempt $attemptCount for ${person.getPrimitiveRoleName}. $failureReason")
        }
        throw new RuntimeException(s"Generated $evidenceTypeName evidence failed verification. ${verificationResult.failureReason.getOrElse(s"Expected: '${evidenceItem.answer}', Got: '${verificationResult.lastModelAnswer}'")}")
      }
      })
    } catch {
      case timeout: java.util.concurrent.TimeoutException =>
        // Timeout is expected behavior, log it and move on
        if (Config.DEBUG) {
          println(s"    ⏱️ Use case ${useCase.id} for ${person.getPrimitiveRoleName} timed out after ${Config.Generation.USE_CASE_TIMEOUT_MINUTES} minutes")
        }
        // Track abandoned core only if we generated one
        if (coreGenerated) {
          stats.abandonedEvidenceCores.incrementAndGet()
        }
        None
        
      case exception: Exception =>
        // Always log the first few attempts and high attempt counts
        if ((attemptCount <= 5 || attemptCount > Config.Reporting.RETRY_LOGGING_THRESHOLD) && Config.DEBUG) {
          println(s"    ❌ $evidenceTypeName evidence generation failed on attempt $attemptCount for ${person.getPrimitiveRoleName}: ${exception.getMessage}")
          if (attemptCount <= 5) {
            println(s"        Stack trace: ${exception.getStackTrace.take(3).mkString("; ")}")
          }
        }
        
        // For the first person and first few use cases, throw the exception to surface the problem immediately
        val isEarlyFailure = stats.peopleCompleted.get() == 0 && attemptCount <= 3
        if (isEarlyFailure) {
          throw new RuntimeException(s"🚨 Early failure in $evidenceTypeName evidence generation for first person ${person.getPrimitiveRoleName} on attempt $attemptCount: ${exception.getMessage}", exception)
        }
        
        // Use case is too difficult, skip it - track abandoned core only if we generated one
        if (coreGenerated) {
          stats.abandonedEvidenceCores.incrementAndGet()
        }
        None
    }
  }


}
