package com.salesforce.crmmembench

import java.util.{Timer, TimerTask}
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unified stats reporting framework for periodic and final statistics display.
 * 
 * Usage:
 * 1. Implement the StatsProvider trait to define what stats to display
 * 2. Call StatsReporter.startPeriodicReporting() to begin periodic updates
 * 3. Call StatsReporter.printFinalStats() and cancel() when done
 */
trait StatsProvider {
  /**
   * Generate the current stats string to be displayed.
   * This method is called both periodically and for final reporting.
   */
  def getStatsString: String
  
  /**
   * Called periodically to flush stats and any other data.
   * Default implementation just prints stats. Override for custom behavior.
   */
  def flush(): Unit = {
    println(getStatsString)
  }
  
  /**
   * Starts periodic stats reporting.
   * 
   * @param intervalSeconds How often to display stats (default: 5 seconds)
   * @return A Timer instance that can be cancelled
   */
  def startPeriodicReporting(intervalSeconds: Int = 5): Timer = {
    val timer = new Timer(true) // daemon timer
    val intervalMs = intervalSeconds * 1000
    
    timer.scheduleAtFixedRate(new TimerTask {
      def run(): Unit = {
        flush()
      }
    }, intervalMs, intervalMs) // Start after first interval, then repeat
    
    timer
  }
  
  /**
   * Prints final stats and cancels the periodic reporting.
   * 
   * @param timer The timer to cancel (can be null)
   */
  def printFinalStatsAndCancel(timer: Timer): Unit = {
    if (timer != null) {
      timer.cancel()
    }
    println(getStatsString)
  }
}

/**
 * Deprecated - methods moved to StatsProvider trait
 */
object StatsReporter {
  // Methods have been moved to StatsProvider trait
}

/**
 * Common stats tracking utilities for atomic counters and time calculations.
 */
object StatsUtils {
  
  /**
   * Calculate elapsed time in seconds from a start time.
   */
  def elapsedSeconds(startTime: Long): Double = {
    (System.currentTimeMillis() - startTime) / 1000.0
  }
  
  /**
   * Calculate rate per second for a counter.
   */
  def ratePerSecond(count: Int, elapsedSeconds: Double): Double = {
    if (elapsedSeconds > 0) count / elapsedSeconds else 0.0
  }
  
  /**
   * Calculate rate per minute for a counter.
   */
  def ratePerMinute(count: Int, elapsedSeconds: Double): Double = {
    if (elapsedSeconds > 0) (count / elapsedSeconds) * 60.0 else 0.0
  }
  
  /**
   * Calculate progress percentage.
   */
  def progressPercentage(completed: Int, total: Int): Double = {
    if (total > 0) completed.toDouble / total * 100 else 0.0
  }
  
  /**
   * Format a stats section with Unicode box drawing characters.
   */
  def formatStatsSection(title: String, content: String): String = {
    s"""
📊 $title
$content
""".stripMargin
  }
  
  /**
   * Format a progress line with Unicode characters.
   */
  def formatProgressLine(label: String, completed: Int, total: Int, percentage: Double): String = {
    f"├─ $label%-20s: $completed%3d / $total%3d completed ($percentage%.1f%%)"
  }
  
  /**
   * Format a metric line with rate calculation.
   */
  def formatMetricLine(label: String, count: Int, rate: Double, unit: String): String = {
    f"├─ $label%-20s: $count%4d total (${rate}%.2f $unit/second)"
  }
  
  /**
   * Format a metric line with rate per minute calculation.
   */
  def formatMetricLinePerMinute(label: String, count: Int, rate: Double, unit: String): String = {
    f"├─ $label%-20s: $count%4d total (${rate}%.2f $unit/minute)"
  }
  
  /**
   * Format runtime information.
   */
  def formatRuntimeLine(elapsedSeconds: Double): String = {
    f"└─ Runtime              : ${elapsedSeconds.toInt}%3d seconds"
  }
}

/**
 * Base trait for common stats patterns with atomic counters and timing.
 */
trait BaseStats extends StatsProvider {
  val startTime: Long = System.currentTimeMillis()
  
  def elapsed: Double = StatsUtils.elapsedSeconds(startTime)
  
  def progressPercentage(completed: Int, total: Int): Double = 
    StatsUtils.progressPercentage(completed, total)
    
  def ratePerSecond(count: Int): Double = 
    StatsUtils.ratePerSecond(count, elapsed)
    
  def ratePerMinute(count: Int): Double = 
    StatsUtils.ratePerMinute(count, elapsed)
}