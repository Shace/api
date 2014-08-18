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
import models.AccessToken.Type;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.api.libs.json.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;
import controllers.AccessTokens;

public class UsersController extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
    	token = AccessToken.create(false, null, Type.GUEST);
    }

    /**
     * Checks the user creation in database through the user controller
     */
    @Test
    public void createUsers() {
    	/**
    	 * Valid request creating one user
    	 */
    	standardAddUser(
    			"{\"email\":\"test@gmail.com\",\"password\":\"test\",\"first_name\":\"Test\",\"last_name\":\"test\"}",
    			CREATED, 1, token.token);
    	

    	/**
         * Valid request creating one user
         */
        standardAddUser(
                "{\"email\":\"test2@gmail.com\",\"password\":\"test\",\"first_name\":\"Test\",\"last_name\":\"test\"}",
                CREATED, 2, token.token);


    	/**
    	 * Valid request with a mail already used
    	 */
    	standardAddUser(
                "{\"email\":\"test@gmail.com\",\"password\":\"test\",\"first_name\":\"Test\",\"last_name\":\"test\"}",
    			BAD_REQUEST, 2, token.token);
    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardAddUser(
                "{\"email\":\"test3@gmail.com\",\"password\":\"test\",\"first_name\":\"Test\",\"last_name\":\"test\"}",
                FORBIDDEN, 2, null);
    
    }
	
    /**
     * Checks the user update in database through the user controller
     */
    @Test
    public void updateUser() {
    	/**
    	 * Initialization
    	 */
    	User newUser = new User("test4@gmail.com", "test", "test", "test");
    	newUser.firstName = "Test";
    	newUser.lastName = "test";
    	newUser.save();
    	
    	AccessToken token = AccessTokens.authenticate(newUser.email, "test", true);
    	
    	/**
    	 * Valid request updating one user
    	 */
    	standardUpdateUser(
    			"{\"first_name\":\"Chuck\",\"last_name\":\"Norris\", \"password\":\"42\"}",
    			newUser.id, OK, true, "Chuck", "Norris", Utils.Hasher.hash("42"), token.token);
    	newUser = User.find.byId(newUser.id);

    	
    	/**
    	 * Valid request without changing values
    	 */
    	standardUpdateUser(
                "{}",
                newUser.id, OK, true, newUser.firstName, newUser.lastName, newUser.password, token.token);
        newUser = User.find.byId(newUser.id);
    	
    	
    	/**
    	 * Unvalid request with not existing user
    	 */
    	standardUpdateUser(
                "{\"first_name\":\"Chuck\",\"last_name\":\"Norris\", \"password\":\"42\"}",
               4242, NOT_FOUND, false, "", "", "", token.token);
    	
    	
    	/**
    	 * Valid request with a not connected user
    	 */
    	standardUpdateUser(
                "{\"first_name\":\"Chuck\",\"last_name\":\"Norris\", \"password\":\"42\"}",
               newUser.id, FORBIDDEN, false, "", "", "", null);
    	
    	
    	/**
    	 * Valid request with another connected
    	 */
    	// TODO: Change when there will be rights handling
        User otherUser = User.create("other@gmail.com", "othersecret");
    	AccessToken otherToken = AccessTokens.authenticate(otherUser.email, "othersecret", true);
    	standardUpdateUser(
                "{\"first_name\":\"Chuck\",\"last_name\":\"Norris\", \"password\":\"42\"}",
               newUser.id, FORBIDDEN, false, "", "", "", otherToken.token);	
    	
    }

    /**
     * Checks the user deletion in database through the user controller
     */
    @Test
    public void deleteUser() {

    }

	/**
     * Simple useful function that generates "add user" test from the parameters.
     * @param jsonBody : the request json body
     * @param expectedStatus : the expected Http response status
     * @param expectedNewUserNumber : the expected number of users in the table after running this test
	 * @param token : the string corresponding to the current connected user (or null if there is not)
     */
	private void	standardAddUser(String jsonBody, int expectedStatus, int expectedNewUserNumber, String token) {
    	FakeRequest fakeRequest = new FakeRequest(POST, "/users/").withJsonBody(Json.parse(jsonBody));
    	Result result = callAction(controllers.routes.ref.Users.add(token), fakeRequest);

    	assertEquals(expectedStatus, status(result));
    	assertEquals(expectedNewUserNumber, User.find.all().size());
	}
	
	/**
     * Simple useful function that generates "update user" test from the parameters.
     * @param jsonBody : the request json body
	 * @param userId : the id of the user to update
     * @param expectedStatus : the expected Http response status
	 * @param checkNewValues : check or not the new values (name, description ...)
	 * @param token : the string corresponding to the current connected user (or null if there is not)
	 */
	private void	standardUpdateUser(String jsonBody, Integer userId, int expectedStatus, boolean checkNewValues, String expectedNewFirstName, String expectedNewLastName, String expectedNewPassword, String token) {
	    FakeRequest fakeRequest = new FakeRequest(PUT, "/users/" + userId).withJsonBody(Json.parse(jsonBody));
    	Result result = callAction(controllers.routes.ref.Users.update(userId, token), fakeRequest);

    	assertEquals(expectedStatus, status(result));
    	if (!checkNewValues)
    		return ;
    	User currentUser = User.find.where().eq("id", userId).findUnique();
    	assertNotNull(currentUser);
    	assertEquals(expectedNewFirstName, currentUser.firstName);
        assertEquals(expectedNewLastName, currentUser.lastName);
        assertEquals(expectedNewPassword, currentUser.password);

	}

    private AccessToken token;
}
