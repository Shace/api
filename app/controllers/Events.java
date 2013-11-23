package controllers;

import java.util.List;

import models.Event;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@CORS
public class Events extends Controller {
    
	/**
	 * Get the object node representing an event
	 */
	public static ObjectNode getEventObjectNode(Event event) {
		ObjectNode result = Json.newObject();
		
		result.put("token", event.token);
		result.put("name", event.name);
		result.put("description", event.description);
		result.put("id", event.id);
		result.put("creation", event.creation.getTime());
		
		return result;
	}
	
	/**
	 * List all visible events
	 */
    public static Result events(String accessToken) {
    	List<Event> events = Event.find.where()
    	.eq("privacy", Event.Privacy.PUBLIC)
        .findList();
    	
    	ArrayNode eventsNode = Json.newObject().arrayNode();

    	for (Event event : events) {
    		eventsNode.add(getEventObjectNode(event));
    	}
    	ObjectNode result = Json.newObject();
    	result.put("events", eventsNode);
    	return ok(result);
    }

    /**
     * Add an event
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete an event
     */
    public static Result delete(String token, String accessToken) {
    	return TODO;
    }
    
    /**
     * Update an event
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String token, String accessToken) {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get event information
     */
    public static Result event(String token, String accessToken) {
    	return TODO;
    }
}
