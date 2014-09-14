package controllers;

import java.util.List;

import models.AccessToken;
import models.Event;
import models.EventUserRelation;
import models.User;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;
import Utils.Mailer;
import Utils.Mailer.EmailType;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import errors.Error.ParameterType;
import errors.Error.Type;

/**
 * Controller that handles the different API action applied to the Event Permissions
 * @author Samuel Olivier
 * @category controllers
 */
@CORS
public class EventPermissions extends Controller {
    public static Result	permissions(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        Event event = Ebean.find(Event.class).where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        error = Access.hasPermissionOnEvent(access, event, Access.AccessType.ADMINISTRATE);
        if (error != null) {
            return error;
        }

        List<EventUserRelation> permissions = event.permissions;
        ArrayNode permissionsNode = Json.newObject().arrayNode();
        for (EventUserRelation permission : permissions) {
            boolean addPermission = false;

            if (permission.permission.compareTo(Access.AccessType.ADMINISTRATE) >= 0) {
                addPermission = true;
            } else if (event.writingPrivacy == Event.Privacy.PRIVATE &&
                    permission.permission.compareTo(Access.AccessType.WRITE) >= 0) {
                addPermission = true;
            } else if (event.readingPrivacy == Event.Privacy.PRIVATE &&
                    permission.permission.compareTo(Access.AccessType.READ) >= 0) {
                addPermission = true;
            }

            if (addPermission && permission.email != null) {
                ObjectNode permissionNode = Json.newObject();

                permissionNode.put("id", permission.id);
                permissionNode.put("email", permission.email);
                permissionNode.put("permission", permission.permission.toString());
                permissionsNode.add(permissionNode);
            }
        }

        ObjectNode result = Json.newObject();
        result.put("permissions", permissionsNode);

        return ok(result);
    }

    public static Result	setPermissions(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        Event event = Ebean.find(Event.class).where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        Access.AccessType userPermission = Access.getPermissionOnEvent(access, event);
        if (userPermission.compareTo(Access.AccessType.ADMINISTRATE) < 0) {
        	return new errors.Error(errors.Error.Type.NEED_ADMINISTRATE).toResponse();
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }

        JsonNode permissionList = root.get("permissions");
        if (permissionList == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("permissions", ParameterType.REQUIRED).toResponse();
        }

        ArrayNode permissionsNode = Json.newObject().arrayNode();

        for (JsonNode permissionNode : permissionList) {
            Access.AccessType givenPermission = null;

            String permissionString = permissionNode.path("permission").textValue();
            if (permissionString != null) {
                try {
                    givenPermission = Access.AccessType.valueOf(permissionString);
                } catch (IllegalArgumentException e) {
                    givenPermission = null;
                }
            }

            if (givenPermission == null || givenPermission.compareTo(userPermission) >= 0) {
                continue;
            }

            String email = permissionNode.path("email").textValue();
            User associated = null;
            
            if (email != null) {
                associated = User.find.where().eq("email", email).findUnique();
            } else {
                Integer id = permissionNode.path("id").intValue();
                if (id != null) {
                    associated = User.find.byId(id);
                    email = (associated == null) ? null : associated.email;
                }
            }

            if (email == null) {
                continue ;
            }

            EventUserRelation currentRelation = null;
            for (EventUserRelation relation : event.permissions) {
                if (relation.email != null && email.equals(relation.email)) {
                    currentRelation = relation;
                    break ;
                }
            }

            if (currentRelation == null) {
                currentRelation = new EventUserRelation(event, email, givenPermission);
            } else {
                currentRelation.permission = givenPermission;
            }

            if (associated == null) {
		    	Mailer.get().sendMail(EmailType.EVENT_ANONYMOUS_INVITATION, access.getLang(), email, ImmutableMap.of("FIRSTNAME", access.user.firstName, "LASTNAME", access.user.lastName, "TOKEN", token, "EVENT", event.name));
            } else {
		    	Mailer.get().sendMail(EmailType.EVENT_INVITATION, associated.lang, email, ImmutableMap.of("FIRSTNAME", access.user.firstName, "LASTNAME", access.user.lastName, "TOKEN", token, "USER_FIRSTNAME", associated.firstName, "EVENT", event.name));
            }
            
            currentRelation.save();
            ObjectNode newPermissionNode = Json.newObject();

            newPermissionNode.put("id", currentRelation.id);
            newPermissionNode.put("email", email);
            newPermissionNode.put("permission", currentRelation.permission.toString());
            permissionsNode.add(newPermissionNode);
        }

        return ok(permissionsNode);
    }

    public static Result	deletePermissions(String token, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        Event event = Ebean.find(Event.class).where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        Access.AccessType userPermission = Access.getPermissionOnEvent(access, event);
        if (userPermission.compareTo(Access.AccessType.ADMINISTRATE) < 0) {
        	return new errors.Error(errors.Error.Type.NEED_ADMINISTRATE).toResponse();
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }

        JsonNode permissionList = root.get("permissions");
        if (permissionList == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("permissions", ParameterType.REQUIRED).toResponse();
        }

        ArrayNode toDeleteNode = Json.newObject().arrayNode();

        for (JsonNode permissionNode : permissionList) {

            String email = permissionNode.path("email").textValue();
            User associated = null;

            if (email != null) {
                associated = User.find.where().eq("email", email).findUnique();
            } else {
                Integer id = permissionNode.path("id").intValue();
                if (id != null) {
                    associated = User.find.byId(id);
                    email = (associated == null) ? null : associated.email;
                }
            }

            if (email == null) {
                continue ;
            }

            boolean hasDeleted = false;
            for (EventUserRelation relation : event.permissions) {
                if (relation.email != null && email.equals(relation.email)) {
                    relation.delete();
                    hasDeleted = true;
                }
            }

            if (hasDeleted == false) {
                continue ;
            }

            ObjectNode currentDeletedNode = Json.newObject();

            currentDeletedNode.put("email", email);
            toDeleteNode.add(currentDeletedNode);
        }

        return ok(toDeleteNode);
    }

    public static Result    deleteUserPermissions(String token, String accessToken, Integer permissionId) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        Event event = Ebean.find(Event.class).where().eq("token", token).findUnique();
        if (event == null) {
        	return new errors.Error(errors.Error.Type.EVENT_NOT_FOUND).toResponse();
        }

        Access.AccessType userPermission = Access.getPermissionOnEvent(access, event);
        if (userPermission.compareTo(Access.AccessType.ADMINISTRATE) < 0) {
        	return new errors.Error(errors.Error.Type.NEED_ADMINISTRATE).toResponse();
        }

        ArrayNode toDeleteNode = Json.newObject().arrayNode();

        for (EventUserRelation relation : event.permissions) {
            if (relation.id != null && permissionId.equals(relation.id)) {
                relation.delete();
            }
        }

        ObjectNode currentDeletedNode = Json.newObject();

        currentDeletedNode.put("id", permissionId);
        toDeleteNode.add(currentDeletedNode);

        return ok();
    }
}
