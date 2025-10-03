package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.evaluation.generators._

/**
 * Test the redesigned Locomo generator implementation.
 */
object TestLocomoRedesign extends App {
  
  println("="*80)
  println("TESTING REDESIGNED LOCOMO GENERATORS")
  println("="*80)
  
  // Expected counts for validation
  val expectedCounts = Map(
    LocomoCategories.BasicFacts -> 282,
    LocomoCategories.Temporal -> 321,
    LocomoCategories.Reasoning -> 96,
    LocomoCategories.MultiSession -> 841,
    LocomoCategories.Abstention -> 446
  )
  
  println("\n📊 TESTING SINGLETON GENERATORS:")
  println("-" * 60)
  
  // Test each singleton generator
  val singletonGenerators = List(
    ("BasicFacts", LocomoBasicFactsGenerator),
    ("Temporal", LocomoTemporalGenerator),
    ("Reasoning", LocomoReasoningGenerator),
    ("MultiSession", LocomoMultiSessionGenerator),
    ("Abstention", LocomoAbstentionGenerator)
  )
  
  var totalQuestions = 0
  var allPassed = true
  
  for ((name, generator) <- singletonGenerators) {
    println(s"\n📌 Testing $name singleton generator:")
    println(s"   Category ID: ${generator.category.id}")
    println(s"   Category Name: ${generator.category.name}")
    println(s"   Description: ${generator.category.description}")
    
    try {
      val testCases = generator.generateTestCases()
      val questionCount = testCases.flatMap(_.evidenceItems).size
      val expected = expectedCounts(generator.category)
      
      println(s"   Loaded: ${testCases.size} test cases, $questionCount questions")
      println(s"   Expected: $expected questions")
      
      if (questionCount == expected) {
        println(s"   ✅ PASS")
      } else {
        println(s"   ❌ FAIL - Count mismatch!")
        allPassed = false
      }
      
      totalQuestions += questionCount
      
      // Verify generator type for results path
      println(s"   Generator type: ${generator.generatorType}")
      println(s"   Generator class type (for results): ${generator.generatorClassType}")
      
    } catch {
      case e: Exception =>
        println(s"   ❌ ERROR: ${e.getMessage}")
        allPassed = false
    }
  }
  
  println("\n" + "="*80)
  println("TESTING FACTORY METHODS")
  println("="*80)
  
  // Test factory apply method
  println("\n📌 Testing LocomoTestCasesGenerator.apply:")
  val factoryGen = LocomoTestCasesGenerator(LocomoCategories.BasicFacts)
  val factoryTestCases = factoryGen.generateTestCases()
  println(s"   Factory generator loaded: ${factoryTestCases.flatMap(_.evidenceItems).size} questions")
  
  // Test partial dataset loading
  println("\n📌 Testing partial dataset loading:")
  val partialGen = LocomoTestCasesGenerator(LocomoCategories.Temporal, List(0, 1, 2))
  val partialTestCases = partialGen.generateTestCases()
  println(s"   Partial generator loaded: ${partialTestCases.size} test cases")
  println(s"   Total questions: ${partialTestCases.flatMap(_.evidenceItems).size}")
  
  // Test category lookup methods
  println("\n" + "="*80)
  println("TESTING CATEGORY LOOKUPS")
  println("="*80)
  
  println("\n📌 Testing fromNumber:")
  for (num <- 1 to 6) {
    val catOpt = LocomoCategories.fromNumber(num)
    catOpt match {
      case Some(cat) => println(s"   $num -> ${cat.id} ✅")
      case None => println(s"   $num -> None ✅")
    }
  }
  
  println("\n📌 Testing fromId:")
  val testIds = List("locomo_basic_facts", "locomo_temporal", "invalid_id")
  for (id <- testIds) {
    val catOpt = LocomoCategories.fromId(id)
    catOpt match {
      case Some(cat) => println(s"   '$id' -> ${cat.name} ✅")
      case None => println(s"   '$id' -> None ✅")
    }
  }
  
  // Test all categories list
  println("\n📌 Testing LocomoCategories.all:")
  println(s"   Total categories: ${LocomoCategories.all.size}")
  LocomoCategories.all.foreach { cat =>
    println(s"   - ${cat.id}: ${cat.name}")
  }
  
  // Test allGenerators
  println("\n📌 Testing LocomoTestCasesGenerator.allGenerators:")
  val allGens = LocomoTestCasesGenerator.allGenerators
  println(s"   Total generators: ${allGens.size}")
  allGens.foreach { gen =>
    println(s"   - ${gen.category.id}")
  }
  
  // Verify proper paths for evaluation results
  println("\n" + "="*80)
  println("EVALUATION RESULTS PATH VERIFICATION")
  println("="*80)
  
  println("\n📌 Expected evaluation results paths:")
  for (generator <- singletonGenerators.map(_._2)) {
    val resultsPath = s"src/main/resources/evaluation_results/${generator.generatorClassType}/"
    println(s"   ${generator.category.name}:")
    println(s"      -> ${resultsPath}")
  }
  
  // Final summary
  println("\n" + "="*80)
  println("SUMMARY")
  println("="*80)
  println(s"Total questions loaded: $totalQuestions")
  println(s"Expected total: ${expectedCounts.values.sum}")
  
  if (totalQuestions == expectedCounts.values.sum && allPassed) {
    println("✅ ALL TESTS PASSED!")
  } else {
    println("❌ SOME TESTS FAILED")
    System.exit(1)
  }
}