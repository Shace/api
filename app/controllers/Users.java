package controllers;

import java.util.Date;
import java.util.List;

import models.AccessToken;
import models.BetaInvitation;
import models.BetaInvitation.State;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Controller that handles the different API action applied to a User
 * @author Hajar Fares
 * @category controllers
 */
@CORS
public class Users extends Controller {

    /**
     * Convert a user to a JSON object.
     * 
     * @param user A User object to convert
     * @return The JSON object containing the user information
     */
    public static ObjectNode getUserObjectNode(User user) {
        ObjectNode result = Json.newObject();

        result.put("id", user.id);
        result.put("email", user.email);
        result.put("first_name", user.firstName);
        result.put("last_name", user.lastName);
        if (user.birthDate != null) {
            result.put("birth_date", user.birthDate.getTime());
        } else {
            result.putNull("birth_date");
        }
        result.put("inscription", user.inscriptionDate.getTime());
        result.put("is_admin", user.isAdmin);

        return result;
    }

    /**
     * List all the visible users (only admins can do that).
     * 
     * @return An HTTP JSON response containing the properties of all the users
     */
    public static Result users(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
        if (error != null) {
        	return error;
        }
        List<User> users = User.find.findList();

        ArrayNode usersNode = Json.newObject().arrayNode();

        for (User user : users) {
            usersNode.add(getUserObjectNode(user));
        }
        ObjectNode result = Json.newObject();
        result.put("users", usersNode);

        return ok(result);
    }

    /**
     * Create a new user. The user properties are contained into the HTTP Request body as JSON format.
     * 
     * @return An HTTP JSON response containing the properties of the created
     *         user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.NOT_CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSON required");
        }

        String email = root.path("email").textValue();
        if (email == null || email.isEmpty()) {
            return badRequest("Missing parameter [email]");
        }

        String password = root.path("password").textValue();
        if (password == null || password.isEmpty()) {
            return badRequest("Missing parameter [password]");
        }

        if (User.find.where().eq("email", email).findUnique() != null) {
            return badRequest("Email already exists");
        }
        
        // Beta Handling
        BetaInvitation betaInvitation = BetaInvitation.find.where().eq("email", email).findUnique();
        if (betaInvitation == null) {
        	betaInvitation = new BetaInvitation(null, email, State.REQUESTING);
        	betaInvitation.save();
        	return forbidden("Your request to join the beta has been sent.");
        } else if (betaInvitation.state == State.INVITED) {        
	        User newUser = new User(email, password);
	        updateOneUser(newUser, root);
	        newUser.save();
	        
	        // Beta Handling
	        betaInvitation.createdUser = newUser;
	        betaInvitation.state = State.CREATED;
	        betaInvitation.save();
	        return created(getUserObjectNode(newUser));
        } else {
        	return forbidden("Your request to join the beta is still being processed.");
        }
    }

    /**
     * Delete the user identified by the id parameter.
     * 
     * @param id : the user identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    public static Result delete(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        User user = User.find.byId(id);
        if (user == null) {
            return notFound("User with id " + id + " not found");
        }

        error = Access.hasPermissionOnUser(access, user, Access.UserAccessType.WRITE);
        if (error != null) {
        	return error;
        }

        // TODO This is a really tricky operation. All the medias, events, tokens, ... need to be delete !
        // For now, deletion is not yet possible
        // Maybe only set a valid flag
        
        return TODO;
    }

    /**
     * Update the user identified by the id parameter.
     * The new user properties are contained into the HTTP Request body as JSON format.
     * 
     * @param id : the user identifier
     * @return An HTTP JSON response containing the new properties of edited user
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSON required");
        }
        
        User user = User.find.byId(id);
        if (user == null) {
            return notFound("User with id " + id + " not found");
        }

        error = Access.hasPermissionOnUser(access, user, Access.UserAccessType.WRITE);
        if (error != null) {
        	return error;
        }

        updateOneUser(user, root);
        user.update();

        return ok(getUserObjectNode(user));
    }

    /**
     * Get the properties of the user identified by the id parameter.
     * 
     * @param id : the user identifier
     * @return An HTTP JSON response containing the properties of the specified user
     */
    public static Result user(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
        User user = User.find.byId(id);
        if (user == null) {
            return notFound("User with id " + id + " not found");
        }

        error = Access.hasPermissionOnUser(access, user, Access.UserAccessType.READ);
        if (error != null) {
        	return error;
        }
 
        return ok(getUserObjectNode(user));
    }

    /**
     * Get user by email and password
     * 
     * @param email User email
     * @param password User password
     * @return The user corresponding to the email and password, null if not found
     */
    public static User authenticate(String email, String password) {
        String sha1 = Utils.Hasher.hash(password);

        if (sha1 == null)
            return null;
        return User.find.where().eq("email", email).eq("password", sha1).findUnique();
    }

    /**
     * Update the user properties from a JSON object.
     * 
     * @param currentUser : The user to update
     * @param currentNode : The new properties to set
     */
    private static void updateOneUser(User currentUser, JsonNode currentNode) {
        String password = currentNode.path("password").textValue();
        if (password != null)
            currentUser.password = Utils.Hasher.hash(password);
        String firstName = currentNode.path("first_name").textValue();
        if (firstName != null)
            currentUser.firstName = firstName;
        String lastName = currentNode.path("last_name").textValue();
        if (lastName != null)
            currentUser.lastName = lastName;
        Long dateTime = currentNode.path("birth_date").asLong();
        if (currentNode.path("birth_date").canConvertToLong()) {
            Date birthDate = new Date(dateTime);
            currentUser.birthDate = birthDate;
        }
    }

    /**
     * Get user information of connected user
     * 
     * @return An HTTP JSON response containing the properties of the connected
     *         user
     */
    public static Result me(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        error = Access.hasPermissionOnUser(access, access.user, Access.UserAccessType.READ);
        if (error != null) {
        	return error;
        }

        return ok(getUserObjectNode(access.user));
    }
}
