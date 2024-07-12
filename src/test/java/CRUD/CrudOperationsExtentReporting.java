package CRUD;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.Matchers.equalTo;

public class CrudOperationsExtentReporting {
    /*========================================================================================================================*/
    // Constants
    private static final String BASE_URL = "https://rahulshettyacademy.com";
    private static final String API_KEY = "qaclick123";
    private static final String ADD_RESOURCE = "/maps/api/place/add/json";
    private static final String GET_RESOURCE = "/maps/api/place/get/json";
    private static final String UPDATE_RESOURCE = "/maps/api/place/update/json";
    private static final String DELETE_RESOURCE = "/maps/api/place/delete/json";

    // Static variables
    private static RequestSpecification rs;
    private static String placeId;

    // Extent Reports
    private static ExtentReports extent;
    private static ExtentTest test;
    /*========================================================================================================================*/

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;
        rs = RestAssured.given().queryParam("key", API_KEY).contentType(ContentType.JSON);

        // Generate unique report name with date and time stamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String reportName = "ExtentReport_" + timeStamp + ".html";

        extent = new ExtentReports();
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter("target/ExtentReports/" + reportName);
        extent.attachReporter(sparkReporter);
    }

    @AfterClass
    public void teardown() {
        if (extent != null) {
            extent.flush();
        }
    }

    @Test(priority = 1)
    public void addPlace() {
        test = extent.createTest("Add Place", "Adding a new place using POST request");
        try {
            test.info("Starting Add Place Test");
            rs.basePath(ADD_RESOURCE).body(Payloads.addPlace());
            test.info("Sending POST request to add a new place");

            Response response = rs.post();

            ValidatableResponse validatableResponse = response.then();
            test.info("Response received: " + response.asString());
            validatableResponse.statusCode(200).header("Server", "Apache/2.4.52 (Ubuntu)");
            test.pass("Validated status code 200 and server header");

            placeId = response.jsonPath().getString("place_id");
            test.info("Place ID: " + placeId);
            System.out.println("Place ID: " + placeId);

            test.pass("Place added successfully with Place ID: " + placeId);
        } catch (Exception e) {
            test.fail("Failed to add place: " + e.getMessage());
        }
    }

    @Test(priority = 2, dependsOnMethods = "addPlace")
    public void updatePlace() {
        test = extent.createTest("Update Place", "Updating the place with new address using PUT request");
        try {
            test.info("Starting Update Place Test");
            String newAddress = "Galaxy Apartments, Pune";
            test.info("New Address: " + newAddress);
            rs.basePath(UPDATE_RESOURCE).body("{\n" +
                    "\"place_id\":\"" + placeId + "\",\n" +
                    "\"address\":\"" + newAddress + "\",\n" +
                    "\"key\":\"" + API_KEY + "\"\n" +
                    "}");
            test.info("Sending PUT request to update the address");

            Response response = rs.put();
            ValidatableResponse validatableResponse = response.then();
            test.info("Response received: " + response.asString());
            validatableResponse.statusCode(200).body("msg", equalTo("Address successfully updated"));
            test.pass("Validated status code 200 and success message");

            test.pass("Address successfully updated to: " + newAddress);
        } catch (Exception e) {
            test.fail("Failed to update address: " + e.getMessage());
        }
    }

    @Test(priority = 3, dependsOnMethods = "updatePlace")
    public void getPlace() {
        test = extent.createTest("Get Place", "Retrieving the updated place using GET request");
        try {
            test.info("Starting Get Place Test");
            rs.basePath(GET_RESOURCE).queryParam("place_id", placeId);
            test.info("Sending GET request to retrieve the place");

            Response response = rs.get();
            ValidatableResponse validatableResponse = response.then();
            test.info("Response received: " + response.asString());
            validatableResponse.statusCode(200);
            test.pass("Validated status code 200");

            String retrievedAddress = response.jsonPath().getString("address");
            test.info("Retrieved Address: " + retrievedAddress);
            System.out.println("Retrieved Address: " + retrievedAddress);

            Assert.assertEquals(retrievedAddress, "Galaxy Apartments, Pune", "Validating Address..");
            test.pass("Successfully retrieved and validated updated address: " + retrievedAddress);
        } catch (Exception e) {
            test.fail("Failed to retrieve place: " + e.getMessage());
        }
    }

    @Test(priority = 4, dependsOnMethods = "getPlace")
    public void deletePlace() {
        test = extent.createTest("Delete Place", "Deleting the place using DELETE request");
        try {
            test.info("Starting Delete Place Test");
            rs.basePath(DELETE_RESOURCE).body("{\n" +
                    "\"place_id\":\"" + placeId + "\"\n" +
                    "}");
            test.info("Sending DELETE request to remove the place");

            Response response = rs.delete();
            ValidatableResponse validatableResponse = response.then();
            test.info("Response received: " + response.asString());
            validatableResponse.statusCode(200).body("status", equalTo("OK"));
            test.pass("Validated status code 200 and success message");

            test.pass("Place deleted successfully with Place ID: " + placeId);
        } catch (Exception e) {
            test.fail("Failed to delete place: " + e.getMessage());
        }
    }
}
