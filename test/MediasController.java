import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.CREATED;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.status;
import models.AccessToken;
import models.Event;
import models.Media;
import models.User;

import org.junit.Before;
import org.junit.Test;

import controllers.AccessTokens;

import play.api.libs.json.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;

public class MediasController extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerEvent = Event.create("Valid Event", "First Shace's Event", "This is the first Event of the awesome shace app");
        ownerUser = User.create("toto@gmail.com", "secret");
    }
	
    /**
     * Checks the media creation in database through the media controller
     */
    @Test
    public void createMedias() {
    	AccessToken token = AccessTokens.authenticate(ownerUser.email, "secret", true);
    	
    	assertNotNull(token);
    	/**
    	 * Valid request creating one media
    	 */
    	standardAddMedia(
    			"{\"medias\":[{\"name\":\"Test Image\",\"description\":\"Here is the test image description\"}]}",
    			ownerEvent.token, CREATED, 1, token.token);

    	
    	/**
    	 * Unvalid request without the "medias" containing the list in the Json
    	 */
    	standardAddMedia(
    			"{\"name\":\"Test Image\",\"description\":\"Here is the second image of shace event\"}",
    			ownerEvent.token, BAD_REQUEST, 1, token.token);


    	/**
    	 * Unvalid request with a not existing event
    	 */
    	standardAddMedia(
    			"{\"medias\":[{\"name\":\"Test Image\",\"description\":\"Here is the test image description\"}]}",
    			"UNVALID_EVENT", NOT_FOUND, 1, token.token);
    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardAddMedia(
    			"{\"medias\":[{\"name\":\"Test Image\",\"description\":\"Here is the test image description\"}]}",
    			ownerEvent.token, UNAUTHORIZED, 1, null);

    	
    	// TODO : Make a test a connected user that has no write rights on the event
    	
    	/**
    	 * Valid request creating two medias
    	 */
    	standardAddMedia(
    			"{\"medias\":[{\"name\":\"Test Image1\",\"description\":\"Here is the test image 1 description\"}," +
    			"			  {\"name\":\"Test Image2\",\"description\":\"Here is the test image 2 description\"}]}",
    			ownerEvent.token, CREATED, 3, token.token);
    	
    }
	
    /**
     * Checks the media update in database through the media controller
     */
    @Test
    public void updateMedia() {
    	/**
    	 * Initialization
    	 */
    	Media newMedia = new Media(ownerUser, ownerEvent);
    	newMedia.name = "Test Name";
    	newMedia.description = "Test Description";
    	newMedia.save();
    	
    	
    	/**
    	 * Valid request creating one media
    	 */
    	standardUpdateMedia(
    			"{\"name\":\"New Test Name\",\"description\":\"New Test Description\"}",
    			newMedia.id, OK, true, "New Test Name", "New Test Description");
    	newMedia = Media.find.byId(newMedia.id);

    	
    	/**
    	 * Valid request without changing values
    	 */
    	standardUpdateMedia(
    			"{}",
    			newMedia.id, OK, true, newMedia.name, newMedia.description);
    	newMedia = Media.find.byId(newMedia.id);
    	
    	
    	/**
    	 * Unvalid request with not existing media
    	 */
    	standardUpdateMedia(
    			"{\"name\":\"New Test Name\",\"description\":\"New Test Description\"}",
    			4242, NOT_FOUND, false, "", "");


    	// TODO : Make a test with a not connected user
    	// TODO : Make a test a connected user that has no write rights on the media event
    }
    
    /**
     * Simple useful function that generates "add media" test from the parameters.
     * @param jsonBody : the request json body
     * @param eventToken : the token of the event that will contain (or not) the media
     * @param expectedStatus : the expected Http response status
     * @param expectedNewMediaNumber : the expected number of media in the table after running this test
     */
	private void	standardAddMedia(String jsonBody, String eventToken, int expectedStatus, int expectedNewMediaNumber, String token) {
    	FakeRequest fakeRequest = new FakeRequest(POST, "/events/" + eventToken + "/medias").withJsonBody(Json.parse(jsonBody));
    	Result result = callAction(controllers.routes.ref.Medias.add(eventToken, token), fakeRequest);

    	assertEquals(expectedStatus, status(result));
    	assertEquals(expectedNewMediaNumber, Media.find.all().size());
	}
	
	/**
     * Simple useful function that generates "update media" test from the parameters.
     * @param jsonBody : the request json body
	 * @param mediaId : the id of the media to update
     * @param expectedStatus : the expected Http response status
	 * @param checkNewValues : check or not the new values (name, description ...)
	 * @param expectedNewName : the expected new media name
	 * @param expectedNewDescription : the expected new media description
	 */
	private void	standardUpdateMedia(String jsonBody, Integer mediaId, int expectedStatus, boolean checkNewValues, String expectedNewName, String expectedNewDescription) {
    	FakeRequest fakeRequest = new FakeRequest(PUT, "/medias/" + mediaId).withJsonBody(Json.parse(jsonBody));
    	AccessToken token = AccessTokens.authenticate(ownerUser.email, "secret", true);
    	Result result = callAction(controllers.routes.ref.Medias.update(mediaId, token.token), fakeRequest);

    	assertEquals(expectedStatus, status(result));
    	if (!checkNewValues)
    		return ;
    	Media currentMedia = Media.find.byId(mediaId);
    	assertNotNull(currentMedia);
    	assertEquals(expectedNewName, currentMedia.name);
    	assertEquals(expectedNewDescription, currentMedia.description);
	}
	    
    private Event	ownerEvent;
    private User	ownerUser;
}
