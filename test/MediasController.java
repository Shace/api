import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.CREATED;
import static play.mvc.Http.Status.FORBIDDEN;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.NO_CONTENT;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.status;
import models.AccessToken;
import models.Event;
import models.Event.Privacy;
import models.Media;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.api.libs.json.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;
import controllers.AccessTokens;

public class MediasController extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerUser = User.create("toto@gmail.com", "secret");
        ownerEvent = new Event(Privacy.PUBLIC, ownerUser);
        ownerEvent.token = "Valid Event";
        ownerEvent.save();
        	
    	token = AccessTokens.authenticate(ownerUser.email, "secret", true);
    }

    /**
     * Checks the media creation in database through the media controller
     */
    @Test
    public void createMedias() {
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
    			ownerEvent.token, NOT_FOUND, 1, null);

    	
    	// TODO : Make a test with a connected user that has no write rights on the event
    	
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
    	 * Valid request updating one media
    	 */
    	standardUpdateMedia(
    			"{\"name\":\"New Test Name\",\"description\":\"New Test Description\"}",
    			ownerEvent.token, newMedia.id, OK, true, "New Test Name", "New Test Description", token.token);
    	newMedia = Media.find.byId(newMedia.id);

    	
    	/**
    	 * Valid request without changing values
    	 */
    	standardUpdateMedia(
    			"{}",
    			ownerEvent.token, newMedia.id, OK, true, newMedia.name, newMedia.description, token.token);
    	newMedia = Media.find.byId(newMedia.id);
    	
    	
    	/**
    	 * Unvalid request with not existing media
    	 */
    	standardUpdateMedia(
    			"{\"name\":\"New Test Name\",\"description\":\"New Test Description\"}",
    			ownerEvent.token, 4242, NOT_FOUND, false, "", "", token.token);
    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardUpdateMedia(
    			"{\"name\":\"New Test Name1\",\"description\":\"New Test Description1\"}",
    			ownerEvent.token, newMedia.id, NOT_FOUND, false, "", "", null);


    	// TODO : Make a test a connected user that has no write rights on the media event
    }

    /**
     * Checks the media deletion in database through the media controller
     */
    @Test
    public void deleteMedias() {
    	/**
    	    	 * Initialization
    	 */
    	Media newMedia = new Media(ownerUser, ownerEvent);
    	newMedia.name = "Test Name";
    	newMedia.description = "Test Description";
    	newMedia.save();
    	Media newMedia1 = new Media(ownerUser, ownerEvent);
    	newMedia1.name = "Test Name";
    	newMedia1.description = "Test Description";
    	newMedia1.save();
    	Media newMedia2 = new Media(ownerUser, null);
    	newMedia2.name = "Test Name";
    	newMedia2.description = "Test Description";
    	newMedia2.save();
    	
    	assertEquals(3, Media.find.all().size());

    	/**
    	 * Valid request deleting one media
    	 */
    	standardDeleteMedia(newMedia.id, ownerEvent.token, NO_CONTENT, 3, token.token);


    	/**
    	 * Unvalid request with a not existing media
    	 */
    	standardDeleteMedia(4242, ownerEvent.token, NOT_FOUND, 3, token.token);


    	/**
    	 * Unvalid request with a not existing event
    	 */
    	standardDeleteMedia(newMedia2.id, ownerEvent.token, NOT_FOUND, 3, token.token);

    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardDeleteMedia(newMedia2.id, ownerEvent.token, NOT_FOUND, 3, null);


    	
    	// TODO : Make a test with a connected user that has no write rights on the event
    }
   
	// TODO : Make the tests for the medias() and media() methods


	/**
     * Simple useful function that generates "add media" test from the parameters.
     * @param jsonBody : the request json body
     * @param eventToken : the token of the event that will contain (or not) the media
     * @param expectedStatus : the expected Http response status
     * @param expectedNewMediaNumber : the expected number of media in the table after running this test
	 * @param token : the string corresponding to the current connected user (or null if there is not)
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
	 * @param token : the string corresponding to the current connected user (or null if there is not)
	 */
	private void	standardUpdateMedia(String jsonBody, String eventToken, Integer mediaId, int expectedStatus, boolean checkNewValues, String expectedNewName, String expectedNewDescription, String token) {
    	FakeRequest fakeRequest = new FakeRequest(PUT, "/events/" + eventToken + "/medias/" + mediaId).withJsonBody(Json.parse(jsonBody));
    	Result result = callAction(controllers.routes.ref.Medias.update(eventToken, mediaId, token), fakeRequest);

    	assertEquals(expectedStatus, status(result));
    	if (!checkNewValues)
    		return ;
    	Media currentMedia = Media.find.byId(mediaId);
    	assertNotNull(currentMedia);
    	assertEquals(expectedNewName, currentMedia.name);
    	assertEquals(expectedNewDescription, currentMedia.description);
	}
    
	/**
     * Simple useful function that generates "delete media" test from the parameters.
	 * @param mediaId : the id of the media to update
     * @param expectedStatus : the expected Http response status
     * @param expectedNewMediaNumber : the expected number of media in the table after running this test
	 * @param token : the string corresponding to the current connected user (or null if there is not)
	 */
    private void	standardDeleteMedia(Integer mediaId, String eventToken, int expectedStatus, int expectedNewMediaNumber, String token) {
    	Result result = callAction(controllers.routes.ref.Medias.delete(eventToken, mediaId, token));

    	assertEquals(expectedStatus, status(result));
    	assertEquals(expectedNewMediaNumber, Media.find.all().size());
	}

    private Event	ownerEvent;
    private User	ownerUser;
    private AccessToken token;
}
