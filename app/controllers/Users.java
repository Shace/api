package controllers;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.User;
import play.libs.Json;
import play.mvc.*;


public class Users extends Controller {

	/**
	 * Get the object node representing a user
	 */
	private static ObjectNode getUserObjectNode(User user) {
		ObjectNode result = Json.newObject();
		
		result.put("id", user.id);
		result.put("mail", user.email);
		result.put("firstname", user.firstName);
		result.put("lastname", user.lastName);
		result.put("birthdate", user.birthDate.getTime());
		result.put("inscription", user.inscriptionDate.getTime());
		
		return result;
	}
	
	/**
	 * List all visible users
	 */
    public static Result users() {
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
    public static Result add() {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete a user
     */
    public static Result delete(Integer id) {
    	return TODO;
    }
    
    /**
     * Update a user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(Integer id) {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get user information
     */
    public static Result user(Integer id) {
    	User user = User.find.byId(id);
    
    	if (user != null) {
    		return ok(getUserObjectNode(user));
    	} else {
    		return notFound("User with id " + id + " not found");
    	}
    }
}
