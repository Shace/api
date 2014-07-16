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

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
            return notFound("Event with token " + token + " not found");
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

            if (addPermission && permission.user != null) {
                ObjectNode permissionNode = Json.newObject();

                permissionNode.put("user", permission.user.id);
                permissionNode.put("email", permission.user.email);
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
            return notFound("Event with token " + token + " not found");
        }

        Access.AccessType userPermission = Access.getPermissionOnEvent(access, event);
        if (userPermission.compareTo(Access.AccessType.ADMINISTRATE) < 0) {
            return forbidden("You cannot change permissions on this event");
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSON required");
        }

        JsonNode permissionList = root.get("permissions");
        if (permissionList == null) {
            return badRequest("Missing parameter [permissions]");
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

            User associated = null;

            String email = permissionNode.path("email").textValue();
            if (email != null) {
                associated = User.find.where().eq("email", email).findUnique();
                if (associated == null) {
                    // TODO : Send mail to this person
                }
            } else {
                Integer id = permissionNode.path("id").intValue();
                if (id != null) {
                    associated = User.find.byId(id);
                }
            }

            if (associated == null) {
                continue ;
            }

            EventUserRelation currentRelation = null;
            for (EventUserRelation relation : event.permissions) {
                if (relation.user != null && associated.equals(relation.user)) {
                    currentRelation = relation;
                    break ;
                }
            }

            if (currentRelation == null) {
                currentRelation = new EventUserRelation(event, associated, givenPermission);
            } else {
                currentRelation.permission = givenPermission;
            }
            currentRelation.save();
            ObjectNode newPermissionNode = Json.newObject();

            newPermissionNode.put("user", currentRelation.user.id);
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
            return notFound("Event with token " + token + " not found");
        }

        Access.AccessType userPermission = Access.getPermissionOnEvent(access, event);
        if (userPermission.compareTo(Access.AccessType.ADMINISTRATE) < 0) {
            return forbidden("You cannot change permissions on this event");
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSON required");
        }

        JsonNode permissionList = root.get("permissions");
        if (permissionList == null) {
            return badRequest("Missing parameter [permissions]");
        }

        ArrayNode toDeleteNode = Json.newObject().arrayNode();

        for (JsonNode permissionNode : permissionList) {
            User associated = null;

            String email = permissionNode.path("email").textValue();
            if (email != null) {
                associated = User.find.where().eq("email", email).findUnique();
            } else {
                Integer id = permissionNode.path("id").intValue();
                if (id != null) {
                    associated = User.find.byId(id);
                }
            }

            if (associated == null) {
                continue ;
            }

            boolean hasDeleted = false;
            for (EventUserRelation relation : event.permissions) {
                if (relation.user != null && associated.equals(relation.user)) {
                    relation.delete();
                    hasDeleted = true;
                }
            }

            if (hasDeleted == false) {
                continue ;
            }

            ObjectNode currentDeletedNode = Json.newObject();

            currentDeletedNode.put("user", associated.id);
            toDeleteNode.add(currentDeletedNode);
        }

        return ok(toDeleteNode);
    }

    public static Result    deleteUserPermissions(String token, String accessToken, Integer userId) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        Event event = Ebean.find(Event.class).where().eq("token", token).findUnique();
        if (event == null) {
            return notFound("Event with token " + token + " not found");
        }

        Access.AccessType userPermission = Access.getPermissionOnEvent(access, event);
        if (userPermission.compareTo(Access.AccessType.ADMINISTRATE) < 0) {
            return forbidden("You cannot change permissions on this event");
        }

        ArrayNode toDeleteNode = Json.newObject().arrayNode();


        User associated = User.find.byId(userId);
        if (associated == null) {
            return notFound("User with id " + userId + " not found");
        }

        for (EventUserRelation relation : event.permissions) {
            if (relation.user != null && associated.equals(relation.user)) {
                relation.delete();
            }
        }

        ObjectNode currentDeletedNode = Json.newObject();

        currentDeletedNode.put("user", associated.id);
        toDeleteNode.add(currentDeletedNode);

        return ok(toDeleteNode);
    }
}
