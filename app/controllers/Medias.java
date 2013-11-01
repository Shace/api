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

public class Medias extends Controller {

	/**
	 * List all visible medias
	 */
    public static Result medias() {
    	return TODO;
    }

    /**
     * Add a media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add() {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete a media
     */
    public static Result delete(String uri) {
    	return TODO;
    }
    
    /**
     * Update a media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String uri) {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get media information
     */
    public static Result media(String uri) {
    	return TODO;
    }
}
