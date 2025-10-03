package com.salesforce.crmmembench.util

import java.io.File
import java.nio.file.{Files, Paths}
import scala.collection.JavaConverters._

/**
 * Utility to find and run a class by its simple name.
 * Searches through the codebase to find the full class name and runs it.
 */
object ClassRunner {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println("Usage: ClassRunner ClassName [args...]")
      System.exit(1)
    }
    
    val className = args(0)
    val runArgs = args.drop(1)
    
    println(s"🔍 Searching for class: $className")
    
    // Find the class
    findClass(className) match {
      case Some(fullClassName) =>
        println(s"✅ Found: $fullClassName")
        println(s"🚀 Running with args: ${runArgs.mkString(" ")}")
        
        // Run the class
        runClass(fullClassName, runArgs)
        
      case None =>
        println(s"❌ Class not found: $className")
        println("\nSuggestions:")
        findSimilarClasses(className).foreach { suggestion =>
          println(s"  - $suggestion")
        }
        System.exit(1)
    }
  }
  
  def findClass(simpleName: String): Option[String] = {
    val sourceDir = Paths.get("src/main/scala")
    
    if (!Files.exists(sourceDir)) {
      println("Error: src/main/scala directory not found")
      return None
    }
    
    // Search all .scala files for the object definition
    val allFiles = Files.walk(sourceDir)
      .iterator()
      .asScala
      .filter(_.toString.endsWith(".scala"))
      .toList
    
    // Look for the object definition in all files
    allFiles.flatMap { path =>
      val content = new String(Files.readAllBytes(path))
      
      // Check if this file contains the object we're looking for
      if (content.contains(s"object $simpleName")) {
        // Extract package name from the file
        val packagePattern = """package\s+([\w.]+)""".r
        val packageName = packagePattern.findFirstMatchIn(content).map(_.group(1))
        
        packageName.map { pkg =>
          s"$pkg.$simpleName"
        }
      } else {
        None
      }
    }.headOption
  }
  
  def findSimilarClasses(simpleName: String): List[String] = {
    val sourceDir = Paths.get("src/main/scala")
    
    if (!Files.exists(sourceDir)) {
      return List.empty
    }
    
    val lowerName = simpleName.toLowerCase
    
    // Search all files for object definitions with similar names
    val allFiles = Files.walk(sourceDir)
      .iterator()
      .asScala
      .filter(_.toString.endsWith(".scala"))
      .toList
    
    val objectPattern = """object\s+(\w+)""".r
    
    allFiles.flatMap { path =>
      val content = new String(Files.readAllBytes(path))
      objectPattern.findAllMatchIn(content).map(_.group(1))
    }
    .filter(_.toLowerCase.contains(lowerName))
    .distinct
    .take(10)
    .toList
  }
  
  def runClass(fullClassName: String, args: Array[String]): Unit = {
    try {
      val clazz = Class.forName(fullClassName)
      val method = clazz.getMethod("main", classOf[Array[String]])
      method.invoke(null, args)
    } catch {
      case e: ClassNotFoundException =>
        println(s"Error: Class not found in classpath: $fullClassName")
        System.exit(1)
      case e: NoSuchMethodException =>
        println(s"Error: No main method found in $fullClassName")
        System.exit(1)
      case e: Throwable =>
        println(s"Error running class: ${e.getMessage}")
        e.printStackTrace()
        System.exit(1)
    }
  }
}