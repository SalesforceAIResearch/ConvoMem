package com.salesforce.crmmembench.evaluation

import com.salesforce.crmmembench.questions.evidence.{EvidenceItem, Conversation, Message}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

/**
 * Handles JSON serialization and deserialization of TestCase objects.
 * Uses Circe for JSON processing to ensure compatibility with existing JSON formats.
 */
object TestCaseSerializer {
  
  // Implicit encoders and decoders for the data model
  implicit val messageEncoder: Encoder[Message] = deriveEncoder[Message]
  implicit val messageDecoder: Decoder[Message] = deriveDecoder[Message]
  
  implicit val conversationEncoder: Encoder[Conversation] = deriveEncoder[Conversation]
  implicit val conversationDecoder: Decoder[Conversation] = deriveDecoder[Conversation]
  
  
  implicit val evidenceItemEncoder: Encoder[EvidenceItem] = deriveEncoder[EvidenceItem]
  implicit val evidenceItemDecoder: Decoder[EvidenceItem] = deriveDecoder[EvidenceItem]
  
  implicit val testCaseEncoder: Encoder[TestCase] = deriveEncoder[TestCase]
  implicit val testCaseDecoder: Decoder[TestCase] = deriveDecoder[TestCase]
  
  /**
   * Serialize a list of test cases to JSON string.
   * 
   * @param testCases List of test cases to serialize
   * @return JSON string representation
   */
  def toJson(testCases: List[TestCase]): String = {
    testCases.asJson.spaces2
  }
  
  /**
   * Serialize a single test case to JSON string.
   * Used for streaming serialization to avoid memory issues.
   * 
   * @param testCase Single test case to serialize
   * @return JSON string representation
   */
  def toJsonForSingleTestCase(testCase: TestCase): String = {
    testCase.asJson.noSpaces // Use noSpaces to save memory
  }
  
  /**
   * Deserialize test cases from JSON string.
   * 
   * @param json JSON string to parse
   * @return Option containing list of test cases if successful
   */
  def fromJson(json: String): Option[List[TestCase]] = {
    parse(json) match {
      case Right(jsonValue) =>
        jsonValue.as[List[TestCase]] match {
          case Right(testCases) => Some(testCases)
          case Left(error) =>
            println(s"Failed to decode test cases: ${error.getMessage}")
            None
        }
      case Left(error) =>
        println(s"Failed to parse JSON: ${error.getMessage}")
        None
    }
  }
}