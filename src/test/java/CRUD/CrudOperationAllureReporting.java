package CRUD;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Epic("CRUD Operations")
@Feature("GoogleMapReplica API")
public class CrudOperationAllureReporting {

    private static final Logger logger = LoggerFactory.getLogger(CrudOperationAllureReporting.class);

    // API details. We'll use these a lot, so let's make them constants.
    private static final String BASE_URL = "https://rahulshettyacademy.com";
    private static final String BASE_PATH = "/maps/api/place";
    private static final String API_KEY = "qaclick123";
    private static final String ADD_RESOURCE = "/add/json";
    private static final String GET_RESOURCE = "/get/json";
    private static final String UPDATE_RESOURCE = "/update/json";
    private static final String DELETE_RESOURCE = "/delete/json";

    // We'll use this for all our requests. Saves us some typing later.
    private static RequestSpecification rs;

    // We'll need this ID for our tests. Let's keep it handy.
    private static String placeId;

    @BeforeClass
    @Step("Setting up the test environment")
    public void setup() {
        // First things first, let's set up our base URL and path
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = BASE_PATH;

        // Now, let's create a request spec with some common stuff we'll need for all requests
        rs = RestAssured.given()
                .queryParam("key", API_KEY)
                .contentType(ContentType.JSON)
                .filter(new AllureRestAssured()); // This is for nice Allure reports

        // Let's log some info about our setup. Future us will thank us for this!
        logEnvironmentInfo();
    }

    @Step("Logging environment information")
    private void logEnvironmentInfo() {
        // It's always good to know where we're sending requests to
        String fullUrl = BASE_URL + BASE_PATH;
        Allure.addAttachment("Full Base URL", fullUrl);
        Allure.addAttachment("API Key", API_KEY);
        Allure.addAttachment("Timestamp", getCurrentTimestamp());
    }

    @Test(priority = 1)
    @Story("Add a new place")
    @Description("Adding a new place using POST request")
    public void addPlace() throws IOException {
        // Let's grab our payload from the JSON file
        String payload = loadJsonFile("add_place.json");

        Allure.addAttachment("Request Payload", payload);

        logFullUrl(ADD_RESOURCE);
        rs.body(payload);
        logStep("Alright, sending POST request to add a new place");

        // Fire off the request and grab the response
        Response response = rs.post(ADD_RESOURCE);
        logResponse(response);

        // We'll need this place ID later, so let's save it
        placeId = response.jsonPath().getString("place_id");
        logStep("Got the Place ID: " + placeId);
        Assert.assertNotNull(placeId, "Oops, Place ID is null. That's not good.");

        Allure.addAttachment("Place ID", placeId);
    }

    @Test(priority = 2, dependsOnMethods = "addPlace")
    @Story("Update an existing place")
    @Description("Updating the place with new address using PUT request")
    public void updatePlace() throws IOException {
        String newAddress = "Galaxy Apartments, Pune";
        logStep("We're updating the address to: " + newAddress);

        // Let's customize our update payload
        String payload = loadJsonFile("update_place.json")
                .replace("${placeId}", placeId)
                .replace("${newAddress}", newAddress)
                .replace("${apiKey}", API_KEY);

        Allure.addAttachment("Update Request Payload", payload);

        logFullUrl(UPDATE_RESOURCE);
        rs.body(payload);
        logStep("Sending PUT request to update the address");

        // Send the update request
        Response response = rs.put(UPDATE_RESOURCE);
        logResponse(response);

        // Let's make sure the update actually worked
        String msg = response.jsonPath().getString("msg");
        Assert.assertEquals(msg, "Address successfully updated", "Hmm, update message doesn't look right");

        Allure.addAttachment("Update Message", msg);
    }

    @Test(priority = 3, dependsOnMethods = "updatePlace")
    @Story("Retrieve an existing place")
    @Description("Retrieving the updated place using GET request")
    public void getPlace() {
        logFullUrl(GET_RESOURCE);
        rs.queryParam("place_id", placeId);
        logStep("Time to check if our update worked. Sending GET request.");

        // Let's get our updated place details
        Response response = rs.get(GET_RESOURCE);
        logResponse(response);

        // Did we get the right address?
        String retrievedAddress = response.jsonPath().getString("address");
        logStep("Address we got back: " + retrievedAddress);

        Assert.assertEquals(retrievedAddress, "Galaxy Apartments, Pune", "Uh oh, address doesn't match what we set");

        Allure.addAttachment("Retrieved Address", retrievedAddress);
    }

    @Test(priority = 4, dependsOnMethods = "getPlace")
    @Story("Delete an existing place")
    @Description("Deleting the place using DELETE request")
    public void deletePlace() throws IOException {
        // Set up our delete payload
        String payload = loadJsonFile("delete_place.json")
                .replace("${placeId}", placeId);

        Allure.addAttachment("Delete Request Payload", payload);

        logFullUrl(DELETE_RESOURCE);
        rs.body(payload);
        logStep("Okay, let's delete this place. Sending DELETE request.");

        // Time to say goodbye to our place
        Response response = rs.delete(DELETE_RESOURCE);
        logResponse(response);

        // Did it work?
        String status = response.jsonPath().getString("status");
        Assert.assertEquals(status, "OK", "Delete didn't work? That's not great.");

        Allure.addAttachment("Delete Status", status);
    }

    // Helper method to load our JSON payloads
    private String loadJsonFile(String fileName) throws IOException {
        String filePath = "src/test/resources/payloads/" + fileName;
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    // This helps us see what's going on with our requests
    @Attachment(value = "API Response", type = "application/json")
    private byte[] logResponse(Response response) {
        logStep("Here's what we got back: " + response.asString());
        return response.asString().getBytes();
    }

    // Quick way to log steps
    @Step("{0}")
    private void logStep(String step) {
        logger.info(step);
    }

    // Log the full URL we're hitting
    @Step("Full URL: {0}")
    private void logFullUrl(String resource) {
        String fullUrl = BASE_URL + BASE_PATH + resource;
        Allure.addAttachment("Full URL", fullUrl);
    }

    // Just a helper to get the current time
    private String getCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}