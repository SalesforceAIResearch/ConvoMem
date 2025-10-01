package com.salesforce.crmmembench.LLM_endpoints

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Success, Failure}
import io.circe._
import io.circe.parser._

class OpenAIJsonTest extends AnyFlatSpec with Matchers {
  
  "OpenAI JSON models" should "exist and have proper configuration" in {
    // Check that JSON models exist
    OpenAI.gpt4oMiniJson should not be null
    OpenAI.gpt4oJson should not be null
    
    // Check model names
    OpenAI.gpt4oMiniJson.getModelName shouldBe "gpt-4o-mini"
    OpenAI.gpt4oJson.getModelName shouldBe "gpt-4o"
    
    // Check provider
    OpenAI.gpt4oMiniJson.getProvider shouldBe "openai"
    OpenAI.gpt4oJson.getProvider shouldBe "openai"
  }
  
  it should "return valid JSON responses" in {
    // Test with a prompt that requests JSON output
    val jsonPrompt = """
    Return a JSON object with the following fields:
    - name: "John Doe"
    - age: 30
    - city: "San Francisco"
    
    Only return the JSON object, no other text.
    """
    
    // Test gpt4oMiniJson
    OpenAI.gpt4oMiniJson.generateContent(jsonPrompt) match {
      case Success(response) =>
        println(s"gpt4oMiniJson response: ${response.content}")
        
        // Try to parse the response as JSON
        parse(response.content) match {
          case Right(json) =>
            // Successfully parsed as JSON
            json.isObject shouldBe true
            
            // Check if expected fields exist
            json.hcursor.get[String]("name") match {
              case Right(name) => name should not be empty
              case Left(err) => fail(s"Failed to extract name: $err")
            }
            
            json.hcursor.get[Int]("age") match {
              case Right(age) => age shouldBe 30
              case Left(err) => fail(s"Failed to extract age: $err")
            }
            
            json.hcursor.get[String]("city") match {
              case Right(city) => city should not be empty
              case Left(err) => fail(s"Failed to extract city: $err")
            }
            
          case Left(err) =>
            fail(s"Failed to parse response as JSON: $err\nResponse was: $response")
        }
        
      case Failure(e) =>
        fail(s"Failed to generate content: ${e.getMessage}")
    }
  }
  
  it should "handle complex JSON structures" in {
    val complexJsonPrompt = """
    Return a JSON object representing a person with:
    - name: "Alice Smith"
    - skills: ["Python", "Scala", "Java"]
    - experience: { years: 5, companies: ["Google", "Meta"] }
    
    Only return the JSON object, no other text.
    """
    
    // Test gpt4oJson (the more powerful model)
    OpenAI.gpt4oJson.generateContent(complexJsonPrompt) match {
      case Success(response) =>
        println(s"gpt4oJson complex response: ${response.content}")
        
        parse(response.content) match {
          case Right(json) =>
            // Check structure
            json.isObject shouldBe true
            
            // Check skills array
            json.hcursor.get[List[String]]("skills") match {
              case Right(skills) => 
                skills should contain allOf ("Python", "Scala", "Java")
              case Left(err) => 
                fail(s"Failed to extract skills: $err")
            }
            
            // Check nested experience object
            json.hcursor.downField("experience").get[Int]("years") match {
              case Right(years) => years shouldBe 5
              case Left(err) => fail(s"Failed to extract years: $err")
            }
            
            json.hcursor.downField("experience").get[List[String]]("companies") match {
              case Right(companies) => 
                companies should contain allOf ("Google", "Meta")
              case Left(err) => 
                fail(s"Failed to extract companies: $err")
            }
            
          case Left(err) =>
            fail(s"Failed to parse complex response as JSON: $err\nResponse was: $response")
        }
        
      case Failure(e) =>
        fail(s"Failed to generate complex content: ${e.getMessage}")
    }
  }
  
  it should "be distinct from regular models" in {
    // Verify JSON models are different instances than regular ones
    OpenAI.gpt4oMini should not be OpenAI.gpt4oMiniJson
    OpenAI.gpt4o should not be OpenAI.gpt4oJson
  }
  
  "Parity between Gemini and OpenAI JSON support" should "be verified" in {
    val testPrompt = """
    Return a JSON object with:
    - model: "test"
    - status: "success"
    - timestamp: 123456789
    
    Only return the JSON object.
    """
    
    // Test all JSON models
    val jsonModels = List(
      ("Gemini.flashJson", Gemini.flashJson),
      ("Gemini.proJson", Gemini.proJson),
      ("OpenAI.gpt4oMiniJson", OpenAI.gpt4oMiniJson),
      ("OpenAI.gpt4oJson", OpenAI.gpt4oJson)
    )
    
    jsonModels.foreach { case (modelName, model) =>
      model.generateContent(testPrompt) match {
        case Success(response) =>
          println(s"\n${response.modelName} response: ${response.content}")
          
          parse(response.content) match {
            case Right(json) =>
              // All should return valid JSON
              json.isObject shouldBe true
              
              // All should have the requested fields
              json.hcursor.get[String]("model") shouldBe Right("test")
              json.hcursor.get[String]("status") shouldBe Right("success")
              json.hcursor.get[Long]("timestamp") shouldBe Right(123456789L)
              
              println(s"✓ $modelName returned valid JSON with expected structure")
              
            case Left(err) =>
              fail(s"$modelName failed to return valid JSON: $err")
          }
          
        case Failure(e) =>
          fail(s"$modelName failed to generate content: ${e.getMessage}")
      }
    }
    
    println("\n✓ All JSON models (Gemini and OpenAI) demonstrate parity in JSON support")
  }
}