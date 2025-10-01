package com.salesforce.crmmembench.memory.mem0

import com.salesforce.crmmembench.Config
import org.scalatest.funsuite.AnyFunSuite

class Mem0FlagTest extends AnyFunSuite with Mem0TestHelper {
  
  test("Mem0 flag should be false by default") {
    runIfMem0Enabled {
      // This should not run
      fail("This test should have been skipped when Config.RUN_MEM0_TESTS is false")
    }
    
    // This should always run
    assert(!Config.RUN_MEM0_TESTS, "Config.RUN_MEM0_TESTS should be false by default")
    println("✅ Mem0 tests are correctly disabled")
  }
}