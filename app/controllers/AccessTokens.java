package controllers;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.AccessToken;
import models.User;
import play.libs.Json;
import play.mvc.*;


public class AccessTokens extends Controller {

	/**
	 * Get the object node representing an access token
	 */
	private static ObjectNode getAccessTokenObjectNode(AccessToken accessToken) {
		ObjectNode result = Json.newObject();
		
		result.put("token", accessToken.token);
		result.put("autoRenew", accessToken.autoRenew);
		result.put("expiration", accessToken.expiration.getTime());
		result.put("creation", accessToken.creation.getTime());
		
		return result;
	}
	

    /**
     * Add a user
     */
    public static Result add() {
		return TODO;
    }
    
    /**
     * Delete a user
     */
    public static Result delete() {
    	return TODO;
    }
    
    /**
     * Update a user
     */
    public static Result update(Integer id) {
		return TODO;
    }
    
    /**
     * Get access token
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result accessToken() {
    	JsonNode json = request().body().asJson();
    	
    	if (json == null) {
    		return badRequest("Expecting Json data");
		} 
    	
    	String email = json.path("email").textValue();
    	String password = json.path("password").textValue();
    	if (email == null) {
			return badRequest("Missing parameter [email]");
        } else if (password == null) {
        	return badRequest("Missing parameter [password]");
        } else {
        	User user = Users.authenticate(email, password);
        	if (user == null) {
        		return unauthorized("Invalid user or password");
        	}
        	        	
    		boolean autoRenew = json.path("autoRenew").booleanValue();
    		return ok(getAccessTokenObjectNode(AccessToken.create(autoRenew, user)));
        }
    }
    
    /*
     * Get connected user
     */
    public static User connectedUser(String accessToken) {
    	
    	AccessToken token = AccessToken.find.where().eq("token", accessToken).where().gt("expiration", new Date()).setMaxRows(1).findUnique();
    	
    	if (token == null) {
    		return null;
    	}
    	
    	if (token.autoRenew) {
    		token.expiration = new Date((new Date()).getTime() + AccessToken.autoRenewExpirationTime);
    		token.save();
		}
    	return token.user;
    }
}
