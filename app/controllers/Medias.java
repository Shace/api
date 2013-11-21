package controllers;

import java.util.List;

import models.Media;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import models.Event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Controller that handles the different API action applied to the media
 * @author olivie_a
 * @category controllers
 */
public class Medias extends Controller {

	/**
	 * List all the available media.
	 * @return An HTTP Json response containing the properties of all the media
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
     * Add one or several medias into an event.
     * The medias properties are contained into the HTTP Request body as Json format.
     * @param ownerEventToken : The id of the event that will contain the medias
	 * @return An HTTP Json response containing the properties of all the added media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String ownerEventToken, String accessToken) {
    	User	ownerUser = AccessTokens.connectedUser(accessToken);
    	Event	ownerEvent = Event.find.byId(ownerEventToken);
   
    	if (ownerUser == null)
    		return unauthorized("No user connected");
    	else if (ownerEvent == null)
    		return notFound("Event not found");
    	// TODO : Check User rights for this Event (can he add a media?) like this
    	// else if (!Events.hasWriteAccess(ownerUser))
    	// 	return unauthorized("No write access");

    	JsonNode root = request().body().asJson();
    	if (root == null)
    		return badRequest("Unexpected format, JSon required");
    	JsonNode mediaList = root.get("medias");

    	if (mediaList == null)
    		return badRequest("Missing parameter [medias]");
		ArrayNode mediasNode = Json.newObject().arrayNode();
    	for (JsonNode mediaNode : mediaList) {
        	Media newMedia = new Media(ownerUser, ownerEvent);
        	updateOneMedia(newMedia, mediaNode);
        	newMedia.save();
        	mediasNode.add(getMediaObjectNode(newMedia));
		}
    	if (mediasNode.size() == 0)
    		return badRequest("Empty/Invalid media list");
  		ObjectNode result = Json.newObject();
		result.put("medias", mediasNode);
		return created(result);
    }
    
    /**
     * Delete the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    public static Result delete(Integer id) {
    	// TODO : Get connected User instead of this
    	User	currentUser = User.find.findUnique();
    	Media	currentMedia = Media.find.byId(id);

    	if (currentUser == null)
    		return unauthorized("No user connected");
    	else if (currentMedia == null)
    		return notFound("Media not found");
    	Event	currentEvent = currentMedia.ownerEvent;
    	if (currentEvent == null)
    		return notFound("Media not found");
    	// TODO : Check User rights for this Event (can he edit a media?) like this
    	// else if (!Events.hasWriteAccess(ownerUser))
    	// 	return unauthorized("No write access");

       	currentMedia.delete();
		return noContent();
	}
    
    /**
     * Update the media identified by the id parameter.
     * The new media properties are contained into the HTTP Request body as Json format.
     * @param id : the media identifier
	 * @return An HTTP Json response containing the new properties of edited media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(Integer id) {
    	// TODO : Get connected User instead of this
    	User	currentUser = User.find.findUnique();
    	Media	currentMedia = Media.find.byId(id);

    	if (currentUser == null)
    		return unauthorized("No user connected");
    	else if (currentMedia == null)
    		return notFound("Media not found");
    	Event	currentEvent = currentMedia.ownerEvent;
    	if (currentEvent == null)
    		return notFound("Media not found");
    	// TODO : Check User rights for this Event (can he edit a media?) like this
    	// else if (!Events.hasWriteAccess(ownerUser))
    	// 	return unauthorized("No write access");

    	JsonNode root = request().body().asJson();
    	if (root == null)
    		return badRequest("Unexpected format, JSon required");
       	updateOneMedia(currentMedia, root);
       	currentMedia.save();
  		ObjectNode result = Json.newObject();
		result.put("medias", getMediaObjectNode(currentMedia));
		return ok(result);
    }
    
    /**
     * Get the properties of the media identified by the id parameter.
     * @param id : the media identifier
	 * @return An HTTP Json response containing the properties of the specified media
     */
    public static Result media(Integer id) {
    	// TODO : Get connected User instead of this
    	User	currentUser = User.find.findUnique();
    	Media	currentMedia = Media.find.byId(id);

    	if (currentUser == null)
    		return unauthorized("No user connected");
    	else if (currentMedia == null)
    		return notFound("Media not found");
    	Event	currentEvent = currentMedia.ownerEvent;
    	if (currentEvent == null)
    		return notFound("Media not found");
    	// TODO : Check User rights for this Event (can he edit a media?) like this
    	// else if (!Events.hasWriteAccess(ownerUser))
    	// 	return unauthorized("No write access");
    	
   		return ok(getMediaObjectNode(currentMedia));
    }
    
    /**
     * Update the media properties from a Json object.
     * @param currentMedia : The media to update
     * @param currentNode : The new properties to set
     */
    private static void updateOneMedia(Media currentMedia, JsonNode currentNode) {
    	String	name = currentNode.findPath("name").textValue();
    	if (name != null)
    		currentMedia.name = name;
    	String description = currentNode.path("description").textValue();
    	if (description != null)
    		currentMedia.description = description;

    }

	/**
	 * Convert a Media to a Json object.
	 * @param media : A Media object to convert
	 * @return The Json object containing the media information
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
}
