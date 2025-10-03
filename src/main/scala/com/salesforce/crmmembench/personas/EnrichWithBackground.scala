package com.salesforce.crmmembench.personas

import com.salesforce.crmmembench.GeneralPrompts.PROJECT_BACKGROUND
import com.salesforce.crmmembench.LLM_endpoints.ModelLists.EXPENSIVE_MODELS
import com.salesforce.crmmembench.personas.GeneratePersonas.RolesResponse
import com.salesforce.crmmembench.{Personas, Utils}

object EnrichWithBackground {
  def main(args: Array[String]): Unit = {
    val loadPersonas = Personas.loadPersonas()
    println(s"Loaded ${loadPersonas.roles.size} personas")
    val enrichedPersons = Utils.parallelMap(loadPersonas.roles, threadCount = 30) { person =>
      val response = EXPENSIVE_MODELS.generateContent(PROJECT_BACKGROUND + Prompts.BACKGROUND_ENRICHMENT(person)).get
      person.copy(background = Some(response.content))
    }
    Personas.savePersonasToFile(RolesResponse(enrichedPersons), stage = "enriched_backgrounds")
  }
}
