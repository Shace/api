package controllers;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.Event;
import models.Privacy;
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
    	return TODO;
    }

    /**
     * Add an user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add() {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete an user
     */
    public static Result delete(String email) {
    	return TODO;
    }
    
    /**
     * Update an user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String email) {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get user information
     */
    public static Result user(String email) {
    	return TODO;
    }
}
