package com.salesforce.crmmembench.personas

import com.salesforce.crmmembench.Personas.Person

object Prompts {
  val INITIAL_ROLES_GENERATION =
    """
    You are an expert in AI, Customer Relationship Management (CRM), and benchmark creation. Your task is to generate a list of 50 distinct roles for a synthetic dataset to benchmark an AI assistant's long-term memory and reasoning capabilities within a CRM context, like Salesforce.

    **Background Context from Research (LongMemEval):**
    A robust benchmark for conversational AI assistants requires evaluating core memory abilities through realistic, long-term interactions. Key principles involve:
    1.  **Diverse Roles & Scenarios:** The data should reflect a wide range of real-world interactions, not just simple Q&A. This means including roles from marketing, sales, customer service, and internal operations.
    2.  **Information Synthesis:** The benchmark should test the AI's ability to connect information across multiple conversations and user roles (e.g., connecting a marketing campaign to a sales outcome).
    3.  **Realistic Interactions:** Conversations should be task-oriented, where users are trying to accomplish specific goals within the CRM, such as analyzing data, managing customer relationships, or resolving issues.
    4.  **Attribute Ontology:** The generated data should be based on a clear structure, or ontology, of user attributes, life events, and professional responsibilities.

    **Your Task:**
    Based on this context, generate a list of exactly 50 distinct roles. These roles should cover the entire customer lifecycle and internal enterprise operations. Organize them into the following six categories:

    1.  **Marketing & Lead Generation**
    2.  **Sales & Business Development**
    3.  **Customer & Prospect Personas**
    4.  **Customer Service & Technical Support**
    5.  **Customer Success & Post-Sales**
    6.  **Internal Operations & Executive Leadership**

    **Output Format:**
    Please provide the output in a single JSON object. The root should be a key named "roles" which contains an array of 50 role objects. Each object in the array must have the following three string fields:
    - `category`: The name of the category the role belongs to (must be one of the six listed above).
    - `role_name`: The specific title of the role.
    - `description`: A brief, one-sentence explanation of the role's primary function and how it relates to the CRM.

    Do not include any text or explanation outside of the JSON object.
    """

  def BACKGROUND_ENRICHMENT(person: Person) = {
    s"""
       |You are an expert AI assistant collaborating with a Generative AI Research Engineer at Salesforce. Your task is to generate a detailed user persona background for a new, comprehensive memory benchmark for Large Language Models (LLMs), similar in scope to LongMemEval.
       |
       |Project Context & Goal:
       |The primary goal is to create a rich, detailed persona background that will serve as the "ground truth" for a user's memory. This background should provide a rich, narrative foundation from which specific, verifiable facts ("evidence statements") can later be derived. The story itself should be compelling and internally consistent, forming a solid basis for the persona.
       |
       |Your Task:
       |Based on the high-level persona details provided below, generate a detailed, narrative-style background, approximately one page long. The goal is to create a believable character story, not just a list of facts.
       |
       |Persona Details:
       |
       |Role Name: ${person.role_name}
       |
       |Category: ${person.category}
       |
       |Primary Function: ${person.description}
       |
       |Instructions for Background Generation:
       |Generate a coherent narrative that seamlessly weaves together the following attributes. The details should be vivid and descriptive, allowing for concrete evidence statements to be easily created from the text in a later step.
       |
       |Education: Describe their educational journey. What field did they study, and what was the experience like? This should give a sense of their intellectual foundation and how it shaped their early career.
       |
       |Career Path & Previous Roles: Narrate their professional journey. What were their key roles and at what kind of companies? The story should provide a clear career trajectory, highlighting key transitions or formative experiences.
       |
       |Hobbies & Preferences: Illustrate their personal life through their hobbies and interests. Instead of just listing them, show them in action. For example, rather than stating they like to travel, you could describe a memorable trip and its impact on them. This will reveal their preferences naturally.
       |
       |Intelligence & Work Style: Define their professional persona through their actions and attitudes. Are they shown to be meticulous in a brief anecdote? Do they speak in a way that reveals a strategic mind? These character traits should emerge from the story.
       |
       |Approximate Age: Hint at their age through their life experiences and career stage, making it feel organic to the narrative rather than a stated fact.
       |
       |Personal Details & Events: Weave in significant personal events or details. A recent move, a new pet, a personal project—these should appear as natural parts of their life's story.
       |
       |Make the description realistic. You are not making marketing pitch, make it real. Not just good staff, but bad as well.
       |
       |The final output should be a single block of narrative text without any Markdown formatting. It should be a rich story that brings the persona to life, providing ample material to support our benchmark's data generation needs.
       |""".stripMargin
  }

}
