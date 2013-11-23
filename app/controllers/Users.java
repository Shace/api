package controllers;

import java.util.Date;
import java.util.List;

import models.AccessToken;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@CORS
public class Users extends Controller {

	/**
	 * Get the object node representing a user
	 */
	public static ObjectNode getUserObjectNode(User user) {
		ObjectNode result = Json.newObject();
		
		result.put("id", user.id);
		result.put("email", user.email);
		result.put("first_name", user.firstName);
		result.put("last_name", user.lastName);
		result.put("birth_date", user.birthDate.getTime());
		result.put("inscription", user.inscriptionDate.getTime());
		result.put("is_admin", user.isAdmin);
		
		return result;
	}
	
	/**
	 * List all visible users
	 */
    public static Result users(String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);

    	if (access == null)
    		return unauthorized("Not a valid token");
    	else if (!access.isConnectedUser())
    		return unauthorized("No user connected");
    	else if (access.user.isAdmin == false)
    		return forbidden("You need to be admin");
    	List<User> users = User.find.findList();
    	
    	ArrayNode usersNode = Json.newObject().arrayNode();
    	
    	for (User user : users) {
    		usersNode.add(getUserObjectNode(user));
    	}
    	ObjectNode result = Json.newObject();
    	result.put("users", usersNode);
    	
    	return ok(result);
    }

    /**
     * Create a new user.
     * The user properties are contained into the HTTP Request body as Json format.
	 * @return An HTTP Json response containing the properties of the created user
     */    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
    	JsonNode root = request().body().asJson();
    	if (root == null)
    		return badRequest("Unexpected format, JSon required");
    	String email = root.get("email").textValue();
    	if (email == null)
    		return badRequest("Missing parameter [email]");

    	String password = root.get("password").textValue();
    	if (password == null)
    		return badRequest("Missing parameter [password]");

    	User newUser = User.create(email, password);
    	updateOneUser(newUser, root);
    	newUser.save();
		return created(getUserObjectNode(newUser));
    }
    
    /**
     * Delete a user
     */
    public static Result delete(Integer id, String accessToken) {
    	return TODO;
    }
    
    /**
     * Update a user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(Integer id, String accessToken) {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get user information
     */
    public static Result user(Integer id, String accessToken) {
    	User user = User.find.byId(id);
    
    	if (user != null) {
    		return ok(getUserObjectNode(user));
    	} else {
    		return notFound("User with id " + id + " not found");
    	}
    }
    
    public static User authenticate(String email, String password) {
    	String sha1 = Utils.hash(password);
    	
    	if (sha1 == null)
    		return null;
		return User.find.where().eq("email", email).eq("password", sha1).findUnique();
	}
    
    /**
     * Update the user properties from a Json object.
     * @param currentUser : The user to update
     * @param currentNode : The new properties to set
     */
    private static void updateOneUser(User currentUser, JsonNode currentNode) {
    	String	email = currentNode.findPath("email").textValue();
    	if (email != null)
    		currentUser.email = email;
    	String	password = currentNode.findPath("password").textValue();
    	if (password != null)
    		currentUser.password = Utils.hash(password);
    	String	firstName = currentNode.findPath("first_name").textValue();
    	if (firstName != null)
    		currentUser.firstName = firstName;
    	String	lastName = currentNode.findPath("last_name").textValue();
    	if (lastName != null)
    		currentUser.lastName = lastName;
    	Long	dateTime = currentNode.findPath("birth_date").asLong();
    	if (dateTime != null) {
        	Date	birthDate = new Date(dateTime);
    		currentUser.birthDate = birthDate;
    	}
    }
}
