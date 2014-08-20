package controllers;

import Utils.Access;
import Utils.BucketsUpdater;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
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
import models.AccessToken;
import models.AccessTokenEventRelation;
import models.Event;
import models.Image;
import models.Event.Privacy;
import models.Image.FormatType;
import models.ImageFileRelation;
import models.Media;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Controller that handles the different API action applied to an Event
 * @author Leandre Deguerville
 * @category controllers
 */
@CORS
public class Events extends Controller {

    /**
     * Convert an event to a JSON object.
     * 
     * @param event An Event object to convert
     * @return The JSON object containing the event information
     */
    public static ObjectNode getEventObjectNode(Event event, AccessToken accessToken) {
        ObjectNode result = Json.newObject();

        result.put("token", event.token);
        result.put("name", event.name);
        result.put("description", event.description);
        result.put("creation", event.creation.getTime());
        result.put("privacy", event.readingPrivacy.toString().toLowerCase());
        result.put("permission", Access.getPermissionOnEvent(accessToken, event).toString());
        if (event.coverImage != null) {
        	result.put("cover", Images.getImageObjectNode(event.coverImage));
        }
        
        ArrayNode medias = result.putArray("medias");
        for (Media media : event.medias) {
            if (media.image != null && media.image.files != null) {
                if (media.image.files.size() > 0)
                    medias.add(Medias.mediaToJson(accessToken, event, media, null, false));
            }
        }
        
        result.put("bucket", Buckets.getBucketObjectNode(accessToken, event, event.root));

        return result;
    }

    /**
     * List all the visible events.
     * 
     * @return An HTTP JSON response containing the properties of all the events
     */
    public static Result events(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
        	return error;
        }

        // TODO privacy => readingPrivacy
        List<Event> events = Event.find.where().eq("privacy", Event.Privacy.PUBLIC).findList();

        ArrayNode eventsNode = Json.newObject().arrayNode();

        for (Event event : events) {
            eventsNode.add(getEventObjectNode(event, access));
        }
        ObjectNode result = Json.newObject();
        result.put("events", eventsNode);
        return ok(result);
    }

    /**
     * Search for an event.
     *
     * TODO The user should also be able to search for ALL events he joined (private and protected as well) (if he's logged)
     * TODO Change the orderBy to return the most popular events first (If $query match an event at 100%, this event should appears first, followed by the most popular ones )
     * TODO Throw PARAMETERS_ERROR if query length is 0
     *
     * @param query query to lookup
     * @param accessToken client access token
     *
     * @return An HTTP JSON response containing the properties of all the events
     */
    public static Result search(String query, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
        	return error;
        }

        List<Event> events = Event.find.where().eq("readingPrivacy", Event.Privacy.PUBLIC)
                                       .where().istartsWith("token", query)
                                       .orderBy("token")
                                       .setMaxRows(20)
                                       .findList();

        ArrayNode eventsNode = Json.newObject().arrayNode();

        for (Event event : events) {
            eventsNode.add(getEventObjectNode(event, access));
        }
        ObjectNode result = Json.newObject();
        result.put("events", eventsNode);
        return ok(result);
    }

    /**
     * List of all forbidden tokens
     */
    private static final List<String> forbiddenTokens = Arrays.asList("search");
    
    /**
     * Add an event.
     * The event properties are contained into the HTTP Request body as JSON format.
     * 
     * @return An HTTP JSON response containing the properties of the added event
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
        JsonNode json = request().body().asJson();
        if (json == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        Privacy readingPrivacy = Privacy.NOT_SET;
        String 	token = null;
        String	readingPassword = null;
        
        String readingPrivacyStr = json.path("privacy").textValue();
        Event event = null;

        if (readingPrivacyStr == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("readingPrivacy", ParameterType.REQUIRED).toResponse();
        }
        if (readingPrivacyStr.equals("public")) {
            token = json.path("token").textValue();

            if (token == null) {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.REQUIRED).toResponse();
            } else if (Event.find.where().eq("token", token).findUnique() != null) {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.DUPLICATE).toResponse();
            }
            readingPrivacy = Privacy.PUBLIC;
        } else if (readingPrivacyStr.equals("protected")) {
            token = json.path("token").textValue();
            readingPassword = json.path("password").textValue();

            if (token == null) {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.REQUIRED).toResponse();
            } else if (Event.find.where().eq("token", token).findUnique() != null) {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.DUPLICATE).toResponse();
            } else if (readingPassword == null) {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("password", ParameterType.REQUIRED).toResponse();
            }
            readingPrivacy = Privacy.PROTECTED;
            readingPassword = Utils.Hasher.hash(readingPassword);
        } else if (readingPrivacyStr.equals("private")) {
        	readingPrivacy = Privacy.PRIVATE;
        } else {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("readingPrivacy", ParameterType.FORMAT).toResponse();
        }
        
        event = new Event(readingPrivacy, access.user);
        if (token != null) {
        	if (forbiddenTokens.contains(token)) {
            	return new errors.Error(Type.FORBIDDEN_TOKEN).toResponse();
        	} else if (!token.matches("[a-zA-Z0-9|-]*")) {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.FORMAT).toResponse();
        	}
        	event.token = token;
        }
        if (readingPassword != null) {
        	event.readingPassword = readingPassword;
        }
        
        String name = json.path("name").textValue();
        if (name == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("name", ParameterType.REQUIRED).toResponse();
        }
        event.name = name;

        fillEventFromJSON(event, json);
        event.save();

        event.root.event = event;
        event.root.save();
        
        event.saveOwnerPermission();
        return created(getEventObjectNode(event, access));
    }

    /**
     * Delete the event identified by the token parameter.
     * 
     * @param token : the event identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    public static Result delete(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
        	return error;
        }

        Event event = Event.find.where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }
        
        error = Access.hasPermissionOnEvent(access, event, Access.AccessType.ROOT);
        if (error != null) {
        	return error;
        }

        // TODO This is a really tricky operation. All the medias, events, tokens, files ... need to be delete !
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
    public static Result access(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
        	return error;
        }

        Event event = Event.find.where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }

        if (event.writingPrivacy != Event.Privacy.PROTECTED && event.readingPrivacy != Event.Privacy.PROTECTED) {
        	return new errors.Error(errors.Error.Type.NO_PASSWORD).toResponse();
        }
        
        String password = root.path("password").textValue();
        if (password == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("password", ParameterType.REQUIRED).toResponse();
        }
        
        password = Utils.Hasher.hash(password);
        Access.AccessType grantedAccess = Access.AccessType.NONE;
        if (event.writingPrivacy == Event.Privacy.PROTECTED && password.equals(event.writingPassword)) {
        	grantedAccess = Access.AccessType.WRITE;        		
        } else if (event.readingPrivacy == Event.Privacy.PROTECTED && password.equals(event.readingPassword)) {
        	if (event.writingPrivacy == Event.Privacy.NOT_SET) {
            	grantedAccess = Access.AccessType.WRITE;
        	} else {
            	grantedAccess = Access.AccessType.READ;
        	}
        } else {
        	return new errors.Error(errors.Error.Type.WRONG_PASSWORD).toResponse();
        }
        
		AccessTokenEventRelation newAccess = AccessTokenEventRelation.find.where().eq("accessToken", access).eq("event", event).findUnique();

		if (newAccess == null) {
			newAccess = new AccessTokenEventRelation(event, access, grantedAccess);
		} else {
			newAccess.permission = grantedAccess;
		}
        newAccess.save();
        return ok();
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
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        Event event = Event.find.where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }
        
        error = Access.hasPermissionOnEvent(access, event, Access.AccessType.ADMINISTRATE);
        if (error != null) {
        	return error;
        }
        
        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        fillEventFromJSON(event, root);
        
        String readingPrivacyStr = root.path("privacy").textValue();
        if (readingPrivacyStr != null) {
        	if (readingPrivacyStr.equals("public")) {
        		token = root.path("token").textValue();

        		if (event.readingPrivacy == Privacy.PRIVATE && token == null) {
                	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.REQUIRED).toResponse();
        		} else if (token != null && !token.equals(event.token) && Event.find.where().eq("token", token).findUnique() != null) {
                	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.DUPLICATE).toResponse();
        		} else {
        			event.readingPrivacy = Privacy.PUBLIC;
        			if (token != null) {
        				event.token = token;
        			}
        		}
        	} else if (readingPrivacyStr.equals("protected")) {
        		token = root.path("token").textValue();

        		if (event.readingPrivacy == Privacy.PRIVATE && token == null) {
                	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.REQUIRED).toResponse();
        		} else if (token != null && !token.equals(event.token) && Event.find.where().eq("token", token).findUnique() != null) {
                	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.DUPLICATE).toResponse();
        		}
   
        		String readingPassword = root.path("password").textValue();
        		if (readingPassword == null) {
                	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("password", ParameterType.REQUIRED).toResponse();
        		}
        		
        		event.readingPrivacy = Privacy.PROTECTED;
        		if (token != null) {
        			event.token = token;
        		}
        		event.readingPassword = Utils.Hasher.hash(readingPassword);

        		Access.AccessType toDelete = (event.writingPrivacy == Event.Privacy.NOT_SET) ? Access.AccessType.WRITE : Access.AccessType.READ;
            	Ebean.delete(AccessTokenEventRelation.find.where().eq("event", event).eq("permission", toDelete).findList());

        	} else if (readingPrivacyStr.equals("private")) {
        		event.readingPrivacy = Privacy.PRIVATE;
        		event.token = event.id;
        	} else {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("privacy", ParameterType.FORMAT).toResponse();
        	}
        }
        String writingPrivacyStr = root.path("writingPrivacy").textValue();
        if (writingPrivacyStr != null) {
        	if (writingPrivacyStr.equals("public")) {
        		event.writingPrivacy = Privacy.PUBLIC;
        	} else if (writingPrivacyStr.equals("protected")) {
        		String writingPassword = root.path("writingPassword").textValue();
        		if (writingPassword == null) {
                	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("writingPassword", ParameterType.REQUIRED).toResponse();
        		}
        		if (event.writingPrivacy == Event.Privacy.NOT_SET && event.readingPrivacy == Event.Privacy.PROTECTED) {
        			List<AccessTokenEventRelation> permissions = AccessTokenEventRelation.find.where().eq("event", event).eq("permission", Access.AccessType.WRITE).findList();
        			for (AccessTokenEventRelation permission : permissions) {
        				permission.permission = Access.AccessType.READ;
        				permission.update();
        			}
        		} else {
                	Ebean.delete(AccessTokenEventRelation.find.where().eq("event", event).eq("permission", Access.AccessType.WRITE).findList());
        		}
        		event.writingPrivacy = Privacy.PROTECTED;
        		event.writingPassword = Utils.Hasher.hash(writingPassword);
        	} else if (writingPrivacyStr.equals("private")) {
        		event.writingPrivacy = Privacy.PRIVATE;
        	} else {
            	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("readingPrivacy", ParameterType.FORMAT).toResponse();
        	}

        	if (event.readingPrivacy.compareTo(event.writingPrivacy) > 0) {
            	return new errors.Error(errors.Error.Type.READING_TOO_STRONG).toResponse();
            }
        }

        if (forbiddenTokens.contains(event.token)) {
        	return new errors.Error(Type.FORBIDDEN_TOKEN).toResponse();
    	} else if (!event.token.matches("[a-zA-Z0-9|-]*")) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.FORMAT).toResponse();
    	}
        event.update();
        return ok(getEventObjectNode(event, access));
    }

    /**
     * Get the properties of the event identified by the token parameter.
     * 
     * @param token : the event identifier
     * @return An HTTP JSON response containing the properties of the specified event
     */
    public static Result event(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
            return error;
        }

        Event event = Ebean.find(Event.class).fetch("medias").orderBy("original asc").fetch("medias.owner").fetch("medias.image")
                .fetch("medias.image.files").fetch("medias.image.files.file").fetch("root").fetch("coverImage").fetch("coverImage.files").fetch("coverImage.files.file").where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        error = Access.hasPermissionOnEvent(access, event, Access.AccessType.READ);
        if (error != null) {
            return error;
        }
        return ok(getEventObjectNode(event, access));
    }
    
    /**
     * Update the event properties from a JSON object.
     * 
     * @param currentEvent : The event to update
     * @param currentNode : The new properties to set
     */
    private static void fillEventFromJSON(Event currentEvent, JsonNode currentNode) {
        String name = currentNode.path("name").textValue();
        if (name != null)
            currentEvent.name = name;
        String description = currentNode.path("description").textValue();
        if (description != null)
            currentEvent.description = description;
    }
    
    /**
     * Add a file to the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the file upload success
     */
    public static Result addCover(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
    	
        Event event = Event.find.where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

    	error = Access.hasPermissionOnEvent(access, event, Access.AccessType.ADMINISTRATE);
        if (error != null) {
        	return error;
        }
        
        
        MultipartFormData body = request().body().asMultipartFormData();
        FilePart filePart = null; 
        if (body != null) {
            filePart = body.getFile("file");
        }
        if (filePart != null) {
          File file = filePart.getFile();
          try {
        	  String s = "DELETE FROM se_image_file_relation where image_id = :imageid";
              SqlUpdate update = Ebean.createSqlUpdate(s);
              update.setParameter("imageid", event.coverImage.id);
              Ebean.execute(update);
              event.coverImage.addFile(file, FormatType.EVENT_COVER);
          } catch (Image.BadFormat b) {
          	return new errors.Error(errors.Error.Type.BAD_FORMAT_IMAGE).toResponse();
          }
        }

        event.update();

        return noContent();
    }
}
