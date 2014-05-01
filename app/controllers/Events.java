package controllers;

import java.util.List;

import models.AccessToken;
import models.Event;
import models.Event.Privacy;
import models.Media;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
     * @param media An Event object to convert
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
        
        ArrayNode medias = result.putArray("medias");
        for (Media media : event.medias) {
            if (media.image.files.size() > 0)
                medias.add(Medias.mediaToJson(media, null));
        }
        
        result.put("bucket", Buckets.getBucketObjectNode(event.root));

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
        if (json == null)
            return badRequest("Unexpected format, JSON required");
        
        Privacy readingPrivacy = Privacy.NOT_SET;
        String 	token = null;
        String	readingPassword = null;
        
        String readingPrivacyStr = json.path("privacy").textValue();
        Event event = null;

        if (readingPrivacyStr == null) {
            return badRequest("Missing parameter [readingPrivacy]");
        }
        if (readingPrivacyStr.equals("public")) {
            token = json.path("token").textValue();

            if (token == null) {
                return badRequest("Missing parameter [token]");
            } else if (Event.find.where().eq("token", token).findUnique() != null) {
                return badRequest("Token already used");
            }
            readingPrivacy = Privacy.PUBLIC;
        } else if (readingPrivacyStr.equals("protected")) {
            token = json.path("token").textValue();
            readingPassword = json.path("password").textValue();

            if (token == null) {
                return badRequest("Missing parameter [token]");
            } else if (Event.find.where().eq("token", token).findUnique() != null) {
                return badRequest("Token already used");
            } else if (readingPassword == null) {
                return badRequest("Missing parameter [password]");
            }
            readingPrivacy = Privacy.PROTECTED;
            readingPassword = Utils.Hasher.hash(readingPassword);
        } else if (readingPrivacyStr.equals("private")) {
        	readingPrivacy = Privacy.PRIVATE;
        } else {
            return badRequest("[readingPrivacy] has to be in ('public', 'protected', 'private')");
        }
        
        event = new Event(readingPrivacy, access.user);
        if (token != null) {
        	event.token = token;
        }
        if (readingPassword != null) {
        	event.readingPassword = readingPassword;
        }
        
        String name = json.path("name").textValue();
        if (name == null) {
            return badRequest("Missing parameter [name]");
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
            return notFound("Event with token " + token + " not found");
        }
        
        error = Access.hasPermissionOnEvent(access, event, Event.AccessType.ROOT);
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
    public static Result update(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        Event event = Event.find.where().eq("token", token).findUnique();
        if (event == null) {
            return notFound("Event with token " + token + " not found");
        }
        
        error = Access.hasPermissionOnEvent(access, event, Event.AccessType.ADMINISTRATE);
        if (error != null) {
        	return error;
        }
        
        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSON required");
        }
        
        fillEventFromJSON(event, root);
        
        String readingPrivacyStr = root.path("privacy").textValue();
        if (readingPrivacyStr != null) {
        	if (readingPrivacyStr.equals("public")) {
        		token = root.path("token").textValue();

        		if (event.readingPrivacy == Privacy.PRIVATE && token == null) {
        			return badRequest("A token is required for a public event");
        		} else if (token != null && !token.equals(event.token) && Event.find.where().eq("token", token).findUnique() != null) {
        			return badRequest("Token already used");
        		} else {
        			event.readingPrivacy = Privacy.PUBLIC;
        			if (token != null) {
        				event.token = token;
        			}
        		}
        	} else if (readingPrivacyStr.equals("protected")) {
        		token = root.path("token").textValue();

        		if (event.readingPrivacy == Privacy.PRIVATE && token == null) {
        			return badRequest("A token is required for a protected event");
        		} else if (token != null && !token.equals(event.token) && Event.find.where().eq("token", token).findUnique() != null) {
        			return badRequest("Token already used");
        		} else {
        			String readingPassword = root.path("readingPassword").textValue();
        			if (readingPassword == null) {
        				return badRequest("Missing parameter [readingPassword]");
        			}
        			event.readingPrivacy = Privacy.PROTECTED;
        			if (token != null) {
        				event.token = token;
        			}
        			event.readingPassword = Utils.Hasher.hash(readingPassword);
        		}
        	} else if (readingPrivacyStr.equals("private")) {
        		event.readingPrivacy = Privacy.PRIVATE;
        		event.token = event.id;
        	} else {
        		return badRequest("[readingPrivacy] has to be in ('public', 'protected', 'private')");
        	}
        }
        String writingPrivacyStr = root.path("writingPrivacy").textValue();
        if (writingPrivacyStr != null) {
            boolean writingValid = true;

            if (writingPrivacyStr.equals("public")) {
        		if (event.readingPrivacy.compareTo(Privacy.PUBLIC) <= 0) {
        			event.writingPrivacy = Privacy.PUBLIC;
        		} else {
        			writingValid = false;
        		}
        	} else if (writingPrivacyStr.equals("protected")) {
        		if (event.readingPrivacy.compareTo(Privacy.PROTECTED) <= 0) {
        			String writingPassword = root.path("writingPassword").textValue();
        			if (writingPassword == null) {
        				return badRequest("Missing parameter [writingPassword]");
        			}
        			event.writingPrivacy = Privacy.PROTECTED;
        			event.writingPassword = Utils.Hasher.hash(writingPassword);
        		} else {
        			writingValid = false;
        		}
        	} else if (writingPrivacyStr.equals("private")) {
        		if (event.readingPrivacy.compareTo(Privacy.PRIVATE) <= 0) {
        			event.writingPrivacy = Privacy.PRIVATE;
        		} else {
        			writingValid = false;
        		}
        	} else {
        		return badRequest("[readingPrivacy] has to be in ('public', 'protected', 'private')");
        	}
            
            if (writingValid == false) {
				return badRequest("The writing privacy cannot match the reading privacy");
            }
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

        Event event = Ebean.find(Event.class).fetch("medias").fetch("medias.owner").fetch("medias.image")
                .fetch("medias.image.files").fetch("medias.image.files.file").fetch("root").where().eq("token", token).findUnique();
        if (event == null) {
            return notFound("Event with token " + token + " not found");
        }

        error = Access.hasPermissionOnEvent(access, event, Event.AccessType.READ);
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
}
