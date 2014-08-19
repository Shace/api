package controllers;

import models.AccessToken;
import models.Comment;
import models.Event;
import models.Media;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;
import Utils.RequestParameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import errors.Error.ParameterType;
import errors.Error.Type;

/**
 * Controller that handles the different API action applied to the comments
 * @author Loick Michard
 * @category controllers
 */
@CORS
public class Comments extends Controller {
    /**
     * Add a comment to a media.
     * The comment information is contained into the HTTP Request body as JSON format.
     * @param ownerEventToken : The id of the event containing the media
     * @param mediaId : The id of the media to comment
     * @return An HTTP Json response containing the properties of the new comment
     */
    @BodyParser.Of(BodyParser.Json.class)
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

        JsonNode message = root.get("message");
        if (message == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("message", ParameterType.REQUIRED).toResponse();
        }

        Comment comment = Comment.create(access.user, media, message.asText());

        return created(commentToJson(access, ownerEvent, comment, null));
    }

    /**
     * Delete the comment identified by the id parameter.
     * @param mediaId : the media identifier
     * @param id : the comment identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
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

        Comment   comment = Comment.find.byId(id);

        if (comment == null) {
        	return new errors.Error(errors.Error.Type.COMMENT_NOT_FOUND).toResponse();
        } else if ((!comment.owner.equals(access.user)) && Access.hasPermissionOnEvent(access, ownerEvent, Access.AccessType.ADMINISTRATE) != null) {
        	return new errors.Error(errors.Error.Type.NEED_ADMINISTRATE).toResponse();
        }

        comment.delete();
        return noContent();
    }

    /**
     * Convert a comment to a Json object.
     * @param comment : A Comment object to convert
     * @return The Json object containing the comment information
     */
    public static ObjectNode commentToJson(AccessToken access, Event ownerEvent, Comment comment, RequestParameters params) {
        //      JSONSerializer tmp = new JSONSerializer();
        ObjectNode result = Json.newObject();

        result.put("id", comment.id);
        result.put("message", comment.message);
        result.put("owner", comment.owner.id);
        result.put("username", (comment.owner.firstName == null) ? "Anonymous" : comment.owner.firstName);
        result.put("media", comment.media.id);
        result.put("creation", comment.creation.getTime());
        if (access.user == null || ((!comment.owner.equals(access.user)) && 
                Access.hasPermissionOnEvent(access, ownerEvent, Access.AccessType.ADMINISTRATE) != null)) {
            result.put("permission", Access.AccessType.READ.toString());
        } else {
            result.put("permission", Access.AccessType.ROOT.toString());
        }

        return result;
    }
}
