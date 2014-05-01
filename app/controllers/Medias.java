package controllers;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import models.AccessToken;
import models.Event;
import models.Image;
import models.Media;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import Utils.Access;
import Utils.BucketsUpdater;
import Utils.RequestParameters;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Controller that handles the different API action applied to the media
 * @author olivie_a
 * @category controllers
 */
@CORS
public class Medias extends Controller {
    /**
     * Add one or several medias into an event.
     * The medias properties are contained into the HTTP Request body as Json format.
     * @param ownerEventToken : The id of the event that will contain the medias
	 * @return An HTTP Json response containing the properties of all the added media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String ownerEventToken, String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        Event		ownerEvent = Event.find.where().eq("token", ownerEventToken).findUnique();
    	if (ownerEvent == null) {
    		return notFound("Event not found");
    	}

        error = Access.hasPermissionOnEvent(access, ownerEvent, Event.AccessType.WRITE);
        if (error != null) {
        	return error;
        }        

    	JsonNode root = request().body().asJson();
    	if (root == null) {
    		return badRequest("Unexpected format, JSon required");
    	}
    	
    	JsonNode mediaList = root.get("medias");
    	if (mediaList == null) {
    		return badRequest("Missing parameter [medias]");
    	}
    	
		ArrayNode mediasNode = Json.newObject().arrayNode();
    	for (JsonNode mediaNode : mediaList) {

        	Media newMedia = new Media(access.user, ownerEvent);
        	updateOneMedia(newMedia, mediaNode);

        	newMedia.save();
        	RequestParameters	params = RequestParameters.create(request());
        	mediasNode.add(mediaToJson(newMedia, params));
		}

    	if (mediasNode.size() == 0) {
    		return badRequest("Empty/Invalid media list");
    	}

    	ObjectNode result = Json.newObject();
		result.put("medias", mediasNode);
		return created(result);
    }
    
    /**
     * Delete the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    public static Result delete(String token, Integer id, String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
    	
    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
    		return notFound("Media not found");
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
    		return notFound("Media not found");
    	} else if (!currentMedia.owner.equals(access.user)) {
    		return forbidden("Permission Denied");
    	}

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
    public static Result update(String token, Integer id, String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
    		return notFound("Media not found");
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
    		return notFound("Media not found");
    	} else if (!currentMedia.owner.equals(access.user)) {
    		return forbidden("Permission Denied");
    	}

    	JsonNode root = request().body().asJson();
    	if (root == null) {
    		return badRequest("Unexpected format, JSon required");
    	}

    	updateOneMedia(currentMedia, root);
       	currentMedia.save();
  		ObjectNode result = Json.newObject();
    	RequestParameters	params = RequestParameters.create(request());
		result.put("medias", mediaToJson(currentMedia, params));
		return ok(result);
    }
    
    /**
     * Get the properties of the media identified by the id parameter.
     * @param id : the media identifier
	 * @return An HTTP Json response containing the properties of the specified media
     */
    public static Result media(String token, Integer id, String accessToken) {
       	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
        	return error;
        }

    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
    		return notFound("Media not found");
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
    		return notFound("Media not found");
    	}

    	error = Access.hasPermissionOnEvent(access, currentEvent, Event.AccessType.READ);
        if (error != null) {
        	return error;
        }

    	RequestParameters	params = RequestParameters.create(request());
   		return ok(mediaToJson(currentMedia, params));
    }
    
    /**
     * Update the media properties from a Json object.
     * @param currentMedia : The media to update
     * @param currentNode : The new properties to set
     */
    private static void updateOneMedia(Media currentMedia, JsonNode currentNode) {
    	String	name = currentNode.path("name").textValue();
    	if (name != null)
    		currentMedia.name = name;
    	String description = currentNode.path("description").textValue();
    	if (description != null)
    		currentMedia.description = description;
    }

	/**
	 * Convert a Media to a Json object.
	 * @param media : A Media object to convert
	 * @param fields 
	 * @param depth
	 * @return The Json object containing the media information
	 */
	public static ObjectNode mediaToJson(Media media, RequestParameters params) {
//		JSONSerializer tmp = new JSONSerializer();
		ObjectNode result = Json.newObject();

		result.put("id", media.id);
		result.put("name", media.name);
		result.put("type", media.type.toString());
		result.put("description", media.description);
		result.put("rank", media.rank);
		result.put("owner", media.owner.id);
		result.put("event", media.event.token);
		result.put("creation", media.creation.getTime());
		result.put("image", Images.getImageObjectNode(media.image));
		
		return result;
	}
	
	/**
     * Add a file to the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the file upload success
     */
    public static Result addFile(String token, Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
    		return notFound("Media not found");
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
    		return notFound("Media not found");
    	}

        if (access.user.equals(currentMedia.owner)) {
            MultipartFormData body = request().body().asMultipartFormData();
            FilePart filePart = body.getFile("file");
            if (filePart != null) {
              File file = filePart.getFile();
              try {
                currentMedia.image.addFile(file);
              } catch (Image.BadFormat b) {
                  return badRequest("Bad format image " + b.getMessage());
              }
              
              /*
               * Get all EXIF information
               */
              try {
                  Metadata metadata = ImageMetadataReader.readMetadata(file);
                  Directory directory = metadata.getDirectory(ExifSubIFDDirectory.class);
                  if (directory == null) {
                      directory = metadata.getDirectory(ExifIFD0Directory.class);
                  }
                  if (directory != null) {
                      currentMedia.original = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                  }
              } catch (ImageProcessingException e) {
              } catch (IOException e) {
              }
              if (currentMedia.original == null) {
                  currentMedia.original = new Date();
              }
            }
            
        } else {
            return forbidden("Only the owner can edit a media");
        }

        currentMedia.update();

        //Buckets.addNewMediaToEvent(currentEvent, currentMedia);
        BucketsUpdater.get().updateBucket(currentEvent, currentMedia);
        return noContent();
    }
}
