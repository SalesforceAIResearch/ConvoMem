package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.{EvidenceUseCase, GeneratedEvidenceCore, EvidenceItem}
import scala.collection.concurrent.TrieMap

/**
 * Thread-safe collector for intermediate steps during evidence generation.
 * Collects all phases of generation (use cases, cores, evidence items) for later logging.
 */
class IntermediateStepsCollector {
  // Thread-safe collections for intermediate results
  var useCases: List[EvidenceUseCase] = List.empty
  val evidenceCores = TrieMap[Int, GeneratedEvidenceCore]()
  val evidenceItems = TrieMap[Int, (EvidenceItem, Int)]() // useCase.id -> (item, attemptNumber)
  
  /**
   * Set generated use cases (Phase 1)
   */
  def setUseCases(cases: List[EvidenceUseCase]): Unit = synchronized {
    useCases = cases
  }
  
  /**
   * Add generated evidence core (Phase 2)
   */
  def addEvidenceCore(useCaseId: Int, core: GeneratedEvidenceCore): Unit = {
    evidenceCores(useCaseId) = core
  }
  
  /**
   * Add generated evidence item (Phase 3)
   */
  def addEvidenceItem(useCaseId: Int, item: EvidenceItem, attemptNumber: Int): Unit = {
    evidenceItems(useCaseId) = (item, attemptNumber)
  }
  
  /**
   * Get all collected steps as a structured object
   */
  def getAllSteps(): IntermediateSteps = {
    IntermediateSteps(
      useCases = useCases,
      evidenceCores = evidenceCores.toMap,
      evidenceItems = evidenceItems.mapValues { case (item, attempt) => (item, attempt) }.toMap
    )
  }
}

/**
 * Container for all intermediate steps
 */
case class IntermediateSteps(
  useCases: List[EvidenceUseCase],
  evidenceCores: Map[Int, GeneratedEvidenceCore],
  evidenceItems: Map[Int, (EvidenceItem, Int)]
)