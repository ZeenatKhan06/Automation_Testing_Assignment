package restapitesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.log4j.*;

import org.json.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static io.restassured.RestAssured.given;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


// Libraries for Reading REST API endpoints from Config.properties.
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class RestAPIAutomationTestingExercise {
	
	static List<RestAPIEndpoint> RestAPIEndpointList;
	
	static List<Map<String, JSONArray>> SportsJSONArrayList;
	
	// Asssignment Question 2 -> A & B
	
	@BeforeClass
	
	public static void setup_multidomain_restapi_endpoints()
			throws IOException,FileNotFoundException,ParseException,JSONException {
		
		// To avoid log4j warning messages during runtime. 
		
		Logger.getRootLogger().setLevel(Level.OFF);
		
		// Retrieve REST API Endpoints from external JSON config file. 
		
		String RestEndpointsJsonfile = ".\\resources\\config\\rest_endpoints_config.json"; // Multi Domain REST API endpoints are retrieved from this file. 
		
		JSONParser jsonParser = new JSONParser();
		
		FileReader reader = new FileReader(RestEndpointsJsonfile);
		
		JSONObject RestEndpointsJsonObject = new JSONObject(jsonParser.parse(reader).toString()); // Populating rest_endpoints_config.json JSON file contents into Java JSONObject. 
		
		JSONArray RestEndpointsJsonArray = RestEndpointsJsonObject.getJSONArray("endpoints"); // Retriving JSON Array of REST API endpoint entries i.e. endpoints[] from JSONObject. 
		
		// JSON Array Deserelization in List of POJO class RestAPIEndpoint using Jackson libraries. 
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		// Converting JSON Array of REST API Endpoint entries into List of RestAPIEndpoint Class Objects
		
		RestAPIEndpointList = objectMapper.readValue(RestEndpointsJsonArray.toString(), new TypeReference<List<RestAPIEndpoint>>(){}); 
	
		/* Retrieve Sports[] JSON Array i.e SportsJSONArray[] from each REST API Endpoint and populate it in ArrayList<baseURI,SportsJSONArray> with its respective baseURI / REST-API Endpoint.
		
		For Example, First object in  Arraylist:  SportsJSONArrayList[0] = <"https://www.unibet.co.uk",SportsJSONArray[]> where, SportsJSONArray[] is JSON array of Sports[] retrieved from "https://www.unibet.co.uk" */		
		
		
		SportsJSONArrayList = new ArrayList<Map<String, JSONArray>>();;
		
		String baseURIString,apiPathString;
		
		// Iterate through RestAPIEndpointList to retrieve each REST API Endpoint to connect and retrieve respective sports[] JSON Array. 
		
		for (int i=0; i < RestAPIEndpointList.size();i++)
		{
			baseURIString = RestAPIEndpointList.get(i).getBaseURI();
			apiPathString = RestAPIEndpointList.get(i).getApiPath();
					
			RestAssured.baseURI = baseURIString;

		      //Obtain HTTP Response in default JSON format using GET request
			
		      Response res = given()
		      .when()
		      .get(apiPathString);
		      
		      if(res.getStatusCode() == 200)
		      {  
		    	  
		    	  JSONObject responseJSONObject = new JSONObject(res.asString());
			  		
		    	  // Retriving JSON Array for Sports[] from HTTP JSON Response from following path $.layout.sections[1].widgets[0].sports
		    	  
			      JSONArray sportsJSONArray = (JSONArray) responseJSONObject.getJSONObject("layout").getJSONArray("sections").getJSONObject(1).getJSONArray("widgets").getJSONObject(0).getJSONArray("sports");
		      
			      Map<String, JSONArray> JSONArrayMap = new HashMap<String, JSONArray>();
			      
			      // Put each JSONArray of Sports[] with its respective REST API Endpoint in SportsJSONArrayList ArrayList for running validation on each attribute (boCount,name,iconURL) of sports[] in remaining tests. 
			      
			      JSONArrayMap.put(baseURIString, sportsJSONArray);
			      
			      SportsJSONArrayList.add(JSONArrayMap);
		      };
		      
		}
	}
	
	@Test
	public void validate_sports_iconURL_valid_URL_for_image_file()
			throws IOException, JSONException {
			
		String baseURIString;
		
		String iconURLValidationAssertionErrorMessage,iconURLImageValidationAssertionErrorMessage;
		
		boolean isIconURLValid;
		boolean isIconURLValidImage;
		int iconURLStatusCode;
		
		JSONArray sportsJSONArray;
		
		for (int i = 0; i < SportsJSONArrayList.size(); i++) 
		{
	    	  
			baseURIString = SportsJSONArrayList.get(i).keySet().toArray()[0].toString();
					
			iconURLImageValidationAssertionErrorMessage = baseURIString + " : " + "iconURL must be valid URL for an Image file";
			
			iconURLValidationAssertionErrorMessage = baseURIString + " : " + "iconURL must be valid URL";
			
			sportsJSONArray = SportsJSONArrayList.get(i).get(baseURIString);
			
			System.out.println("Sports iconURL Validation Started for "+ baseURIString);
			
			for(int j=0; j < sportsJSONArray.length(); j++)
			{
			
				JSONObject sportJsonobject = sportsJSONArray.getJSONObject(j);
	          
				Object iconUrl = sportJsonobject.get("iconUrl");
	          
				Response iconURLResponse = RestAssured.get(iconUrl.toString());
	          
				iconURLStatusCode = iconURLResponse.getStatusCode();
	          
				if (iconURLStatusCode >= 400 && iconURLStatusCode < 500)
				{
					isIconURLValid=false;
					
					// Throws Assertion Errors if iconUrl is an Invalid URL. 
					
					assertTrue(iconURLValidationAssertionErrorMessage + " : "+ iconUrl.toString() + ": Response Code: "+ iconURLStatusCode ,isIconURLValid);
				}
				else if (iconURLStatusCode == 200)
				{
	        	  isIconURLValidImage = iconURLResponse.getContentType().contains("image");
	        	  
	        	// Throws Assertion Errors if iconUrl is not a URL for a Image
	        	  
	        	  assertTrue(iconURLImageValidationAssertionErrorMessage + ": Actual: "+iconUrl.toString(),isIconURLValidImage);
				}
			
			}
	    
		}
		      
	}
				
	
	@Test 
	public void validate_sports_name_contains_only_alphanumeric_space_characaters()
			throws IOException,JSONException  {
		
		String baseURIString;
		
		String nameValidationAssertionErrorMessage;
		
		boolean isSportNameAlphanumeric;
		
		JSONArray sportsJSONArray;
		
		for (int i = 0; i < SportsJSONArrayList.size(); i++) 
		{ 
			baseURIString = SportsJSONArrayList.get(i).keySet().toArray()[0].toString();
					
			nameValidationAssertionErrorMessage = baseURIString + " : " + "Sports Name must only contain Alphanumeric and Space Characters";
			
			sportsJSONArray = SportsJSONArrayList.get(i).get(baseURIString);
			
			System.out.println("Sports Name Validation Started for "+ baseURIString);
			
			for(int j=0; j < sportsJSONArray.length(); j++)
			{
			
				JSONObject sportJsonobject = sportsJSONArray.getJSONObject(j);
	         
				Object sportName = sportJsonobject.get("name");
              
				isSportNameAlphanumeric = sportName instanceof String && sportName.toString().matches("^[a-zA-Z0-9]+\\s*[a-zA-Z0-9]*$");
              
				// Throws Assertion Errors when Sports name contains special characters other than whitespace characters. 
              
				assertTrue(nameValidationAssertionErrorMessage + ": Actual: "+sportName.toString(), isSportNameAlphanumeric);
              
			}
	    
		}
		
	}
		
	@Test
	public void check_sports_boCount_positive_integer()
			throws IOException,JSONException  {
			
		String baseURIString;
		
		String boCountValidationAssertionErrorMessage;
		
		boolean isSportsBoCountPositiveInteger;
		
		JSONArray sportsJSONArray;
			
		for (int i = 0; i < SportsJSONArrayList.size(); i++) 
		{
	    	  
			baseURIString = SportsJSONArrayList.get(i).keySet().toArray()[0].toString();
					
			boCountValidationAssertionErrorMessage = baseURIString + " : " + "boCount attribute must be a Positve Integer";
			
			sportsJSONArray = SportsJSONArrayList.get(i).get(baseURIString);
			
			System.out.println("Sports boCount Validation Started for "+ baseURIString);
			
			for(int j=0; j < sportsJSONArray.length(); j++)
			{
			
				JSONObject sportJsonobject = sportsJSONArray.getJSONObject(j);
	         
				Object boCount = sportJsonobject.get("boCount");
              
				isSportsBoCountPositiveInteger = boCount instanceof Integer && (int) boCount >= 0; 
              
				// Throws Assertion Errors when boCount is not a positive Integer. 
              
				assertTrue(boCountValidationAssertionErrorMessage + ": Actual: "+boCount.toString(), isSportsBoCountPositiveInteger);
              
			}
	    
		}
		
	}
	
	// Asssignment Question 1 -> B
	
	@Test 
	
	public void validate_required_request_parameters()
			throws IOException,JSONException  {
		
		String baseURIConfigFile = ".\\resources\\config\\restapiURLConfig.config"; // REST API URL is retrieved from this file. 
				
		FileReader reader = new FileReader(baseURIConfigFile); 
		
		Properties properties = new Properties();
		
		properties.load(reader);
		
		String RestApiURL = properties.getProperty("RestApiURL");
		
		String errorCode,errorDescription;
		
		RequestSpecification httpRequest = RestAssured.given();
		
		Response response = httpRequest.get(RestApiURL);
		
		int HttpResponseStatusCode = response.getStatusCode();
		
		if(HttpResponseStatusCode == 400)
		{
			errorCode = response.jsonPath().get("error.code").toString();
			errorDescription = response.jsonPath().get("error.description").toString();
			assertTrue(errorCode +": Request Parameter "+ errorDescription,false);		
		}
		
		if(HttpResponseStatusCode == 200)
		{
			assertTrue("Response is not a valid JSON ",response.getContentType().toString().contains("json") && response.asString().contains("endpointUrl"));	
		}
		else
		{
			errorDescription = response.jsonPath().get("error").toString();
			assertEquals(errorDescription + " : "+ "HTTP Status Code: ",HttpStatus.SC_OK,HttpResponseStatusCode);
			
		}
		
	}
	
}	
