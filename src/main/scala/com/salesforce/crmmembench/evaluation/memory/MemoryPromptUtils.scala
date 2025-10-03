package com.salesforce.crmmembench.evaluation.memory

/**
 * Common prompting utilities for memory answerers.
 * 
 * This object provides shared prompt components that are truly common
 * across different memory implementations, particularly:
 * - Judge evaluation criteria that all memory systems should use
 * - Common prompt patterns for memory-based systems
 */
object MemoryPromptUtils {

  /**
   * Get the judge evaluation criteria as a string to include in prompts.
   * This ensures the model understands how its answer will be evaluated.
   */
  def getJudgeEvaluationCriteria: String = {
    """When answering, keep in mind that your response will be evaluated based on:

1. **Core Information Accuracy**: Your answer must contain all essential factual information that directly addresses the question.

2. **Completeness with Transparency**: 
   - Provide all information you have that is requested by the question
   - If the question has multiple parts, answer what you can and explicitly state which parts you cannot answer
   - For partial knowledge, use phrases like:
     * "I know [specific fact], but I don't have information about [missing part]"
     * "Based on the conversations, I can confirm [known fact], however [unknown aspect] was not discussed"
     * "I have information about [part A]: [details]. Regarding [part B], I don't have that information"

3. **Factual Correctness**: All information in your response must be accurate and align with the available information.

4. **Appropriate Synthesis**: When multiple pieces of relevant information exist, synthesize them appropriately.

5. **Clarity and Honesty**: 
   - Be direct and factual in your response
   - Clearly distinguish between what you know and what you don't know
   - Never guess or fabricate information to fill gaps
   - Avoid unnecessary elaboration unless specifically requested

Examples of good partial responses:
- "I can tell you that the meeting is on Tuesday at 3 PM, but I don't have information about the location."
- "The project budget is $50,000. I don't have details about the timeline or deliverables."
- "Based on our conversations, Sarah prefers email communication. I don't know her phone preferences."

If you don't have ANY relevant information to answer the question, clearly state "I don't know" or "I don't have any information about that."
If you have SOME relevant information, provide what you know and explicitly acknowledge what you don't know."""
  }

  /**
   * Build a prompt for answering questions based on retrieved memories.
   * Used by Mem0MemoryAnswerer and other retrieval-based systems.
   * 
   * @param question The question to answer
   * @param memories The retrieved memory snippets
   * @return The formatted prompt string
   */
  def buildMemoryBasedPrompt(question: String, memories: List[String]): String = {
    if (memories.isEmpty) {
      s"""You are an assistant helping to answer questions based on available information.

${getJudgeEvaluationCriteria}

No relevant memories were found for this question. If you cannot answer based on the available information, please say "I don't know."

Question: $question

Answer:"""
    } else {
      val memoriesContext = memories.zipWithIndex.map { case (memory, idx) =>
        s"${idx + 1}. $memory"
      }.mkString("\n")
      
      s"""You are an assistant helping to answer questions based on retrieved memories. Your task is to incorporate ALL relevant information from these memories to provide accurate and complete answers.

${getJudgeEvaluationCriteria}

The following memories have been retrieved as relevant to your question. Each memory may contain important facts that should be considered in your answer:

$memoriesContext

Based on ALL the information in the memories above, answer the following question. Ensure your response incorporates all relevant facts from the retrieved memories.

Question: $question

Answer:"""
    }
  }

}