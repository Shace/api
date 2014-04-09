import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.CREATED;
import static play.mvc.Http.Status.FORBIDDEN;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.status;
import models.AccessToken;
import models.Event;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.api.libs.json.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;
import controllers.AccessTokens;

public class EventsController extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerUser = User.create("toto@gmail.com", "secret");
    	token = AccessTokens.authenticate(ownerUser.email, "secret", true);
    }

    /**
     * Checks the event creation in database through the event controller
     */
    @Test
    public void createEvents() {
    	/**
    	 * Valid request creating one event
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token public\",\"privacy\":\"public\",\"name\":\"Test Event\",\"description\":\"Here is the test event description\"}",
    			CREATED, 1, token.token);
    	

    	/**
    	 * Valid request creating one event
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token protected\",\"privacy\":\"public\",\"name\":\"Test Event\",\"password\":\"secret\",\"description\":\"Here is the test event description\"}",
    			CREATED, 2, token.token);
    	

    	/**
    	 * Valid request creating one event
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token private\",\"privacy\":\"public\",\"name\":\"Test Event\",\"description\":\"Here is the test event description\"}",
    			CREATED, 3, token.token);


    	/**
    	 * Valid request with a token already used
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token public\",\"privacy\":\"public\",\"name\":\"Test Event\",\"description\":\"Here is the test event description\"}",
    			BAD_REQUEST, 3, token.token);
    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token1\",\"privacy\":\"public\",\"name\":\"Test Event\",\"description\":\"Here is the test event description\"}",
    			FORBIDDEN, 3, null);

    	
    	/**
    	 * Unvalid request with a bad privacy name
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token1\",\"privacy\":\"UNVALID\",\"name\":\"Test Event\",\"description\":\"Here is the test event description\"}",
    			BAD_REQUEST, 3, token.token);
    	
    	
    	/**
    	 * Unvalid request with a protected privacy but no password
    	 */
    	standardAddEvent(
    			"{\"token\":\"test token1\",\"privacy\":\"protected\",\"name\":\"Test Event\",\"description\":\"Here is the test event description\"}",
    			BAD_REQUEST, 3, token.token);    	
    }
	
    /**
     * Checks the event update in database through the event controller
     */
    @Test
    public void updateEvent() {
    	/**
    	 * Initialization
    	 */
    	Event newEvent = new Event(Event.Privacy.PUBLIC, ownerUser);
    	newEvent.token = "event token";
    	newEvent.name = "Test Name";
    	newEvent.description = "Test Description";
    	newEvent.save();
    	
    	
    	/**
    	 * Valid request updating one event
    	 */
    	standardUpdateEvent(
    			"{\"name\":\"New Test Name\",\"description\":\"New Test Description\"}",
    			newEvent.token, OK, true, "New Test Name", "New Test Description", token.token);
    	newEvent = Event.find.byId(newEvent.token);

    	
    	/**
    	 * Valid request without changing values
    	 */
    	standardUpdateEvent(
    			"{}",
    			newEvent.token, OK, true, newEvent.name, newEvent.description, token.token);
    	newEvent = Event.find.byId(newEvent.token);
    	
    	
    	/**
    	 * Unvalid request with not existing event
    	 */
    	standardUpdateEvent(
    			"{\"name\":\"New Test Name\",\"description\":\"New Test Description\"}",
    			"unvalid_token", NOT_FOUND, false, "", "", token.token);
    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardUpdateEvent(
    			"{\"name\":\"New Test Name1\",\"description\":\"New Test Description1\"}",
    			newEvent.token, FORBIDDEN, false, "", "", null);
    	
    	
    	/**
    	 * Valid request with another connected
    	 */
    	// TODO: Change when there will be rights handling
        User otherUser = User.create("other@gmail.com", "othersecret");
    	AccessToken otherToken = AccessTokens.authenticate(otherUser.email, "othersecret", true);
    	standardUpdateEvent(
    			"{\"name\":\"New Test Name1\",\"description\":\"New Test Description1\"}",
    			newEvent.token, FORBIDDEN, false, "", "", otherToken.token);  	
    	
    }

    /**
     * Checks the media deletion in database through the media controller
     */
    @Test
    public void deleteEvent() {
//    	/**
//    	 * Initialization
//    	 */
//    	Media newMedia = new Media(ownerUser, ownerEvent);
//    	newMedia.name = "Test Name";
//    	newMedia.description = "Test Description";
//    	newMedia.save();
//    	Media newMedia1 = new Media(ownerUser, ownerEvent);
//    	newMedia1.name = "Test Name";
//    	newMedia1.description = "Test Description";
//    	newMedia1.save();
//    	Media newMedia2 = new Media(ownerUser, null);
//    	newMedia2.name = "Test Name";
//    	newMedia2.description = "Test Description";
//    	newMedia2.save();
//    	
//    	assertEquals(3, Media.find.all().size());
//
//    	/**
//    	 * Valid request deleting one media
//    	 */
//    	standardDeleteEvent(newMedia.id, NO_CONTENT, 2, token.token);
//
//
//    	/**
//    	 * Unvalid request with a not existing media
//    	 */
//    	standardDeleteEvent(4242, NOT_FOUND, 2, token.token);
//
//
//    	/**
//    	 * Unvalid request with a not existing event
//    	 */
//    	standardDeleteEvent(newMedia2.id, NOT_FOUND, 2, token.token);
//
//    	
//    	
//    	/**
//    	 * Valid request with a not connected user
//    	 */
//    	// TODO: Uncomment this when the access tokens are available for the update method on the controller
////    	standardDeleteMedia(newMedia2.id, UNAUTHORIZED, 1, null);
//
//
//    	
//    	// TODO : Make a test with a connected user that has no write rights on the event
    }
   
	// TODO : Make the tests for the medias() and media() methods


	/**
     * Simple useful function that generates "add event" test from the parameters.
     * @param jsonBody : the request json body
     * @param expectedStatus : the expected Http response status
     * @param expectedNewEventNumber : the expected number of event in the table after running this test
	 * @param token : the string corresponding to the current connected user (or null if there is not)
     */
	private void	standardAddEvent(String jsonBody, int expectedStatus, int expectedNewEventNumber, String token) {
    	FakeRequest fakeRequest = new FakeRequest(POST, "/events/").withJsonBody(Json.parse(jsonBody));
    	Result result = callAction(controllers.routes.ref.Events.add(token), fakeRequest);
    	assertEquals(expectedStatus, status(result));
    	assertEquals(expectedNewEventNumber, Event.find.all().size());
	}
	
	/**
     * Simple useful function that generates "update event" test from the parameters.
     * @param jsonBody : the request json body
	 * @param eventId : the id of the event to update
     * @param expectedStatus : the expected Http response status
	 * @param checkNewValues : check or not the new values (name, description ...)
	 * @param expectedNewName : the expected new event name
	 * @param expectedNewDescription : the expected new event description
	 * @param token : the string corresponding to the current connected user (or null if there is not)
	 */
	private void	standardUpdateEvent(String jsonBody, String eventId, int expectedStatus, boolean checkNewValues, String expectedNewName, String expectedNewDescription, String token) {
    	FakeRequest fakeRequest = new FakeRequest(PUT, "/events/" + eventId).withJsonBody(Json.parse(jsonBody));
    	Result result = callAction(controllers.routes.ref.Events.update(eventId, token), fakeRequest);

    	assertEquals(expectedStatus, status(result));
    	if (!checkNewValues)
    		return ;
    	Event currentEvent = Event.find.where().eq("token", eventId).findUnique();
    	assertNotNull(currentEvent);
    	assertEquals(expectedNewName, currentEvent.name);
    	assertEquals(expectedNewDescription, currentEvent.description);
	}
    
	/**
     * Simple useful function that generates "delete event" test from the parameters.
	 * @param eventId : the id of the event to update
     * @param expectedStatus : the expected Http response status
     * @param expectedNewEventNumber : the expected number of event in the table after running this test
	 * @param token : the string corresponding to the current connected user (or null if there is not)
	 */
    private void	standardDeleteEvent(String eventId, int expectedStatus, int expectedNewEventNumber, String token) {
    	Result result = callAction(controllers.routes.ref.Events.delete(eventId, token));

    	assertEquals(expectedStatus, status(result));
    	assertEquals(expectedNewEventNumber, Event.find.all().size());
	}

    private User	ownerUser;
    private AccessToken token;
}
