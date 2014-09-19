package controllers;

import models.AccessToken;
import models.Event;
import models.Media;
import models.Tag;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;
import Utils.RequestParameters;
import Utils.Slugs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import errors.Error.ParameterType;
import errors.Error.Type;

@CORS
public class Tags extends Controller {
    /**
     * Add a tag to a media.
     * The tag information is contained into the HTTP Request body as JSON format.
     * @param ownerEventToken : The id of the event containing the media
     * @param mediaId : The id of the media to tag
     * @return An HTTP Json response containing the properties of the new tag
     */
    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
    public static Result add(String ownerEventToken, Integer mediaId, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        Event ownerEvent = Event.find.where().eq("token", ownerEventToken).findUnique();
        if (ownerEvent == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        error = Access.hasPermissionOnEvent(access, ownerEvent, Access.AccessType.READ);
        if (error != null) {
            return error;
        }
        
        Media       media = Media.find.byId(mediaId);
        if (media == null) {
        	return new errors.Error(errors.Error.Type.MEDIA_NOT_FOUND).toResponse();
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        JsonNode name = root.get("name");
        if (name == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("name", ParameterType.REQUIRED).toResponse();
        }
 
        Tag tag = Tag.find.where().eq("slug", Slugs.toSlug(name.asText())).eq("media.id", media.id).findUnique();

        if (tag != null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("name", ParameterType.DUPLICATE).toResponse();
        }
        //Comment comment = Comment.create(access.user, media, message.asText());
        tag = Tag.create(name.asText(), access.user, media);
        
        return created(tagToJson(access, ownerEvent, tag, null));
    }
    
    /**
     * Delete the tag identified by the id parameter.
     * @param mediaId : the media identifier
     * @param id : the tag identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    @Transactional
    public static Result delete(String token, Integer mediaId, Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }
        
        Event ownerEvent = Event.find.where().eq("token", token).findUnique();
        if (ownerEvent == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }
        
        Tag   tag = Tag.find.byId(id);
        
        if (tag == null) {
        	return new errors.Error(errors.Error.Type.TAG_NOT_FOUND).toResponse();
        } else if ((!tag.creator.equals(access.user)) && Access.hasPermissionOnEvent(access, ownerEvent, Access.AccessType.ADMINISTRATE) != null) {
        	return new errors.Error(errors.Error.Type.NEED_ADMINISTRATE).toResponse();
        }

        tag.delete();
        return noContent();
    }
    
    /**
     * Convert a tag to a Json object.
     * @param tag : A Tag object to convert
     * @return The Json object containing the tag information
     */
    public static ObjectNode tagToJson(AccessToken access, Event ownerEvent, Tag tag, RequestParameters params) {
        ObjectNode result = Json.newObject();

        result.put("id", tag.id);
        result.put("name", tag.name);
        result.put("slug", tag.slug);
        result.put("owner", tag.creator.id);
        result.put("media", tag.media.id);
        result.put("creation", tag.creation.getTime());
        if (access.user == null || ((!tag.creator.equals(access.user)) && 
            Access.hasPermissionOnEvent(access, ownerEvent, Access.AccessType.ADMINISTRATE) != null)) {
            result.put("permission", Access.AccessType.READ.toString());
        } else {
            result.put("permission", Access.AccessType.ROOT.toString());
        }
        return result;
    }
}
