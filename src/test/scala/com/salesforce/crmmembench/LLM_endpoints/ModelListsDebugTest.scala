package com.salesforce.crmmembench.LLM_endpoints

object ModelListsDebugTest {
  def main(args: Array[String]): Unit = {
    println("Expensive models: " + ModelLists.EXPENSIVE_MODELS.getModelName)
    println("Cheap models: " + ModelLists.cheapModels.getModelName)
  }
}