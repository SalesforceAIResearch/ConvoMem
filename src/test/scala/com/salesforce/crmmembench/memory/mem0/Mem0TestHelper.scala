package com.salesforce.crmmembench.memory.mem0

import com.salesforce.crmmembench.Config
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

/**
 * Helper trait for mem0 tests that handles skipping tests when Config.RUN_MEM0_TESTS is false.
 * Mix this trait into any mem0 test suite to automatically skip tests when the flag is disabled.
 */
trait Mem0TestHelper extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: org.scalatest.Suite =>

  var skipTests: Boolean = false

  override def beforeAll(): Unit = {
    if (!Config.RUN_MEM0_TESTS) {
      skipTests = true
      println(s"\nSkipping all tests in ${this.getClass.getSimpleName} - Config.RUN_MEM0_TESTS is false")
      println("To enable mem0 tests, set Config.RUN_MEM0_TESTS = true in Utils.scala\n")
      // Don't call super.beforeAll() to prevent test setup when skipping
      return
    }
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    if (!Config.RUN_MEM0_TESTS) {
      // Don't call super.afterAll() to prevent cleanup when skipping
      return
    }
    super.afterAll()
  }

  /**
   * Wrapper method for test execution that skips if mem0 tests are disabled.
   */
  def runIfMem0Enabled(testCode: => Any): Unit = {
    if (skipTests) {
      // Test is skipped - do nothing
      return
    }
    testCode
  }
}