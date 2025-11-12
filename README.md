# ConvoMem Benchmark

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Dataset on HF](https://img.shields.io/badge/🤗%20Hugging%20Face-Dataset-yellow)](https://huggingface.co/datasets/Salesforce/ConvoMem)
[![Paper](https://img.shields.io/badge/Paper-arXiv-red)](https://arxiv.org/abs/XXXX.XXXXX)

A comprehensive benchmark for evaluating conversational memory in large language models, featuring **75,336 question-answer pairs** across six evidence categories. This benchmark enables systematic evaluation of how well AI assistants remember and utilize information from previous conversations.

## 📊 Dataset Overview

ConvoMem provides diverse test cases for evaluating conversational memory:

| Category | Description | Test Cases | Example |
|----------|-------------|------------|---------|
| **User Facts** | User states facts, later asks assistant to recall | 16,733 | User: "I have 3 kids named Emma, Josh, and Lily" → Q: "How many children do I have?" → A: "3" |
| **Assistant Facts** | Assistant provides info, user asks to recall it | 12,745 | Assistant: "The team meeting is scheduled for 3pm in Conference Room B" → Q: "When did you say the meeting was?" → A: "3pm" |
| **Changing Facts** | Information evolves over conversation | 18,323 | "My budget is $5k" → "Actually, increase it to $7k" → "Wait, make it $10k" → Q: "What's my current budget?" → A: "$10k" |
| **Abstention** | Questions where no answer exists | 14,910 | Conversation about project timeline, no phone mentioned → Q: "What's John's phone number?" → A: "This wasn't mentioned in our conversation" |
| **Preferences** | User preferences for recommendations | 5,079 | User: "I prefer Python, value simplicity, dislike Java" → Q: "What backend framework should I use?" → A: "FastAPI or Flask (Python-based, simple)" |
| **Implicit Connections** | Multi-hop reasoning across messages | 7,546 | "Sarah manages the Sales team" + "Sales team uses Salesforce" → Q: "What CRM does Sarah's team use?" → A: "Salesforce" |

### Multi-Message Evidence Distribution

The benchmark tests both concentrated and distributed information:
- **Single-message evidence (40%)**: Information in one message
- **Multi-message evidence (60%)**: Information spread across 2-6 messages

This tests whether systems can synthesize scattered information, not just retrieve single facts.

## 🚀 Quick Start

The benchmark evaluation runs through Gradle (Java 17+ required). Results are saved to `logs/[evidence_type]/[timestamp]/` with detailed metrics, costs, and per-question results.

```bash
# Set API keys
echo "GOOGLE_AI_API_KEY=your_gemini_key" > .env

# Run evaluation
./gradlew run --args="evaluate --type user_facts --memory long_context"
```

## 📁 Repository Structure

```
CRM_Mem_Bench/
├── src/
│   ├── main/
│   │   ├── scala/com/salesforce/crmmembench/
│   │   │   ├── conversations/          # Conversation generation and loading
│   │   │   │   ├── ConversationGenerator.scala
│   │   │   │   ├── ConversationLoader.scala
│   │   │   │   └── FillerConversationGenerator.scala
│   │   │   │
│   │   │   ├── evaluation/            # Evaluation framework
│   │   │   │   ├── Evaluator.scala    # Base evaluator trait
│   │   │   │   ├── MultithreadedEvaluator.scala
│   │   │   │   ├── TestCasesGenerator.scala
│   │   │   │   └── memory/            # Memory-specific evaluators
│   │   │   │       ├── Evaluate*UserFactsEvidence*.scala
│   │   │   │       ├── Evaluate*ChangingEvidence*.scala
│   │   │   │       └── Evaluate*PreferenceEvidence*.scala
│   │   │   │
│   │   │   ├── LLM_endpoints/         # LLM integrations
│   │   │   │   ├── LLMClient.scala    # Base LLM interface
│   │   │   │   ├── Gemini.scala       # Google Gemini models
│   │   │   │   ├── GPT.scala          # OpenAI GPT models
│   │   │   │   └── Claude.scala       # Anthropic Claude (not used)
│   │   │   │
│   │   │   ├── memory/                # Memory system implementations
│   │   │   │   ├── MemoryAnswerer.scala           # Base trait
│   │   │   │   ├── LongContextMemoryAnswerer.scala
│   │   │   │   ├── BlockExtractedContextAnswerer.scala
│   │   │   │   └── mem0/
│   │   │   │       └── Mem0MemoryAnswerer.scala
│   │   │   │
│   │   │   ├── personas/              # Persona management
│   │   │   │   └── PersonaManager.scala
│   │   │   │
│   │   │   ├── questions/             # Question/evidence generation
│   │   │   │   └── evidence/
│   │   │   │       ├── EvidenceGenerator.scala    # Base trait
│   │   │   │       ├── UserFactsEvidenceGenerator.scala
│   │   │   │       ├── ChangingEvidenceGenerator.scala
│   │   │   │       ├── PreferenceEvidenceGenerator.scala
│   │   │   │       └── generators/    # Specific implementations
│   │   │   │
│   │   │   └── test/                  # Test runners
│   │   │       ├── TestRunner.scala
│   │   │       └── TestUtils.scala
│   │   │
│   │   └── resources/
│   │       ├── personas/              # Generated persona files
│   │       ├── evidence/              # Generated evidence Q&A pairs
│   │       ├── conversations/         # Generated conversations
│   │       └── test_cases/            # Cached test cases (gitignored)
│   │
│   └── test/                          # Unit tests
│       └── scala/com/salesforce/crmmembench/
│           ├── conversations/*Test.scala
│           ├── evaluation/*Test.scala
│           └── questions/*Test.scala
│
├── logs/                              # Evaluation results (gitignored)
│   └── [evidence_type]/[timestamp]/
│       ├── results.json              # Detailed results
│       ├── summary.txt               # Accuracy by context size
│       └── costs.csv                 # Token usage and costs
│
├── build.gradle                      # Gradle build configuration
├── gradle.properties                  # Gradle settings
└── README.md                         # This file
```

## 📥 Accessing the Dataset

You can access the benchmark dataset in two ways:

1. **From Hugging Face**: Download the pre-generated dataset with 75,336 question-answer pairs at [huggingface.co/datasets/Salesforce/ConvoMem](https://huggingface.co/datasets/Salesforce/ConvoMem)

2. **From This Repository**: Generate or load the dataset using the Scala evaluation framework:

```scala
import com.salesforce.crmmembench.questions.evidence.generators.UserFactsEvidenceGenerator
import com.salesforce.crmmembench.evaluation.{ConversationLoader, TestCasesGenerator}

// Create evidence generator with 3 facts to track
val evidenceGenerator = new UserFactsEvidenceGenerator(3)

// Load evidence items from resources
val evidenceItems = evidenceGenerator.loadEvidenceItems()

// Load irrelevant conversations for mixing
val irrelevantConversations = ConversationLoader.loadIrrelevantConversations()

// Create test cases with standard mixing strategy
val testCasesGen = TestCasesGenerator.createStandard(evidenceGenerator)
val testCases = testCasesGen.generateTestCases()
```

## 💻 Understanding the Benchmark

### How It Works: The Complete Pipeline

The benchmark creates realistic memory challenges through a sophisticated multi-stage pipeline:

#### Stage 1: Persona Generation
**Purpose**: Create diverse, realistic user profiles to ensure variety in conversations
**Process**: Generate 100 personas spanning different roles (sales, marketing, engineering, etc.) with rich backgrounds including work history, communication style, and domain expertise
**Main Class**: `PersonaGeneration.scala` - Orchestrates persona creation and enrichment
**Validation**: Check for diversity across roles, ensure no duplicate names, verify each persona has complete biographical information

#### Stage 2: Persona Enrichment
**Purpose**: Add depth and nuance to make conversations authentic
**Process**: Each persona receives additional details like specific tool preferences, industry jargon, and personal quirks that make their conversations unique
**Main Class**: `PersonaEnricher.scala` - Adds biographical details and conversational traits
**Validation**: Verify enrichment details are consistent with base persona (e.g., a sales director shouldn't have engineering tools as preferences), ensure no contradictions in biographical details

#### Stage 3: Use Case Generation
**Purpose**: Define realistic scenarios where memory matters
**Process**: For each persona, generate 50-100 scenarios in a single LLM call. This batching is crucial for diversity - the model can see all previously generated scenarios within the batch and ensure each explores different aspects of the persona's life, avoiding repetition and maximizing coverage
**Main Classes**: `UseCaseGenerator.scala` - Creates scenario descriptions and contexts
**Validation**: Check that each scenario includes a clear question, verify scenarios are appropriate for the persona's role and background, ensure diversity across scenarios (no repetitive patterns)

#### Stage 4: Evidence Core Generation
**Purpose**: Create the actual question-answer pairs with supporting evidence
**Process**: Transform use cases into concrete Q&A pairs with evidence messages that must appear in conversations. Process each scenario individually for quality focus
**Main Classes**:
- `EvidenceGenerator.scala` - Base trait for all evidence generation
- `UserFactsEvidenceGenerator.scala` - Generates user-stated facts
- `ChangingEvidenceGenerator.scala` - Creates evolving information
**Validation**:
- **Answerability Test**: Multiple LLMs must correctly answer the question when given only evidence messages (2+ consecutive correct answers required)
- **Necessity Test**: Question must be unanswerable without the evidence
- **Multi-message Validation**: For distributed evidence, removing any single message must make the question unanswerable
- **Retry Logic**: Failed validations trigger regeneration (up to 20 attempts)

#### Stage 5: Conversation Generation
**Purpose**: Embed evidence naturally within realistic dialogue
**Process**: Generate 100-message conversations where evidence appears organically. The conversation flows naturally around the topic without making the evidence obvious
**Main Classes**:
- `ConversationGenerator.scala` - Creates natural dialogue flows
- `ConversationEmbedder.scala` - Places evidence within conversations
**Validation**:
- **Evidence Integrity**: Verify evidence messages appear exactly once in designated conversations using exact, fuzzy, and partial matching
- **Conversation Length**: Ensure each conversation has approximately 100 messages (50 turns)
- **Natural Flow**: Check that evidence doesn't appear artificially or out of context
- **Speaker Consistency**: Validate that User evidence comes from User, Assistant evidence from Assistant

#### Stage 6: Test Case Assembly
**Purpose**: Mix evidence conversations with irrelevant ones at varying scales
**Process**: Combine evidence-containing conversations with filler conversations to create test cases with 2 to 300 total conversations
**Main Classes**:
- `TestCasesGenerator.scala` - Orchestrates test case creation
- `StandardTestCasesGenerator.scala` - Default mixing strategy
- `ConversationLoader.scala` - Loads filler conversations
**Validation**:
- **Coverage Check**: Ensure all required context sizes are generated (2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)
- **Distribution Verification**: Confirm evidence conversations are properly distributed within the mix
- **No Duplication**: Verify no conversation appears multiple times in the same test case
- **Filler Quality**: Ensure filler conversations don't accidentally contain answers to test questions

#### Stage 7: Evaluation
**Purpose**: Test memory systems and measure performance
**Process**: Load conversations into memory systems, ask questions, measure accuracy/cost/latency
**Main Classes**:
- `MultithreadedEvaluator.scala` - Runs parallel evaluations
- `MemoryAnswerer.scala` - Interface for memory systems
- `StatsReporter.scala` - Generates performance reports

### The Challenge

As conversations accumulate, finding relevant information becomes increasingly difficult:
- At 10 conversations: ~1,000 messages to search through
- At 50 conversations: ~5,000 messages
- At 300 conversations: ~30,000 messages

The benchmark measures how accuracy degrades as this "haystack" grows larger.

### Why Different Evidence Types Matter

Each evidence type tests a different cognitive capability:

- **User Facts**: Can the system remember what users explicitly stated?
- **Assistant Facts**: Does it track its own previous statements?
- **Changing Facts**: Can it handle information that evolves over time?
- **Abstention**: Does it avoid making up answers when information is missing?
- **Preferences**: Can it apply learned preferences to new situations?
- **Implicit Connections**: Can it connect information from different conversations?

### Implementing Your Memory System

To integrate your own memory system, implement the `MemoryAnswerer` trait. Your system needs to:

1. **Store conversations** as they're added
2. **Answer questions** based on stored history
3. **Track which conversations** were used for each answer

The framework handles the rest: loading test cases, measuring accuracy, tracking costs, and generating reports.

### Running Evaluations

The repository includes pre-built evaluators for each evidence type. These can be run directly from IntelliJ IDEA or via Gradle. Each evaluator:

- Tests a specific evidence type and count
- Runs across multiple context sizes (2 to 300 conversations)
- Outputs detailed results including accuracy, costs, and latency
- Saves results to `logs/` for analysis

Evidence counts range from 1 (single fact) to 6 (multiple facts to track), testing increasingly complex memory requirements.

## 🏗️ Architecture Overview

### Test Case Generation

The benchmark uses a modular architecture for creating test cases:

1. **Evidence Generators** - Create question-answer pairs with conversation context
   - `UserFactsEvidenceGenerator` - Facts stated by users
   - `ChangingEvidenceGenerator` - Information that evolves over time
   - `PreferenceEvidenceGenerator` - User preferences and recommendations
   - Each generator can be configured with different evidence counts (1-6 items to track)

2. **Test Case Generators** - Mix evidence with irrelevant conversations
   - `StandardTestCasesGenerator` - Tests each evidence item individually with varying context sizes
   - `BatchedTestCasesGenerator` - Groups multiple evidence items for efficiency
   - Takes evidence items and creates realistic test scenarios by mixing with filler conversations

3. **Memory Answerers** - Different approaches to answering questions
   - `LongContextMemoryAnswerer` - Puts entire conversation history in context
   - `BlockExtractedContextAnswerer` - Processes conversations in blocks, extracts relevant info
   - `Mem0MemoryAnswerer` - Uses external RAG-based memory service

### Evaluation Flow

1. **Generate Evidence** → Create Q&A pairs with supporting conversations
2. **Mix Test Cases** → Combine evidence with irrelevant conversations at different scales
3. **Run Evaluation** → Test memory systems on mixed conversations
4. **Measure Performance** → Track accuracy, cost, and latency across context sizes


## 🔧 Configuration

Edit `src/main/scala/com/salesforce/crmmembench/Config.scala`:

```scala
object Config {
  val DEBUG = false  // Verbose logging
  val CONTEXT_SIZES = List(2, 4, 6, 10, 20, 30, 50, 70, 100, 150, 200, 300)
  val EVALUATION_THREADS = 40
  val USE_CACHED_TEST_CASES = true  // Use pre-generated test cases
}
```

## 📈 Evaluation Metrics

The benchmark supports multiple evaluation metrics:

- **Exact Match**: For factual questions (User/Assistant/Changing facts)
- **Semantic Match**: For preference and implicit connection questions
- **Abstention Accuracy**: Correctly saying "I don't know"
- **Multi-Evidence Recall**: Percentage of evidence items correctly recalled

## 🤝 Contributing

We welcome contributions! Areas of interest include:

- Additional evidence categories
- Multi-modal memory (images, audio)
- Cross-lingual memory evaluation
- Longer conversation histories (>300)
- New mixing strategies
- Additional personas and domains

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📝 Citation

```bibtex
@article{convomem2025,
  title={ConvoMem: A Comprehensive Benchmark for Conversational Memory},
  author={[Authors]},
  journal={arXiv preprint arXiv:XXXX.XXXXX},
  year={2025}
}
```

## 📄 License

Apache License 2.0 - see [LICENSE](LICENSE) file.

## 🔗 Resources

- **Paper**: [arXiv:XXXX.XXXXX](https://arxiv.org/abs/XXXX.XXXXX)
- **Dataset**: [Hugging Face](https://huggingface.co/datasets/Salesforce/ConvoMem)
- **Documentation**: [Full API Docs](https://salesforce.github.io/CRM_Mem_Bench)
- **Issues**: [GitHub Issues](https://github.com/salesforce/CRM_Mem_Bench/issues)

---

<p align="center">
  <i>Built by Salesforce AI Research</i>
</p>