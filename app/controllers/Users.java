package controllers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.Event;
import models.Privacy;
import models.User;
import play.*;
import play.api.libs.json.JsArray;
import play.libs.Json;
import play.mvc.*;

import views.html.*;

public class Users extends Controller {

	/**
	 * List all visible users
	 */
    public static Result users() {
    	List<User> users = User.find.where().findList();
    	
    	ArrayNode usersNode = Json.newObject().arrayNode();
    	
    	for (User u : users)
    	{
    		ObjectNode result = Json.newObject();
    		result.put("id", u.id);
    		result.put("mail", u.mail);
    		result.put("firstname", u.firstName);
    		result.put("lastname", u.lastName);
    		result.put("birthdate", u.birthDate.getTime());
    		result.put("inscription", u.inscription.getTime());
    		usersNode.add(result);
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
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete a user
     */
    public static Result delete(Integer id) {
    	List<User> users = User.find.where().findList();
    	
    	ArrayNode usersNode = Json.newObject().arrayNode();
    	
    	for (User u : users)
    	{
    		//ObjectNode result = Json.newObject();
    		if (u.id == id)
    		{
    			
    		}
    		//usersNode.add(result);
    	}
    	//ObjectNode result = Json.newObject();
    	//result.put("users", usersNode);
    	//return ok(result);
    	return TODO;
    }
    
    /**
     * Update a user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(Integer id) {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get user information
     */
    public static Result user(String information) {
    	List<User> users = User.find.where().findList();
    	ArrayNode usersNode = Json.newObject().arrayNode();
    	
    	for (User u : users)
    	{
    		ObjectNode result = Json.newObject();
    		if (information == "mail")
    			result.put(information, u.mail);
    		if (information == "password")
    			result.put(information, u.password);
    		if (information == "firstName")
    			result.put(information, u.firstName);
    		if (information == "lastName")
    			result.put(information, u.lastName);
    		if (information == "birthDate")
    			result.put(information, u.birthDate.getTime());
    		if (information == "inscription")
    			result.put(information, u.inscription.getTime());
    		usersNode.add(result);
    	}
    	ObjectNode result = Json.newObject();
    	result.put("user's information", usersNode);
    	return ok(result);
    }
}
