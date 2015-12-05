package mr.cell;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.assertj.core.api.Assertions.assertThat;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import mr.cell.domain.Whisky;

public class VertxAppIT {

	@BeforeClass
	public static void configureRestAssured() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = Integer.getInteger("http.port", 8080);
	}
	
	@AfterClass
	public static void unconfigureRestAssured() {
		RestAssured.reset();
	}
	
	@Test
	public void testThatWeCanRetrieveIndividualProducts() {
		final int id = get("/api/whiskies").then()
			.assertThat()
			.statusCode(200)
			.extract()
			.jsonPath().getInt("find { it.name == 'Jim Beam' }.id");
		get("/api/whiskies/" + id).then()
			.assertThat()
			.statusCode(200)
			.body("name", equalTo("Jim Beam"))
			.body("origin", equalTo("USA"))
			.body("id", equalTo(id));
	}
	
	@Test
	public void testThatWeCanAddAndDeleteIndividualProduct() {
		Whisky whisky = given()
				.body("{\"name\" : \"Jameson\", \"origin\" : \"Ireland\"}")
				.request().post("/api/whiskies").thenReturn().as(Whisky.class);
		assertThat(whisky.getName()).isEqualToIgnoringCase("Jameson");
		assertThat(whisky.getOrigin()).isEqualToIgnoringCase("Ireland");
		assertThat(whisky.getId()).isNotZero();
		
		get("/api/whiskies/" + whisky.getId()).then()
			.assertThat()
			.statusCode(200)
			.body("name", equalTo("Jameson"))
			.body("origin", equalTo("Ireland"))
			.body("id", equalTo(whisky.getId()));
		
		delete("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(204);
		
		get("/api/whiskies/" + whisky.getId()).then().assertThat().statusCode(404);
	}
	
}
