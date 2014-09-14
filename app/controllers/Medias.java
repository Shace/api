package controllers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.AccessToken;
import models.Bucket;
import models.Comment;
import models.Event;
import models.Image;
import models.UserMediaLikeRelation;
import models.Image.FormatType;
import models.Media;
import models.Tag;
import play.Logger;
import play.db.ebean.Transactional;
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

import errors.Error.ParameterType;
import errors.Error.Type;

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
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
    	}

        error = Access.hasPermissionOnEvent(access, ownerEvent, Access.AccessType.WRITE);
        if (error != null) {
        	return error;
        }        

    	JsonNode root = request().body().asJson();
    	if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
    	}
    	
    	JsonNode mediaList = root.get("medias");
    	if (mediaList == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("medias", ParameterType.REQUIRED).toResponse();
    	}
    	
		ArrayNode mediasNode = Json.newObject().arrayNode();
    	for (JsonNode mediaNode : mediaList) {

        	Media newMedia = new Media(access.user, ownerEvent);
        	updateOneMedia(newMedia, mediaNode);

        	newMedia.save();
        	RequestParameters	params = RequestParameters.create(request());
        	mediasNode.add(mediaToJson(access, ownerEvent, newMedia, params, false));
		}

    	if (mediasNode.size() == 0) {
        	return new errors.Error(errors.Error.Type.EMPTY_MEDIA_LIST).toResponse();
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
    @Transactional
    public static Result delete(String token, Integer id, String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
    	
    	Media	currentMedia = Media.find.fetch("buckets").where().eq("id", id).findUnique();
    	if (currentMedia == null || !currentMedia.valid) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	} else if (!currentMedia.owner.equals(access.user)) {
        	return new errors.Error(errors.Error.Type.NEED_OWNER).toResponse();
    	}

    	List<Bucket> buckets = new ArrayList<>(currentMedia.buckets);
    	currentMedia.buckets.clear();
    	currentMedia.saveManyToManyAssociations("buckets");
    	for (Bucket bucket : buckets) {
    		Bucket currentBucket = Bucket.find.byId(bucket.id);
    		currentBucket.size -= 1;
    		
    		if (currentBucket.size == 0) {
    			if (currentBucket.event.root != currentBucket) {
    				currentBucket.delete();
    			}
    		} else {
    			currentBucket.first = null;
    			currentBucket.last = null;
    			for (Media media: currentBucket.medias) {
    				if (currentBucket.first == null || media.original.getTime() < currentBucket.first.getTime()) {
    					currentBucket.first = media.original;
    				}
    				if (currentBucket.last == null || media.original.getTime() > currentBucket.last.getTime()) {
    					currentBucket.last = media.original;
    				}
    			}
    			currentBucket.save();
    		}
    	}
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
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	} else if (!currentMedia.owner.equals(access.user)) {
        	return new errors.Error(errors.Error.Type.NEED_OWNER).toResponse();
    	}

    	JsonNode root = request().body().asJson();
    	if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
    	}

    	updateOneMedia(currentMedia, root);
       	currentMedia.save();
  		ObjectNode result = Json.newObject();
    	RequestParameters	params = RequestParameters.create(request());
		result.put("medias", mediaToJson(access, currentEvent, currentMedia, params, false));
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
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}

    	error = Access.hasPermissionOnEvent(access, currentEvent, Access.AccessType.READ);
        if (error != null) {
        	return error;
        }

    	RequestParameters	params = RequestParameters.create(request());
   		return ok(mediaToJson(access, currentEvent, currentMedia, params, true));
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
	 * @param full : True will display all information about the media
	 * @return The Json object containing the media information
	 */
	public static ObjectNode mediaToJson(AccessToken access, Event ownerEvent, Media media, RequestParameters params, boolean full) {
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
		if (media.original != null) {
			result.put("original", media.original.getTime());			
		}	    
		result.put("image", Images.getImageObjectNode(media.image));
		
		if (full) {
		    ArrayNode comments = result.putArray("comments");
	        for (Comment comment : media.comments) {
                comments.add(Comments.commentToJson(access, ownerEvent, comment, null));
	        }
	        
	        ArrayNode tags = result.putArray("tags");
            for (Tag tag : media.tags) {
                tags.add(Tags.tagToJson(access, ownerEvent, tag, null));
            }
		}
		
		return result;
	}
	
	/**
     * Add a file to the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the file upload success
     */
    public static Result addFile(String token, Integer id, String accessToken) {
        long startTime = System.nanoTime();

        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}

        if (access.user.equals(currentMedia.owner)) {
            MultipartFormData body = request().body().asMultipartFormData();
            FilePart filePart = null; 
            if (body != null)
                filePart = body.getFile("file");
            if (filePart != null) {
              File file = filePart.getFile();
              try {
                currentMedia.image.addFile(file, FormatType.GALLERY);
              } catch (Image.BadFormat b) {
              	return new errors.Error(errors.Error.Type.BAD_FORMAT_IMAGE).toResponse();
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
            } else {
              	return new errors.Error(errors.Error.Type.BAD_FORMAT_IMAGE).toResponse();
            }
            
        } else {
        	return new errors.Error(errors.Error.Type.NEED_OWNER).toResponse();
        }

        currentMedia.update();

        //Buckets.addNewMediaToEvent(currentEvent, currentMedia);
        BucketsUpdater.get().updateBucket(currentEvent, currentMedia);
        
        long estimatedTime = System.nanoTime() - startTime;
        Logger.debug("Time elapsed to add file : " + Long.toString(estimatedTime / 1000000) + "ms");

        return noContent();
    }
    
    /**
     * Like the media identified by the id parameter.
     * The new media properties are contained into the HTTP Request body as Json format.
     * @param id : the media identifier
	 * @return An HTTP Success or an error
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result like(String token, Integer id, String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	} else if (!currentMedia.owner.equals(access.user)) {
        	return new errors.Error(errors.Error.Type.NEED_OWNER).toResponse();
    	}

    	if (UserMediaLikeRelation.find.where().eq("media_id", currentMedia.id).findUnique() == null) {
    		UserMediaLikeRelation.create(access.user, currentMedia);
    		currentMedia.rank += 1;
       		currentMedia.save();
    	}
		return ok();
    }
    
    /**
     * Like the media identified by the id parameter.
     * The new media properties are contained into the HTTP Request body as Json format.
     * @param id : the media identifier
	 * @return An HTTP Success or an error
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result unlike(String token, Integer id, String accessToken) {
    	AccessToken	access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

    	Media	currentMedia = Media.find.byId(id);
    	if (currentMedia == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	}
    	
    	Event	currentEvent = currentMedia.event;
    	if (currentEvent == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
    	} else if (!currentMedia.owner.equals(access.user)) {
        	return new errors.Error(errors.Error.Type.NEED_OWNER).toResponse();
    	}

		UserMediaLikeRelation like = UserMediaLikeRelation.find.where().eq("media_id", currentMedia.id).eq("user_id", access.user.id).findUnique();
    	if (like != null) {
    		like.delete();
    		currentMedia.rank -= 1;
    		currentMedia.save();
    	}
		return noContent();
    }

}
