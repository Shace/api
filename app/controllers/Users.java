package controllers;

import java.util.List;

import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.AccessToken;

public class Users extends Controller {

	/**
	 * Get the object node representing a user
	 */
	public static ObjectNode getUserObjectNode(User user) {
		ObjectNode result = Json.newObject();
		
		result.put("id", user.id);
		result.put("mail", user.email);
		result.put("firstname", user.firstName);
		result.put("lastname", user.lastName);
		result.put("birthdate", user.birthDate.getTime());
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
     * Add a user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
    	//JsonNode json = request().body().asJson();
		return TODO;
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
}
