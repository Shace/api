package controllers;

import java.util.List;

import models.Event;
import models.Media;
import models.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.*;

public class Medias extends Controller {

	/**
	 * Get the object node representing a media
	 */
	private static ObjectNode getMediaObjectNode(Media media) {
		ObjectNode result = Json.newObject();
		
		result.put("id", media.id);
		result.put("name", media.name);
		result.put("type", media.type.toString());
		result.put("description", media.description);
		result.put("uri", media.uri.toString());
		result.put("rank", media.rank);
		result.put("userOwner", media.ownerUser.id);
		result.put("eventOwner", media.ownerEvent.token);
		result.put("creation", media.creation.getTime());
		
		return result;
	}

	/**
	 * List all visible medias
	 */
	public static Result medias() {
		List<Media> medias = Media.find.findList();

		ArrayNode mediasNode = Json.newObject().arrayNode();

		for (Media media : medias) {
			mediasNode.add(getMediaObjectNode(media));
		}
		ObjectNode result = Json.newObject();
		result.put("medias", mediasNode);
		return ok(result);
	}

    /**
     * Add a media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String ownerEventToken) {
    	// TODO : Get connected User instead of this
    	User	ownerUser = User.find.findUnique();
    	Event	ownerEvent = Event.find.byId(ownerEventToken);
   
    	if (ownerUser == null)
    		return unauthorized("No user connected");
    	else if (ownerEvent == null)
    		return notFound("Event not found");
    	// TODO : Check User rights for this Event (can he add a media?) like this
    	// else if (!Events.hasRightAccess(ownerUser))
    	// 	return unauthorized("No write access");

    	JsonNode root = request().body().asJson();
    	if (root == null)
    		return badRequest("Unexpected format, JSon required");
    	JsonNode mediaList = root.get("medias");

    	if (mediaList == null)
    		return badRequest("Missing parameter [medias]");
		ArrayNode mediasNode = Json.newObject().arrayNode();
    	for (JsonNode mediaNode : mediaList) {
        	String	name = mediaNode.findPath("name").textValue();
        	// TODO : Check behavior when one of the media is invalid : send an error, skip ... ?
        	if (name == null)
        		continue ;
        	Media newMedia = new Media(name, ownerUser, ownerEvent);
        	String description = mediaNode.path("description").textValue();
        	if (description != null)
        		newMedia.description = description;
        	newMedia.save();
        	mediasNode.add(getMediaObjectNode(newMedia));
		}
    	if (mediasNode.size() == 0)
    		return badRequest("Empty/Invalid media list");
  		ObjectNode result = Json.newObject();
		result.put("medias", mediasNode);
		return ok(result);
    }
    
    /**
     * Delete a media
     */
    public static Result delete(Integer id) {
    	return TODO;
    }
    
    /**
     * Update a media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(Integer id) {
    	return TODO;
    }
    
    /**
     * Get media information
     */
    public static Result media(Integer id) {
    	Media media = Media.find.byId(id);
        
    	if (media != null) {
    		return ok(getMediaObjectNode(media));
    	} else {
    		return notFound("Media with id " + id + " not found");
    	}
    }
}
