package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.Config

/**
 * Trait that can be mixed into evaluators to enable caching support.
 * 
 * Usage:
 * ```scala
 * object MyEvaluator extends Evaluator with CachedEvaluator {
 *   override def cacheKey: String = "my_evaluator/v1"
 *   override def testCasesGenerator: TestCasesGenerator = ...
 *   // other overrides...
 * }
 * ```
 * 
 * The caching can be controlled via configuration:
 * - Set Config.USE_CACHED_TEST_CASES = true to enable caching globally
 * - Or use environment variable: CRMMEMBENCH_USE_CACHE=true
 */
trait CachedEvaluator extends Evaluator {
  
  /**
   * The cache key to use for this evaluator.
   * Should be unique and include version information if the test case format changes.
   * Example: "user_facts/3_evidence/v1"
   */
  def cacheKey: String
  
  /**
   * The underlying (non-cached) test cases generator.
   */
  def uncachedTestCasesGenerator: TestCasesGenerator
  
  /**
   * Whether to overwrite existing cache (default: true).
   * Can be overridden by subclasses or controlled via environment variable.
   */
  def overwriteCache: Boolean = {
    sys.env.get("CRMMEMBENCH_OVERWRITE_CACHE").map(_.toLowerCase) match {
      case Some("false") | Some("0") | Some("no") => false
      case _ => true // Default is true
    }
  }
  
  /**
   * Override the test cases generator to wrap it with caching if enabled.
   */
  override def testCasesGenerator: TestCasesGenerator = {
    if (shouldUseCache) {
      CachingTestCasesGenerator.wrap(uncachedTestCasesGenerator, cacheKey, overwriteCache)
    } else {
      uncachedTestCasesGenerator
    }
  }
  
  /**
   * Determine if caching should be used.
   * Can be overridden by subclasses for custom logic.
   */
  def shouldUseCache: Boolean = {
    // Check environment variable first
    sys.env.get("CRMMEMBENCH_USE_CACHE").map(_.toLowerCase) match {
      case Some("true") | Some("1") | Some("yes") => true
      case Some("false") | Some("0") | Some("no") => false
      case _ => Config.Evaluation.USE_CACHED_TEST_CASES
    }
  }
}