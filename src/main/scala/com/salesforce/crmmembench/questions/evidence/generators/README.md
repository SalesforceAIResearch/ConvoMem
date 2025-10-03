# Evidence Generator Development Guide

This guide explains how to develop new Evidence Generators for the CRM Memory Benchmark system.

## Overview

Evidence Generators create test cases that evaluate a model's ability to remember and recall information from conversations. Each generator follows a three-phase process:

1. **Use Case Generation**: Create scenarios that describe what information needs to be embedded
2. **Evidence Core Generation**: Generate the question, answer, and evidence messages
3. **Conversation Generation**: Embed the evidence naturally in full conversations

**CRITICAL: Diversity is Essential**
The quality of your benchmark depends on having diverse test cases. This guide emphasizes generating and validating at least 20 use cases to ensure proper diversity before proceeding with production runs.

## Development Workflow

### Step 1: Understand the Evidence Pattern

Before creating a generator, determine what type of evidence you're testing. You can either use an existing pattern or create a new one:

**Existing Evidence Patterns:**
- **User Facts**: User states facts, later asks to recall them
- **Assistant Facts**: Assistant states facts, user asks to recall
- **Changing Evidence**: Information changes over time, test latest state
- **Abstention**: No answer exists, model should say "I don't know"
- **Preference**: User preferences for recommendations (uses rubrics)

**Creating a New Pattern:**
If your use case doesn't fit the existing patterns, you can create a new one. For example:
- **Implicit Connection**: Questions that require connecting unstated dots (e.g., "what should I do this weekend?" when earlier evidence mentioned a broken leg)
- **Contextual Inference**: Questions requiring understanding of context beyond explicit facts
- **Temporal Reasoning**: Questions about scheduling or timing based on mentioned constraints

When creating a new pattern, you may need to:
1. Define a new `EvidenceConfig` if the existing ones don't fit your needs
2. Consider whether standard verification (exact answer matching) or rubric verification (guidance-based) is more appropriate
3. Create clear guidelines for what makes valid evidence for your pattern

### Step 2: Create Your Generator Class

Create a new generator by extending `EvidenceGenerator`:

```scala
package com.salesforce.crmmembench.questions.evidence.generators

class MyEvidenceGenerator(evidenceCount: Int) 
  extends EvidenceGenerator(MyEvidenceConfig(evidenceCount)) {
  
  // Define the evidence type name for logging
  def getEvidenceTypeName: String = "my_evidence"
  
  // Define prompt parts for each phase...
}
```

### Step 3: Iterative Development Process

**IMPORTANT**: This is an iterative process. For each phase, you will:
1. Generate output
2. Inspect the results
3. If not satisfactory, modify your prompts
4. Regenerate and inspect again
5. Repeat until the output meets your quality standards

Use the provided utilities to develop your generator incrementally:

#### 3.1: Test Use Case Generation

**IMPORTANT: Always generate at least 20 use cases for diversity testing**

```scala
// In QuickGeneratorTest or similar utility
val generator = new MyEvidenceGenerator(1)
val person = getTestPerson()
// Generate 20 use cases to properly assess diversity
val useCases = generator.generateUseCases(person).take(20)

// Inspect ALL generated use cases for diversity
useCases.zipWithIndex.foreach { case (uc, i) =>
  println(s"\n${i+1}. Category: ${uc.category}")
  println(s"   Scenario: ${uc.scenario_description}")
}
```

**What to look for (DIVERSITY CHECKLIST):**
- Are the scenarios relevant to the person's role?
- Do they clearly describe what needs to be tested?
- **Is there good variety across all 20 use cases?**
- **Do scenarios cover different aspects of the person's life (professional/personal)?**
- **Are there different types of temporal patterns (if applicable)?**
- **Do scenarios avoid repetitive patterns or themes?**
- **Is the language varied and natural?**

**Diversity Requirements:**
- At least 20 use cases must be generated for testing
- Verify no more than 2-3 use cases follow the same pattern
- Ensure both professional and personal contexts are represented
- Check for variety in complexity and scope

**If results are not good:**
1. Go back to your `getUseCaseSummaryPromptParts` method
2. Adjust the `evidenceTypeDescription` to be clearer
3. Refine `scenarioGuidelines` to be more specific
4. Add or improve `exampleUseCases` to guide the model
5. Run the test again and check if output improved
6. Repeat until satisfied

#### 3.2: Test Evidence Core Generation

```scala
val useCase = useCases.head
val core = generator.generateEvidenceCore(person, useCase)

println(s"Question: ${core.question}")
println(s"Answer: ${core.answer}")
core.message_evidences.foreach { msg =>
  println(s"[${msg.speaker}]: ${msg.text}")
}
```

**What to look for:**
- Is the question clear and answerable?
- Does the answer match what's in the evidence messages?
- Are the evidence messages natural and contextual?

**If results are not good:**
1. Go back to your `getEvidenceCorePromptParts` method
2. Clarify `questionGuidelines` if questions are unclear
3. Improve `answerGuidelines` if answers don't match evidence
4. Refine `evidenceGuidelines` if messages seem unnatural
5. Update the `exampleCore` to better demonstrate what you want
6. Run the test again and iterate

#### 3.3: Test Conversation Generation

```scala
val conversations = generator.generateConversationsFromCore(person, useCase, core)
conversations.foreach { conv =>
  println(s"Conversation has ${conv.messages.length} messages")
  // Inspect first few messages
}
```

**What to look for:**
- Is the evidence embedded naturally?
- Does the conversation flow well?
- Is it 80-120 messages long?

**If results are not good:**
1. Go back to your `getConversationPromptParts` method
2. Adjust the `scenarioDescription` if needed
3. Consider if the `evidenceMessages` need different context
4. Run the test again and iterate

**Pro tip**: Keep a log of what prompt changes led to what improvements. This helps you understand what works and speeds up future generator development.

### Step 4: End-to-End Testing with Short Runs

Once individual phases work well, **YOU MUST** test the full generator with short runs.

**IMPORTANT**: Short runs now generate 20 use cases per person to ensure diversity validation.

1. Create short runners for evidence counts 1 and 2 (for common sense checking):

```scala
package com.salesforce.crmmembench.questions.evidence.runners.short

object GenerateMyEvidence1Short {
  def main(args: Array[String]): Unit = {
    val generator = new MyEvidenceGenerator(1) {
      override protected val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}

object GenerateMyEvidence2Short {
  def main(args: Array[String]): Unit = {
    val generator = new MyEvidenceGenerator(2) {
      override protected val runShort: Boolean = true
    }
    generator.generateEvidence()
  }
}
```

2. **ACTUALLY RUN THEM** (this is critical!):
   ```bash
   ./gradlew run -PmainClass=...GenerateMyEvidence1Short
   ```

3. **INSPECT THE OUTPUT** in `short_runs/questions/evidence/my_evidence/1_evidence/`
   - Open the generated JSON files
   - Verify the evidence makes sense
   - Check that conversations flow naturally
   - Ensure the rubric/answer is appropriate

**⚠️ IMPORTANT**: You MUST run these short runners before proceeding. This is not optional. The whole point of creating short runners is to quickly validate your generator works end-to-end.

Short runs will:
- Process only 3 personas (instead of 100)
- **Generate 20 use cases per person for diversity validation**
- Complete in ~30 minutes instead of hours
- Write to `short_runs/` instead of `src/main/resources/`

**CRITICAL: Diversity Validation for Short Runs**
After running short runners, you MUST:
1. Open the generated JSON files in `short_runs/`
2. For each person, verify all 20 use cases show diversity:
   - Different scenario types and contexts
   - Varied complexity levels
   - No repetitive patterns
   - Good mix of professional/personal contexts
3. If diversity is insufficient, modify your prompts and regenerate
4. Document any patterns you notice for improvement

### Step 5: Create Production Runners

**ONLY AFTER** verifying your generator works correctly with short runs, create production runners in the `runners` package:

```scala
package com.salesforce.crmmembench.questions.evidence.runners

// For generators with evidenceCount parameter, create runners for 1-6
object GenerateMyEvidence1 {
  def main(args: Array[String]): Unit = {
    new MyEvidenceGenerator(1).generateEvidence()
  }
}

object GenerateMyEvidence2 {
  def main(args: Array[String]): Unit = {
    new MyEvidenceGenerator(2).generateEvidence()
  }
}

// ... up to 6
```

## Prompt Development Tips

### Use Case Generation Prompts

```scala
def getUseCaseSummaryPromptParts(person: Personas.Person): UseCaseSummaryPromptParts = {
  UseCaseSummaryPromptParts(
    evidenceTypeDescription = "situations where users share facts about themselves",
    scenarioGuidelines = """Create scenarios where:
      - The user naturally shares personal information
      - The facts are specific and memorable
      - Later recalling these facts would be useful""",
    exampleUseCases = Some(List(
      EvidenceUseCase(1, "Work Preferences", "User mentions their favorite project management tool"),
      EvidenceUseCase(2, "Personal Details", "User shares their dietary restrictions")
    ))
  )
}
```

### Evidence Core Generation Prompts

```scala
def getEvidenceCorePromptParts(person: Personas.Person, useCase: EvidenceUseCase): EvidenceCorePromptParts = {
  EvidenceCorePromptParts(
    evidenceTypeDescription = "user-stated facts that should be remembered",
    questionGuidelines = "Ask directly about the fact the user shared",
    answerGuidelines = "The answer should be exactly what the user stated",
    evidenceGuidelines = "User should state the fact clearly and naturally",
    exampleCore = Some(GeneratedEvidenceCore(
      question = "What's my favorite project management tool?",
      answer = "Asana",
      message_evidences = List(
        Message("User", "I've been using Asana for years - it's definitely my favorite project management tool.")
      )
    ))
  )
}
```

### Conversation Generation Prompts

```scala
def getConversationPromptParts(person: Personas.Person, useCase: EvidenceUseCase, evidenceCore: GeneratedEvidenceCore): ConversationPromptParts = {
  ConversationPromptParts(
    evidenceType = "user facts memory test",
    scenarioDescription = "Testing if the AI remembers facts the user shares",
    useCaseScenario = Some(useCase.scenario_description),
    evidenceMessages = evidenceCore.message_evidences,
    question = evidenceCore.question,
    answer = evidenceCore.answer,
    evidenceCount = 1
  )
}
```

## Verification System

The verification system uses composable checks that you can mix and match:

```scala
override protected def getVerificationChecks(): List[VerificationCheck] = {
  // Default: verify with evidence + verify without evidence
  super.getVerificationChecks()
  
  // No verification (e.g., temporal evidence, conversations)
  List.empty
  
  // Add partial evidence check for multi-fact scenarios
  super.getVerificationChecks() ++ List(new VerifyWithPartialEvidence())
  
  // Custom combination
  List(
    new VerifyWithEvidence(requiredPasses = 5),
    new VerifyWithoutEvidence(),
    new VerifyWithPartialEvidence()
  )
}
```

**Available Verification Checks:**
- `VerifyWithEvidence`: Model must answer correctly with evidence
- `VerifyWithoutEvidence`: Model must fail without evidence
- `VerifyWithPartialEvidence`: Removing any conversation prevents correct answer

**When to skip verification (empty list):**
- Temporal evidence (legitimate answer variations)
- Conversation generation (no facts to verify)
- Any evidence type where strict matching is inappropriate

## Available Utilities

### QuickGeneratorTest
- `use-case`: Test only use case generation
- `core`: Test only evidence core generation  
- `conversation`: Test only conversation generation
- `demo`: Run all phases with a simple example

### IterativeGeneratorDevelopment
- `testUseCaseGeneration()`: Test use cases with detailed output
- `testEvidenceCoreGeneration()`: Test cores with inspection
- `testConversationGeneration()`: Test conversation embedding
- `testFullPipeline()`: Run all phases with pause points

### TestUserFactsGenerator
Interactive tool for testing generators with a menu system.

## Common Issues and Solutions

### Issue: Use cases are too generic
**Solution**: Add more specific scenario guidelines and examples

### Issue: Evidence messages sound unnatural
**Solution**: Review the evidence guidelines and ensure they encourage natural dialogue

### Issue: Conversations are too short/long
**Solution**: The system enforces 80-120 messages, but check if evidence is properly distributed

### Issue: Verification fails frequently
**Solution**: Ensure question and answer match exactly what's in the evidence messages

## Checklist for New Generators

- [ ] Generator class extends EvidenceGenerator
- [ ] All three prompt methods implemented
- [ ] Evidence type name defined
- [ ] Verification strategy selected
- [ ] **Use case generation tested with 20+ examples and verified for diversity**
- [ ] Evidence cores tested and are accurate
- [ ] Conversations tested and flow naturally
- [ ] Short runner created and tested end-to-end
- [ ] **Output inspected in short_runs directory - all 20 use cases per person checked for diversity**
- [ ] **Diversity issues addressed by modifying prompts if needed**
- [ ] Production runners created (1-6 if applicable)
- [ ] Config class created if needed

## Diversity Validation Guide

### Why Diversity Matters
A benchmark is only as good as its test cases. Without diversity, you're testing the same capability repeatedly rather than comprehensively evaluating the model's abilities.

### How to Validate Diversity

1. **During Development (QuickGeneratorTest)**:
   - Always generate at least 20 use cases
   - Read through ALL of them
   - Look for patterns and repetition
   
2. **Diversity Checklist**:
   - [ ] Topics vary (not all about the same subject)
   - [ ] Complexity varies (simple to complex scenarios)
   - [ ] Context varies (work, home, hobbies, relationships)
   - [ ] Time scales vary (if temporal: days, weeks, months)
   - [ ] Question types vary (if applicable)
   - [ ] Language patterns vary (not formulaic)

3. **Red Flags**:
   - More than 3 use cases follow identical patterns
   - All scenarios focus on one aspect of life
   - Repetitive phrasing or structure
   - Limited vocabulary or expressions

4. **Fixing Diversity Issues**:
   - Add more specific examples in your prompts
   - Include explicit diversity requirements
   - Use categories or types to force variety
   - Add "avoid repetition" instructions

### Example Diversity Analysis

```
BAD - Lacks diversity:
1. User shares their favorite coffee brand
2. User mentions their preferred tea type  
3. User discusses their favorite juice
4. User talks about their preferred soda
(All beverages - too narrow!)

GOOD - Has diversity:
1. User shares their favorite project management tool
2. User mentions their child's food allergy
3. User discusses their marathon training schedule
4. User reveals their budget constraints for a project
(Different contexts, types of information, complexity)
```

## Example: Complete Generator

See `UserFactsEvidenceGenerator` for a complete example that demonstrates all concepts in this guide.