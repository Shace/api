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

public class Events extends Controller {

	/**
	 * List all visible events
	 */
    public static Result events() {
    	List<Event> events = Event.find.where()
    	.eq("privacy", Privacy.PUBLIC)
        .findList();
    	
    	ArrayNode eventsNode = Json.newObject().arrayNode();

    	for (Event event : events) {
    		ObjectNode result = Json.newObject();
    		result.put("token", event.token);
    		result.put("name", event.name);
    		result.put("description", event.description);
    		result.put("id", event.id);
    		result.put("creation", event.creation.getTime());
    		eventsNode.add(result);
    	}
    	ObjectNode result = Json.newObject();
    	result.put("events", eventsNode);
    	return ok(result);
    }

    /**
     * Add an event
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add() {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete an event
     */
    public static Result delete(String token) {
    	return TODO;
    }
    
    /**
     * Update an event
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String token) {
    	JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get event information
     */
    public static Result event(String token) {
    	return TODO;
    }
}
