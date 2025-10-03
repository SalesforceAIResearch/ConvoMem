package com.salesforce.crmmembench

object GeneralPrompts {
  val PROJECT_BACKGROUND =
    """Project Context: Generative AI Memory Benchmark
      |1. Project Overview & Goal:
      |I am a Generative AI Research Engineer at Salesforce. My current project is to develop a new, comprehensive memory benchmark for Large Language Models (LLMs). This benchmark will be comparable in scope and quality to the LongMemEval benchmark. The primary goal is to create a robust evaluation suite by generating high-quality, synthetic data, including detailed personas, user backgrounds, and multi-session conversations.
      |
      |2. Core Principles from LongMemEval:
      |The generated assets for this benchmark must be designed to test the five core long-term memory abilities identified in the LongMemEval paper. Every piece of synthetic data should be created with these evaluation tasks in mind:
      |
      |Information Extraction (IE): The ability to recall specific facts, details, and information provided by both the user and the assistant from within a long, noisy conversational history.
      |
      |Multi-Session Reasoning (MR): The capacity to synthesize and reason over information distributed across multiple, distinct chat sessions to answer complex questions.
      |
      |Knowledge Updates (KU): The ability to track, recognize, and correctly use the most recent information when a user's details or preferences change over time.
      |
      |Temporal Reasoning (TR): The awareness of time, including understanding explicit date/time mentions and reasoning about the relative order of events based on conversation timestamps.
      |
      |Abstention (ABS): The critical ability for the model to recognize when it does not have the information necessary to answer a question and to respond accordingly (e.g., "I don't know"), avoiding hallucination.
      |
      |3. Data Generation Framework:
      |All synthetic data generation should adhere to the following structure, inspired by the LongMemEval curation process:
      |
      |Personas & Backgrounds: Create detailed and coherent user personas based on a wide ontology of attributes (e.g., lifestyle, profession, demographics, personal events, belongings). These backgrounds form the "ground truth" for the user's memory.
      |
      |Evidence Statements: Deconstruct the persona's background into discrete, verifiable facts or "evidence statements." Each statement is a piece of information to be embedded in the dialogue.
      |
      |Evidence Sessions: Design task-oriented user-assistant dialogues where the user indirectly and naturally reveals an "evidence statement." The goal is not to state facts plainly, but to weave them into a realistic conversational context.
      |
      |History Compilation: Construct a long and coherent chat history for each persona. This "haystack" will be composed of the "needle"-like evidence sessions, interspersed with a larger number of non-conflicting, irrelevant chat sessions to simulate a realistic, long-term interaction.
      |
      |Question-Answer Pairs: For each persona and history, create targeted questions that test one or more of the five core memory abilities. The answers must be directly supported by the embedded evidence statements.
      |
      |4. My Role & Task Execution:
      |When I request the generation of an asset (e.g., "create a persona," "write a conversation about planning a trip"), you are to act as my assistant in this research project. You must use this context to ensure the output is directly usable for the benchmark. The generated content should be detailed, realistic, and structured to facilitate the testing of long-term memory in LLMs.
      |
      |""".stripMargin
}
