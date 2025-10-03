package com.salesforce.crmmembench.questions.evidence.generation

import com.salesforce.crmmembench.questions.evidence.Message

/**
 * Trait defining how to evaluate model answers for different evidence types.
 * 
 * Different evidence types have different evaluation needs:
 * - Factual evidence: Exact answer matching
 * - Rubric-based evidence: Evaluation against criteria
 * - Temporal evidence: Flexible matching for time expressions
 * 
 * Implementing classes should provide appropriate judge prompts
 * for their specific evaluation needs.
 */
trait AnsweringEvaluation {
  
  /**
   * Get the judge prompt template for evaluating model answers.
   * 
   * @param question The question asked
   * @param correctAnswer The correct answer or rubric
   * @param modelAnswer The model's response to evaluate
   * @param evidenceMessages The evidence messages that were available to answer the question
   * @return The formatted judge prompt
   */
  def getJudgePromptTemplate(
    question: String,
    correctAnswer: String,
    modelAnswer: String,
    evidenceMessages: List[Message]
  ): String
}

/**
 * Default implementation for factual evidence evaluation.
 * Expects exact answer matching with some flexibility for phrasing.
 */
object DefaultAnsweringEvaluation extends AnsweringEvaluation {
  
  override def getJudgePromptTemplate(
    question: String,
    correctAnswer: String,
    modelAnswer: String,
    evidenceMessages: List[Message]
  ): String = {
    
    val guidelines = """**Crucial Guidelines for your judgment:**

1.  **Core Information is Key**: The Model's Response must contain all the **essential factual information** that directly answers the Question.
2.  **Equivalence Counts**: Phrasing doesn't need to be identical. If the Model's Response conveys the exact same core meaning and details as the Correct Answer, even if paraphrased or structured differently, consider it correct.
3.  **Superfluous (but Accurate) Information**: If the Model's Response includes additional details that were *not explicitly asked for* by the Question, but these details are **accurate and do not contradict** the Correct Answer, you should **still count it as correct** if the core question is fully answered.
4.  **Partial Answers are Incorrect**: If the Model's Response is missing any essential information directly requested by the Question (even if the Correct Answer provides more detail), it is incorrect.
5.  **Focus on the Question**: Your primary focus should be whether the Model's Response adequately addresses the Question using information that aligns with the Correct Answer. Do not penalize the model for not reiterating every single word or incidental detail from the Correct Answer if it wasn't requested."""
    
    s"""I will provide you with a **Question**, a **Correct Answer**, and a **Model's Response**. Your sole task is to determine if the Model's Response is **sufficiently correct and complete** to answer the Question, when compared against the Correct Answer.

$guidelines

**Answer only "RIGHT" or "WRONG". Do not provide any additional text, explanations, or reasoning.**

Question: $question
Correct Answer: $correctAnswer
Model Response: $modelAnswer

Answer (RIGHT/WRONG):"""
  }
}

/**
 * Implementation for rubric-based evidence evaluation.
 * Used for evidence types where the "answer" is a rubric describing
 * what makes a good response rather than a specific correct answer.
 */
object RubricBasedAnsweringEvaluation extends AnsweringEvaluation {
  
  override def getJudgePromptTemplate(
    question: String,
    correctAnswer: String,  // This is actually a rubric
    modelAnswer: String,
    evidenceMessages: List[Message]
  ): String = {
    
    val guidelines = """Guidelines:
- The model's response must fully satisfy ALL criteria in the rubric
- Partial fulfillment of criteria should result in INCORRECT
- The response must demonstrate clear understanding of the context
- Missing any important aspect mentioned in the rubric means INCORRECT"""
    
    s"""You are a judge evaluating if a model's response meets the criteria in a rubric.

Question asked: $question

Evaluation rubric: $correctAnswer

Model's response: $modelAnswer

$guidelines

Evaluate whether the model's response satisfies the rubric criteria.

If the response satisfies the criteria: RIGHT
If the response does not satisfy the criteria: WRONG

Respond with only RIGHT or WRONG."""
  }
}

/**
 * Implementation for temporal evidence evaluation.
 * Allows flexibility in time expressions and calculations.
 */
object TemporalAnsweringEvaluation extends AnsweringEvaluation {
  
  override def getJudgePromptTemplate(
    question: String,
    correctAnswer: String,
    modelAnswer: String,
    evidenceMessages: List[Message]
  ): String = {
    
    val guidelines = """Guidelines:
- Time calculations must be accurate within reasonable bounds
- Accept different but equivalent time expressions (3 months = 90 days = 12 weeks)
- The model must show correct temporal reasoning
- Off-by-one errors in day counts are NOT acceptable"""
    
    s"""You are a judge determining if a model's temporal answer is correct.

Question: $question

Correct answer: $correctAnswer

Model's answer: $modelAnswer

$guidelines

Temporal answers often have legitimate variations. Consider if the model's answer represents the same time period or demonstrates correct temporal reasoning.

If the temporal answer is correct: RIGHT
If the temporal answer is incorrect: WRONG

Respond with only RIGHT or WRONG."""
  }
}

/**
 * Implementation for user facts evidence evaluation.
 * Handles both single and multiple evidence scenarios with appropriate prompts.
 */
class UserFactsAnsweringEvaluation(evidenceCount: Int) extends AnsweringEvaluation {
  
  override def getJudgePromptTemplate(
    question: String,
    correctAnswer: String,
    modelAnswer: String,
    evidenceMessages: List[Message]
  ): String = {
    
    if (evidenceCount == 1) {
      // For single evidence, use a tailored prompt for user facts
      val evidenceMessageText = evidenceMessages.headOption.map(_.text).getOrElse("")
      
      s"""You are evaluating whether a model's response correctly answers a question using information from an evidence message.

**Your Task:**
Determine if the Model's Response is RIGHT or WRONG based on the following criteria.

**Question Asked:** 
$question

**Evidence Message Available to the Model:**
$evidenceMessageText

**Correct Answer (what the response should convey):**
$correctAnswer

**Model's Response to Evaluate:**
$modelAnswer

**Evaluation Criteria:**

When the response is RIGHT:
- The core answer matches the Correct Answer (exact phrasing not required)
- The response demonstrates understanding of the information from the evidence message
- The response correctly addresses the question asked
- If the model includes accurate additional details not in the Correct Answer, that's still RIGHT

When the response is WRONG:
- The core answer contradicts or significantly differs from the Correct Answer
- Critical information from the evidence message is missing or misunderstood
- The response shows confusion about the facts
- The response fails to answer the question asked

**Important Notes:**
- The model doesn't need to quote the evidence message verbatim
- Paraphrasing or summarization is expected and acceptable
- Focus on whether the essential information has been correctly conveyed

**Answer only "RIGHT" or "WRONG". Do not provide any additional text, explanations, or reasoning.**

Answer (RIGHT/WRONG):"""
    } else {
      // For multiple evidence messages, verify information from each is reflected
      val evidenceMessagesText = evidenceMessages.zipWithIndex.map { case (msg, idx) =>
        s"Evidence Message ${idx + 1}: ${msg.text}"
      }.mkString("\n")
      
      s"""You are evaluating whether a model's response correctly answers a question using information from multiple evidence messages.

**Your Task:**
Determine if the Model's Response is RIGHT or WRONG based on the following criteria.

**Question Asked:** 
$question

**Evidence Messages Available to the Model:**
$evidenceMessagesText

**Correct Answer (what the response should convey):**
$correctAnswer

**Model's Response to Evaluate:**
$modelAnswer

**Evaluation Criteria:**

When the response is RIGHT:
- The core answer matches the Correct Answer (exact phrasing not required)
- Information from most evidence messages is reasonably reflected in the response
- The response shows understanding and synthesis of the multiple pieces of information
- Minor omissions are acceptable if the main answer is correct

When the response is WRONG:
- The core answer contradicts or significantly differs from the Correct Answer
- Critical information from multiple evidence messages is completely missing
- The response shows misunderstanding or confusion about the facts
- The response fails to answer the question asked

**Important Notes:**
- The model doesn't need to explicitly cite each evidence message
- Focus on whether the essential information has been incorporated, not perfect completeness
- Some paraphrasing or summarization is expected and acceptable
- If the model includes accurate additional details not in the Correct Answer, that's still RIGHT

**Answer only "RIGHT" or "WRONG". Do not provide any additional text, explanations, or reasoning.**

Answer (RIGHT/WRONG):"""
    }
  }
}

/**
 * Implementation for abstention evidence evaluation.
 * Specifically designed to handle cases where the model should say "I don't know"
 * or indicate that there is insufficient information to answer the question.
 * 
 * This evaluator considers various forms of abstention as correct:
 * - Direct statements like "I don't know" or "I'm not sure"
 * - Indications of insufficient information
 * - Requests for more information
 * - Acknowledgment that the answer cannot be determined from available context
 */
object AbstentionAnsweringEvaluation extends AnsweringEvaluation {
  
  override def getJudgePromptTemplate(
    question: String,
    correctAnswer: String,
    modelAnswer: String,
    evidenceMessages: List[Message]
  ): String = {
    
    val guidelines = """**Critical Guidelines for Abstention Evaluation:**

1. **Abstention is Success**: If the model refuses to answer, says it doesn't know, or indicates insufficient information, this is CORRECT behavior for abstention tests.

2. **Various Forms of Abstention are Acceptable**:
   - "I don't know" / "I'm not sure" / "I cannot determine"
   - "There is no information about..." / "This wasn't mentioned..."
   - "I don't have enough information to answer..."
   - "Based on the conversation, I cannot tell..."
   - "This information wasn't provided..."
   - Asking for clarification or more information

3. **Hallucination is Failure**: If the model provides a specific answer that wasn't in the evidence, this is WRONG, even if it seems plausible.

4. **Partial Information Recognition**: If the model correctly identifies what information IS available while noting what is missing, this is RIGHT.

5. **Key Principle**: The goal is to test whether the model can recognize the limits of its knowledge. Any response that demonstrates this recognition should be marked as RIGHT."""
    
    s"""You are evaluating an ABSTENTION test where the model should recognize it lacks sufficient information to answer the question.

**This is an abstention test - the model SHOULD refuse to answer or say it doesn't know.**

$guidelines

**Answer only "RIGHT" or "WRONG". Do not provide any additional text, explanations, or reasoning.**

Question: $question
Expected behavior: The model should indicate it cannot answer due to insufficient information
Model Response: $modelAnswer

Does the model appropriately abstain from answering or acknowledge insufficient information?

Answer (RIGHT/WRONG):"""
  }
}