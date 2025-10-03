package com.salesforce.crmmembench.memory.mem0

import scala.sys.process._
import scala.util.Try
import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean}

/**
 * Simple manager for Mem0 Docker containers.
 * On JVM startup: kills existing containers, purges data, and starts fresh.
 * During runtime: only restarts if things go very bad (too many failures).
 */
object Mem0DockerManager {
  
  val dockerComposeDir = "vendor/mem0/server"
  
  // Use the memory-only configuration for maximum performance
  // Options: "docker-compose.yaml" (standard), "docker-compose-memory-only.yaml" (all in-memory)
  val dockerComposeFile = "docker-compose-memory-only.yaml"
  
  // Track last restart time to prevent restart storms and for reload detection
  val lastRestartTime = new AtomicLong(System.currentTimeMillis())
  val RESTART_COOLDOWN_MS = 3 * 60 * 1000 // 3 minutes
  
  // Track initialization state
  val isInitializing = new AtomicBoolean(false)
  val initializationLock = new Object()
  
  // Track if containers have been fully initialized and are healthy
  val containersReady = new AtomicBoolean(false)
  
  // This lazy val ensures we only kill and restart containers ONCE per JVM lifetime
  lazy val initialized: Boolean = {
    println("🐳 Initializing Mem0 Docker environment (ONCE per JVM)...")
    
    // Kill and purge any existing containers
    if (areContainersRunning()) {
      println("🔄 Found existing containers - killing and purging all data...")
      stopAndPurge()
      Thread.sleep(3000)
    }
    
    // Start fresh
    start()
    
    // Update restart time after successful initialization
    lastRestartTime.set(System.currentTimeMillis())
    containersReady.set(true)
    println("✅ Mem0 environment is ready!")
    true
  }
  
  /**
   * Initialize Mem0 environment. The lazy val ensures this only happens once.
   */
  def initialize(): Unit = {
    // If containers are already ready and healthy, just verify and return
    if (containersReady.get() && isHealthy()) {
      return
    }
    
    initializationLock.synchronized {
      // Double-check inside the lock
      if (containersReady.get() && isHealthy()) {
        return
      }
      
      // If someone is already initializing, wait for them to finish
      if (isInitializing.get()) {
        println("⏳ Another thread is initializing Mem0, waiting 30 seconds...")
        Thread.sleep(30000)
        
        // After waiting, check if it's healthy
        if (!isHealthy()) {
          println("⚠️  Mem0 still not healthy after waiting, waiting additional 20 seconds...")
          Thread.sleep(20000)
          if (!isHealthy()) {
            throw new RuntimeException("Mem0 failed to become healthy after waiting for initialization")
          }
        }
        return
      }
      
      // Mark that we're initializing
      isInitializing.set(true)
      try {
        // Force lazy val initialization
        initialized
        
        // The first thread should wait longer after initialization
        println("⏳ Primary initialization thread waiting 20 seconds for Mem0 to fully start...")
        Thread.sleep(20000)
        
        // Check health
        if (!isHealthy()) {
          println("⚠️  Mem0 not healthy, waiting additional 10 seconds...")
          Thread.sleep(10000)
          if (!isHealthy()) {
            throw new RuntimeException("Mem0 failed to become healthy")
          }
        }
        
        // Mark containers as ready after successful initialization and health check
        containersReady.set(true)
      } finally {
        isInitializing.set(false)
      }
    }
  }
  
  /**
   * Start Docker containers.
   */
  def start(): Unit = {
    try {
      println(s"🚀 Starting Mem0 containers with $dockerComposeFile...")
      val startCmd = Process(
        Seq("docker", "compose", "-f", dockerComposeFile, "up", "-d"), 
        new java.io.File(dockerComposeDir)
      )
      val result = startCmd.!!
      println(result)
      
      // Wait for containers to be ready (much faster with in-memory storage)
      println("⏳ Waiting 20 seconds for services to initialize...")
      Thread.sleep(50000) // Wait 50 seconds before checking health
      
      // Simple health check
      if (!isHealthy()) {
        throw new RuntimeException("Mem0 services failed to become healthy")
      }
      
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Failed to start Mem0: ${e.getMessage}", e)
    }
  }
  
  /**
   * Stop containers and purge all data.
   */
  def stopAndPurge(): Unit = {
    try {
      println("🛑 Stopping and purging Mem0 containers...")
      
      // Stop containers and remove volumes
      val stopCmd = Process(
        Seq("docker", "compose", "-f", dockerComposeFile, "down", "-v", "--remove-orphans"), 
        new java.io.File(dockerComposeDir)
      )
      stopCmd.!
      
      // Remove history database
      val historyDb = new java.io.File(s"$dockerComposeDir/history/history.db")
      if (historyDb.exists()) {
        historyDb.delete()
      }
      
      Thread.sleep(2000) // Brief pause to ensure cleanup
      
    } catch {
      case e: Exception =>
        println(s"⚠️  Error during cleanup: ${e.getMessage}")
    }
  }
  
  /**
   * Check if any containers are running.
   */
  def areContainersRunning(): Boolean = {
    Try {
      val checkCmd = Process(
        Seq("docker", "compose", "-f", dockerComposeFile, "ps", "-q"),
        new java.io.File(dockerComposeDir)
      )
      val result = checkCmd.!!.trim
      result.nonEmpty
    }.getOrElse(false)
  }
  
  /**
   * Simple health check.
   */
  def isHealthy(): Boolean = {
    Try {
      // Check if we can connect to mem0
      val socket = new java.net.Socket()
      socket.connect(new java.net.InetSocketAddress("localhost", 8888), 2000)
      socket.close()
      true
    }.getOrElse(false)
  }
  
  /**
   * Emergency restart - only use when things are really broken.
   * Synchronized to prevent multiple threads from restarting simultaneously.
   * Has a cooldown period to prevent restart storms.
   */
  def emergencyRestart(): Unit = initializationLock.synchronized {
    val now = System.currentTimeMillis()
    val lastRestart = lastRestartTime.get()
    val timeSinceLastRestart = now - lastRestart
    
    if (timeSinceLastRestart < RESTART_COOLDOWN_MS) {
      val remainingCooldown = (RESTART_COOLDOWN_MS - timeSinceLastRestart) / 1000
      println(s"⏱️  Emergency restart skipped - cooldown active (${remainingCooldown}s remaining)")
      return
    }
    
    // Mark that we're initializing (restarting)
    isInitializing.set(true)
    containersReady.set(false)  // Reset ready state during restart
    try {
      println("🚨 Emergency restart requested...")
      
      stopAndPurge()
      start()
      
      // Update restart time AFTER successful restart
      lastRestartTime.set(System.currentTimeMillis())
      containersReady.set(true)  // Mark as ready after successful restart
      println(s"✅ Emergency restart completed at ${new java.util.Date(lastRestartTime.get())}")
    } catch {
      case e: Exception =>
        println(s"❌ Emergency restart failed: ${e.getMessage}")
        throw e
    } finally {
      isInitializing.set(false)
    }
  }
  
  /**
   * Get the timestamp of the last restart (for reload detection).
   */
  def getLastRestartTime(): Long = lastRestartTime.get()
  
  /**
   * Shutdown on JVM exit.
   */
  def shutdown(): Unit = {
    stopAndPurge()
  }
}