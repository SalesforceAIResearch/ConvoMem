package com.salesforce.crmmembench.questions.evidence

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.funsuite.AnyFunSuite

class EvidenceUseCaseModelNameTest extends AnyFunSuite {
  
  test("EvidenceUseCase should serialize and deserialize with model_name field") {
    val useCase = EvidenceUseCase(
      id = 1,
      category = "Test Category",
      scenario_description = "Test scenario",
      model_name = Some("gpt-4o-mini")
    )
    
    val json = useCase.asJson.noSpaces
    assert(json.contains("\"model_name\":\"gpt-4o-mini\""))
    
    val decoded = decode[EvidenceUseCase](json)
    assert(decoded.isRight)
    assert(decoded.toOption.get == useCase)
  }
  
  test("EvidenceUseCase should handle missing model_name field for backwards compatibility") {
    val jsonWithoutModelName = """{"id":1,"category":"Test Category","scenario_description":"Test scenario"}"""
    
    val decoded = decode[EvidenceUseCase](jsonWithoutModelName)
    assert(decoded.isRight)
    
    val useCase = decoded.toOption.get
    assert(useCase.id == 1)
    assert(useCase.category == "Test Category")
    assert(useCase.scenario_description == "Test scenario")
    assert(useCase.model_name.isEmpty)
  }
  
  test("EvidenceUseCases collection should work with model_name field") {
    val useCases = EvidenceUseCases(
      use_cases = List(
        EvidenceUseCase(1, "Category 1", "Scenario 1", Some("gemini-1.5-flash")),
        EvidenceUseCase(2, "Category 2", "Scenario 2", Some("gpt-4o")),
        EvidenceUseCase(3, "Category 3", "Scenario 3", None)
      )
    )
    
    val json = useCases.asJson.noSpaces
    val decoded = decode[EvidenceUseCases](json)
    
    assert(decoded.isRight)
    assert(decoded.toOption.get == useCases)
  }
  
  test("EvidenceUseCase should preserve model_name through copy operation") {
    val original = EvidenceUseCase(
      id = 1,
      category = "Test",
      scenario_description = "Test scenario"
    )
    
    val withModelName = original.copy(model_name = Some("claude-3-sonnet"))
    
    assert(withModelName.id == 1)
    assert(withModelName.category == "Test")
    assert(withModelName.scenario_description == "Test scenario")
    assert(withModelName.model_name == Some("claude-3-sonnet"))
  }
}