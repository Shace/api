package controllers;

import java.util.List;

import models.AccessToken;
import models.Media;
import models.User;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.*;

public class Medias extends Controller {

	/**
	 * Get the object node representing a media
	 */
	public static ObjectNode getMediaObjectNode(Media media) {
		ObjectNode result = Json.newObject();
		
		result.put("id", media.id);
		result.put("name", media.name);
		result.put("type", media.type.toString());
		result.put("description", media.description);
		result.put("uri", media.uri.toString());
		result.put("rank", media.rank);
		result.put("owner", media.ownerUser.id);
		result.put("owner", media.ownerEvent.token);
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
    public static Result add(String accessToken) {
    	User connectedUser = AccessTokens.connectedUser(accessToken);
    	
    	if (connectedUser == null) {
    		return unauthorized("Not a connected user");
    	}
    	//JsonNode json = request().body().asJson();
		return TODO;
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
    	//JsonNode json = request().body().asJson();
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
