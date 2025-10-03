package com.salesforce.crmmembench.LLM_endpoints

import com.google.api.HttpBody
import com.google.cloud.aiplatform.v1._
// Removed Gemini.projectId import - using direct projectId definition
import com.salesforce.crmmembench.CostTracker
import io.circe.parser._

import scala.util.{Failure, Success, Try}

/**
 * Claude model implementation that wraps Anthropic's models on Vertex AI
 */
class ClaudeModel(modelName: String, maxTokens: Int = 256, responseFormat: Option[String] = None) extends LLMModel {
  val projectId = "salesforce-research-internal"
  val location = "us-east5"
  
  lazy val predictionClient = PredictionServiceClient.create(
    PredictionServiceSettings.newBuilder()
      .setEndpoint(s"$location-aiplatform.googleapis.com:443")
      .build()
  )
  
  override def generateContent(prompt: String): Try[LLMResponse] = {
    val endpointName = s"projects/$projectId/locations/$location/publishers/anthropic/models/$modelName"
    
    // Build the request payload for Anthropic's format
    val requestJson = responseFormat match {
      case Some("json") =>
        // Use tool-based approach for JSON output
        s"""{
          "anthropic_version": "vertex-2023-10-16",
          "messages": [
            {
              "role": "user",
              "content": "${prompt.replace("\"", "\\\"").replace("\n", "\\n")}"
            }
          ],
          "max_tokens": $maxTokens,
          "tools": [{
            "name": "json_response",
            "description": "Provide the response in JSON format",
            "input_schema": {
              "type": "object",
              "additionalProperties": true
            }
          }],
          "tool_choice": {"type": "tool", "name": "json_response"}
        }"""
      case _ =>
        s"""{
          "anthropic_version": "vertex-2023-10-16",
          "messages": [
            {
              "role": "user",
              "content": "${prompt.replace("\"", "\\\"").replace("\n", "\\n")}"
            }
          ],
          "max_tokens": $maxTokens
        }"""
    }
    
    // Create the raw predict request
    val request = RawPredictRequest.newBuilder()
      .setEndpoint(endpointName)
      .setHttpBody(
        HttpBody.newBuilder()
          .setContentType("application/json")
          .setData(com.google.protobuf.ByteString.copyFromUtf8(requestJson))
          .build()
      )
      .build()
    
    Try {
      val response = predictionClient.rawPredict(request)
      val responseJson = response.getData.toStringUtf8
      
      // Parse the response to extract the content
      parse(responseJson) match {
        case Right(json) =>
          val content = if (responseFormat.contains("json")) {
            // For JSON format, extract the tool use response
            json.hcursor
              .downField("content")
              .downArray
              .downField("input")
              .focus
              .map(_.noSpaces)
              .getOrElse(throw new RuntimeException("No tool use input found in response"))
          } else {
            // Regular text response
            json.hcursor
              .downField("content")
              .downArray
              .downField("text")
              .as[String]
              .getOrElse(throw new RuntimeException("No content found in response"))
          }
          
          // Extract token usage from response
          val tokenUsage = json.hcursor
            .downField("usage")
            .focus
            .flatMap { usageJson =>
              for {
                inputTokens <- usageJson.hcursor.downField("input_tokens").as[Int].toOption
                outputTokens <- usageJson.hcursor.downField("output_tokens").as[Int].toOption
              } yield TokenUsage(
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                totalTokens = inputTokens + outputTokens
              )
            }
          
          // Calculate cost based on model and token usage
          val cost = tokenUsage.map { usage =>
            val inputCostPerMillion = modelName match {
              case "claude-opus-4@20250514" => 15.0    // Opus 4 standard pricing
              case "claude-sonnet-4@20250514" => 3.0   // Sonnet 4 standard pricing
              case _ => 3.0  // Default to Sonnet pricing for other models
            }
            val outputCostPerMillion = modelName match {
              case "claude-opus-4@20250514" => 75.0    // Opus 4 standard pricing
              case "claude-sonnet-4@20250514" => 15.0  // Sonnet 4 standard pricing
              case _ => 15.0  // Default to Sonnet pricing for other models
            }
            
            (usage.inputTokens.toDouble / 1000000 * inputCostPerMillion) + 
            (usage.outputTokens.toDouble / 1000000 * outputCostPerMillion)
          }.getOrElse(0.0)
          
          // Track costs globally
          CostTracker.recordInference(
            modelName = modelName,
            cost = cost,
            inputTokens = tokenUsage.map(_.inputTokens),
            outputTokens = tokenUsage.map(_.outputTokens)
          )
          
          LLMResponse(content, modelName, tokenUsage, cost)
        case Left(error) =>
          throw new RuntimeException(s"Failed to parse response: $error. Raw response: $responseJson")
      }
    }
  }
  
  override def getModelName: String = modelName
  override def getProvider: String = "anthropic"
  
  def close(): Unit = {
    predictionClient.close()
  }
}

object Claude {
//  val location = "us-east5"
//
//  // Legacy objects for backward compatibility
//  lazy val predictionClient = PredictionServiceClient.create(
//    PredictionServiceSettings.newBuilder()
//      .setEndpoint(s"$location-aiplatform.googleapis.com:443")
//      .build()
//  )
  
  // New LLMModel implementations
  lazy val sonnet: LLMModel = ThrottledLLMModel(new ClaudeModel("claude-sonnet-4@20250514"))
  lazy val haiku: LLMModel = new ClaudeModel("claude-haiku-3@20250514")
  lazy val opus: LLMModel = new ClaudeModel("claude-opus-3@20250514")
  lazy val opus4: LLMModel = new ClaudeModel("claude-opus-4@20250514")  // Opus 4 model
  
  // JSON response versions
  lazy val sonnetJson: LLMModel = ThrottledLLMModel(new ClaudeModel("claude-sonnet-4@20250514", 256, Some("json")))
  lazy val haikuJson: LLMModel = new ClaudeModel("claude-haiku-3@20250514", 256, Some("json"))
  lazy val opusJson: LLMModel = new ClaudeModel("claude-opus-3@20250514", 256, Some("json"))
  lazy val opus4Json: LLMModel = new ClaudeModel("claude-opus-4@20250514", 256, Some("json"))
  
  def main(args: Array[String]): Unit = {
    // Test the new LLMModel implementation
    println("Testing regular Claude Sonnet:")
    sonnet.generateContent("Hi") match {
      case Success(response) => println(s"Claude says: ${response.content} (model: ${response.modelName})")
      case Failure(e) => 
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    }
    
    println("\nTesting Claude Sonnet JSON:")
    sonnetJson.generateContent("Generate a JSON object with fields: name (string), age (number), and active (boolean)") match {
      case Success(response) => 
        println(s"Claude JSON response: ${response.content} (model: ${response.modelName})")
        // Try to parse the JSON to verify it's valid
        parse(response.content) match {
          case Right(json) => println("✓ Valid JSON returned!")
          case Left(error) => println(s"✗ Invalid JSON: $error")
        }
      case Failure(e) => 
        println(s"Error: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}
