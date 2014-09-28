package controllers;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import models.AccessToken;
import models.AccessTokenEventRelation;
import models.Event;
import models.Event.LinkAccess;
import models.Event.Privacy;
import models.Image;
import models.Image.BadFormat;
import models.Image.FormatType;
import models.ImageFileRelation;
import models.Media;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import Utils.Access;
import Utils.Storage;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import errors.Error.ParameterType;
import errors.Error.Type;

/**
 * Controller that handles the different API action applied to an Event
 * @author Leandre Deguerville
 * @category controllers
 */
@CORS
public class Events extends Controller {
	
	/**
	 * List of all forbidden tokens
	 */
	private static final List<String> FORBIDDEN_TOKENS = Arrays.asList("search");
	
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
	@Transactional
	public static Result search(String query, String accessToken) {
		AccessToken access = AccessTokens.access(accessToken);
		Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
		if (error != null) {
			return error;
		}

		List<Event> events = Event.find.where()
				.or(Expr.eq("readingPrivacy", Event.Privacy.PUBLIC),
					Expr.eq("readingPrivacy", Event.Privacy.PROTECTED))
				.where().istartsWith("token", query)
				.orderBy("token")
				.setMaxRows(20)
				.findList();

		ArrayNode eventsNode = Json.newObject().arrayNode();

		for (Event event : events) {
			eventsNode.add(getEventObjectNode(event, access, false));
		}
		ObjectNode result = Json.newObject();
		result.put("events", eventsNode);
		return ok(result);
	}

    /**
     * Convert an event to a JSON object.
     * 
     * @param event An Event object to convert
     * @return The JSON object containing the event information
     */
    public static ObjectNode getEventObjectNode(Event event, AccessToken accessToken, boolean full) {
        ObjectNode result = Json.newObject();

        result.put("id", event.id);
        result.put("token", event.token);
        result.put("name", event.name);
        result.put("description", event.description);
        result.put("creation", event.creation.getTime());
        result.put("privacy", event.readingPrivacy.toString().toLowerCase());
        result.put("permission", Access.getPermissionOnEvent(accessToken, event).toString());
        if (event.readingPrivacy == Privacy.PRIVATE) {
        	result.put("link_access", event.linkAccess.toString().toLowerCase());
        }
        result.put("privacy", event.readingPrivacy.toString().toLowerCase());
        if (event.startDate != null)
        	result.put("start_date", event.startDate.getTime());
        if (event.finishDate != null)
        	result.put("finish_date", event.finishDate.getTime());
        if (event.coverImage != null) {
        	result.put("cover", Images.getImageObjectNode(event.coverImage));
        }
        
        if (full) {
	        ArrayNode medias = result.putArray("medias");
	        if (event.medias != null) {
		        for (Media media : event.medias) {
		            if (media.image != null && media.image.files != null) {
		                if (media.image.files.size() > 0)
		                    medias.add(Medias.mediaToJson(accessToken, event, media, false));
		            }
		        }
	        }
	        
	        result.put("bucket", Buckets.getBucketObjectNode(accessToken, event, event.root));
        } else {
            result.put("first_picture", (event.root.first != null) ? (event.root.first.getTime()) : (null));
            result.put("last_picture", (event.root.last != null) ? (event.root.last.getTime()) : (null));        	
        }

        return result;
    }

    /**
     * Add an event.
     * The event properties are contained into the HTTP Request body as JSON format.
     * 
     * @return An HTTP JSON response containing the properties of the added event
     */
    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
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
        	if (FORBIDDEN_TOKENS.contains(token)) {
            	return new errors.Error(Type.FORBIDDEN_TOKEN).toResponse();
        	} else if (!token.matches("[a-zA-Z0-9|-]*") || token.length() < 1) {
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
        return created(getEventObjectNode(event, access, true));
    }

    /**
<<<<<<< HEAD
     * Delete the event identified by the token parameter.
     * 
     * @param token : the event identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
	@Transactional    
    public static Result delete(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
        	return error;
        }

        Event event = Event.find.fetch("medias").fetch("root").where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }
        
        error = Access.hasPermissionOnEvent(access, event, Access.AccessType.ROOT);
        if (error != null) {
        	return error;
        }
        
        event.deleted = true;
        event.token = UUID.randomUUID().toString();
        event.readingPrivacy = Privacy.PRIVATE;
        event.writingPrivacy = Privacy.PRIVATE;
        Ebean.delete(event.permissions);
        event.permissions.clear();
        event.update();
//        List<Image> images = new LinkedList<Image>();
//        images.add(event.coverImage);
//        for (Media media : event.medias) {
//        	images.add(media.image);
//        	media.deleteManyToManyAssociations("buckets");
//        }
//        List<ImageFileRelation> fileRelations = ImageFileRelation.find.fetch("file").where().in("image", images).findList();
//        for (ImageFileRelation fileRelation : fileRelations) {
//        	Storage.deleteFile(fileRelation.file);
//        }
//        Ebean.delete(event.medias);
//        List<Bucket> buckets = Bucket.find.where().eq("event", event).findList();
//        event.root = null;
//        event.save();
//        Ebean.delete(buckets);
//        event.delete();
        return noContent();
    }

    /**
     * Update the event identified by the token parameter.
     * The new event properties are contained into the HTTP Request body as JSON format.
     * 
     * @param token : the event identifier
     * @return An HTTP JSON response containing the new properties of edited user
     */
    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
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
		return ok(getEventObjectNode(event, access, true));
	}

	/**
	 * Get the properties of the event identified by the token parameter.
	 * 
	 * @param token : the event identifier
	 * @return An HTTP JSON response containing the properties of the specified event
	 */
	@Transactional
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
		return ok(getEventObjectNode(event, access, true));
	}

	/**
	 * Add a file to the media identified by the id parameter.
	 * @param id : the media identifier
	 * @return An HTTP response that specifies if the file upload success
	 */
	@Transactional
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
				event.coverImage.owner = access.user;
				Images.replaceImage(event.coverImage, file, FormatType.COVER);				
//				addCoverFile(event, file);
				event.coverImage.save();
			} catch (Image.BadFormat b) {
				return new errors.Error(errors.Error.Type.BAD_FORMAT_IMAGE).toResponse();
			}
		}

		event.update();

		return ok(Images.getImageObjectNode(event.coverImage));
	}

//	@Transactional
//	public static Result addCoverFile(Event event, File file) throws BadFormat {
//
//		List<ImageFileRelation> fileRelations = ImageFileRelation.find.fetch("file").where().eq("image", event.coverImage).findList();
//		for (ImageFileRelation fileRelation : fileRelations) {
//			Storage.deleteFile(fileRelation.file);
//		}
//		Ebean.delete(fileRelations);
//		event.coverImage.addFile(file, FormatType.COVER);
//
//		event.update();
//
//		return ok(Images.getImageObjectNode(event.coverImage));
//	}
    
    /**
     * Update the event identified by the token parameter.
     * The new event properties are contained into the HTTP Request body as JSON format.
     * 
     * @param token : the event identifier
     * @return An HTTP JSON response containing the new properties of edited user
     */
    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
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
        		event.linkAccess = LinkAccess.NONE;
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

        if (FORBIDDEN_TOKENS.contains(event.token)) {
        	return new errors.Error(Type.FORBIDDEN_TOKEN).toResponse();
    	} else if (!event.token.matches("[a-zA-Z0-9|-]*") || event.token.length() < 1) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("token", ParameterType.FORMAT).toResponse();
    	}
        fillEventFromJSON(event, root);
        event.update();
        return ok(getEventObjectNode(event, access, true));
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
        if (currentNode.path("start_date").canConvertToLong()) {
            Long dateTime = currentNode.path("start_date").asLong();
            if (dateTime == 0) {
            	currentEvent.startDate = null;
            } else {
	            Date startDate = new Date(dateTime);
	            currentEvent.startDate = startDate;
            }
        }
        if (currentNode.path("finish_date").canConvertToLong()) {
        	Long dateTime = currentNode.path("finish_date").asLong();
            if (dateTime == 0) {
            	currentEvent.finishDate = null;
            } else {
            	Date finishDate = new Date(dateTime);
                currentEvent.finishDate = finishDate;
            }
        }
        String linkAccessStr = currentNode.path("link_access").textValue();
        if (linkAccessStr != null && currentEvent.readingPrivacy == Privacy.PRIVATE) {
        	if (linkAccessStr.equals("none")) {
        		currentEvent.linkAccess = LinkAccess.NONE;
        	} else if (linkAccessStr.equals("read")) {
        		currentEvent.linkAccess = LinkAccess.READ;
        	} else if (linkAccessStr.equals("write")) {
        		currentEvent.linkAccess = LinkAccess.WRITE;
        	}
        }
    }
 
}
