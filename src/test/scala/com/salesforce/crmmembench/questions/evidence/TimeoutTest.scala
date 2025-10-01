package com.salesforce.crmmembench.questions.evidence

import com.salesforce.crmmembench.Config
import org.scalatest.funsuite.AnyFunSuite

class TimeoutTest extends AnyFunSuite {
  
  test("Person timeout configuration should be properly set") {
    assert(Config.Generation.PERSON_PROCESSING_TIMEOUT_HOURS == 10)
    assert(Config.Generation.ENABLE_PERSON_TIMEOUT == true)
  }
  
  test("Use case timeout configuration should be properly set") {
    assert(Config.Generation.USE_CASE_TIMEOUT_MINUTES == 60)
    assert(Config.Generation.ENABLE_USE_CASE_TIMEOUT == true)
  }
  
  test("Person timeout calculation should be correct") {
    val timeoutHours = Config.Generation.PERSON_PROCESSING_TIMEOUT_HOURS
    val timeoutMs = timeoutHours * 3600 * 1000L
    
    // 10 hours should be 36,000,000 milliseconds
    assert(timeoutMs == 36000000L)
  }
  
  test("Use case timeout calculation should be correct") {
    val timeoutMinutes = Config.Generation.USE_CASE_TIMEOUT_MINUTES
    val timeoutMs = timeoutMinutes * 60 * 1000L
    
    // 60 minutes should be 3,600,000 milliseconds
    assert(timeoutMs == 3600000L)
  }
}