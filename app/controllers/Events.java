package controllers;

import java.util.List;

import models.AccessToken;
import models.Event;
import models.Event.Privacy;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@CORS
public class Events extends Controller {

    /**
     * Convert an event to a JSON object.
     * 
     * @param media An Event object to convert
     * @return The JSON object containing the event information
     */
    public static ObjectNode getEventObjectNode(Event event) {
        ObjectNode result = Json.newObject();

        result.put("token", event.token);
        result.put("name", event.name);
        result.put("description", event.description);
        result.put("creation", event.creation.getTime());

        return result;
    }

    /**
     * List all the visible events.
     * 
     * @return An HTTP JSON response containing the properties of all the events
     */
    public static Result events(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        if (access == null)
            return unauthorized("Not a valid token");

        List<Event> events = Event.find.where().eq("privacy", Event.Privacy.PUBLIC).findList();

        ArrayNode eventsNode = Json.newObject().arrayNode();

        for (Event event : events) {
            eventsNode.add(getEventObjectNode(event));
        }
        ObjectNode result = Json.newObject();
        result.put("events", eventsNode);
        return ok(result);
    }

    /**
     * Add an event.
     * The event properties are contained into the HTTP Request body as JSON format.
     * 
     * @return An HTTP JSON response containing the properties of the added event
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);

        if (access == null)
            return unauthorized("Not a valid token");
        if (!access.isConnectedUser())
            return badRequest("No user connected");
        
        JsonNode json = request().body().asJson();
        if (json == null)
            return badRequest("Unexpected format, JSON required");
        
        String token = json.path("token").textValue();
        String privacy = json.path("privacy").textValue();
        if (token == null) {
            return badRequest("Missing parameter [token]");
        } else if (privacy == null) {
            return badRequest("Missing parameter [privacy]");
        } else if (!privacy.equals("public") && !privacy.equals("protected") && !privacy.equals("private")) {
            return badRequest("[privacy] have to be ine ('public', 'protected', 'private')");
        } else {
            if (Event.find.byId(token) != null) {
                return badRequest("token already exists");
            }
            
            Privacy privacyEnum = (privacy.equals("public")) ? Privacy.PUBLIC : ((privacy.equals("protected")) ? Privacy.PROTECTED : Privacy.PRIVATE);
            Event event = new Event(token, privacyEnum, access.user);
            
            String password = json.path("password").textValue();
            if (event.privacy == Privacy.PROTECTED && password == null) {
                return badRequest("Missing parameter [password] for privacy protected");
            } else if (event.privacy == Privacy.PROTECTED) {
                event.password = Utils.hash(password);
            }
            
            updateOneEvent(event, json);
            
            event.save();
            return ok(getEventObjectNode(event));
        }
    }

    /**
     * Delete the event identified by the token parameter.
     * 
     * @param token : the event identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    public static Result delete(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);

        if (access == null)
            return unauthorized("Not a valid token");
        if (!access.isConnectedUser())
            return badRequest("No user connected");
        else if (access.user.isAdmin == false)
            return forbidden("You need to be admin");
        
        // TODO This is a really tricky operation. All the medias, events, tokens, ... need to be delete !
        // For now, deletion is not yet possible
        // Maybe only set a valid flag
        
        return TODO;
    }

    /**
     * Update the event identified by the token parameter.
     * The new event properties are contained into the HTTP Request body as JSON format.
     * 
     * @param token : the event identifier
     * @return An HTTP JSON response containing the new properties of edited user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);

        if (access == null)
            return unauthorized("Not a valid token");
        if (!access.isConnectedUser())
            return badRequest("No user connected");
        
        Event event = Event.find.byId(token);
        if (event == null) {
            return notFound("Event with token " + token + " not found");
        }
        
        if (access.user.id != event.ownerUser.id && access.user.isAdmin == false)
            return forbidden("Can't update other users");
        
        JsonNode root = request().body().asJson();
        if (root == null)
            return badRequest("Unexpected format, JSON required");
        
        updateOneEvent(event, root);
        event.update();
        
        return ok(getEventObjectNode(event));
    }

    /**
     * Get the properties of the event identified by the token parameter.
     * 
     * @param token : the event identifier
     * @return An HTTP JSON response containing the properties of the specified event
     */
    public static Result event(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);

        if (access == null)
            return unauthorized("Not a valid token");
        if (!access.isConnectedUser())
            return badRequest("No user connected");
        
        Event event = Event.find.byId(token);
        if (event == null) {
            return notFound("Event with token " + token + " not found");
        }
        
        if (event.privacy != Privacy.PUBLIC && access.user.id != event.ownerUser.id && access.user.isAdmin == false)
            return forbidden("Can't view other user's events");
        
        return ok(getEventObjectNode(event));
    }
    
    /**
     * Update the event properties from a JSON object.
     * 
     * @param currentEvent : The event to update
     * @param currentNode : The new properties to set
     */
    private static void updateOneEvent(Event currentEvent, JsonNode currentNode) {
        String name = currentNode.path("name").textValue();
        if (name != null)
            currentEvent.name = name;
        String description = currentNode.path("description").textValue();
        if (description != null)
            currentEvent.description = description;
    }
}
