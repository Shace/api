package controllers;

import java.io.File;
import java.util.Date;
import java.util.List;

import models.AccessToken;
import models.BetaInvitation;
import models.Image;
import models.BetaInvitation.State;
import models.Image.FormatType;
import models.EventUserRelation;
import models.User;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import Utils.Access;
import Utils.Mailer;
import Utils.Mailer.EmailType;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import errors.Error.ParameterType;
import errors.Error.Type;

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
        result.put("lang", user.lang.toString().toLowerCase());
        if (user.profilePicture != null) {
        	result.put("profile_picture", Images.getImageObjectNode(user.profilePicture));
        }
        if (user.coverPicture != null) {
        	result.put("cover_picture", Images.getImageObjectNode(user.coverPicture));
        }

        return result;
    }

    /**
     * List all the visible users (only admins can do that).
     * 
     * @return An HTTP JSON response containing the properties of all the users
     */
    @Transactional
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
    
    public static void checkParams(boolean required, String email, String password, String firstname, String lastname, errors.Error parametersErrors) {
       
    	if (email == null || email.isEmpty()) {
    		if (required) {
    			parametersErrors.addParameter("email", ParameterType.REQUIRED);
    		}
        } else if (!Utils.Formats.isValidEmail(email)) {
        	parametersErrors.addParameter("email", ParameterType.FORMAT);
        }

        if (password == null || password.isEmpty()) {
        	if (required) {
    			parametersErrors.addParameter("password", ParameterType.REQUIRED);
    		}
        } else if (password.length() < 5) {
        	parametersErrors.addParameter("password", ParameterType.FORMAT);
        }
        
        if (firstname == null || firstname.isEmpty()) {
        	if (required) {
    			parametersErrors.addParameter("first_name", ParameterType.REQUIRED);
    		}
        } else if (firstname.length() < 2) {
        	parametersErrors.addParameter("first_name", ParameterType.FORMAT);
        }

        if (lastname == null || lastname.isEmpty()) {
        	if (required) {
    			parametersErrors.addParameter("last_name", ParameterType.REQUIRED);
    		}
        } else if (lastname.length() < 2) {
        	parametersErrors.addParameter("last_name", ParameterType.FORMAT);
        }

        if (!parametersErrors.isParameterError() && required && User.find.where().eq("email", email).findUnique() != null) {
        	parametersErrors.addParameter("email", ParameterType.DUPLICATE);
        }
    }

    /**
     * Create a new user. The user properties are contained into the HTTP Request body as JSON format.
     * 
     * @return An HTTP JSON response containing the properties of the created
     *         user
     */
    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.NOT_CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        JsonNode root = request().body().asJson();

        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }

        errors.Error parametersErrors = new errors.Error(Type.PARAMETERS_ERROR);
        String email = root.path("email").textValue();
        String password = root.path("password").textValue();
        String firstname = root.path("first_name").textValue();
        String lastname = root.path("last_name").textValue();
        checkParams(true, email, password, firstname, lastname, parametersErrors);
        
        if (parametersErrors.isParameterError())
        	return parametersErrors.toResponse();
        
        // Beta Handling
        BetaInvitation betaInvitation = BetaInvitation.find.where().eq("email", email).findUnique();
        if (betaInvitation == null) {
        	betaInvitation = new BetaInvitation(null, email, password, firstname, lastname, State.REQUESTING);
        	betaInvitation.lang = access.lang;
        	betaInvitation.save();
        	Mailer.get().sendMail(EmailType.BETA_REQUEST_SENT, access.getLang(), email, ImmutableMap.of("FIRSTNAME", firstname, "LASTNAME", lastname));
        	return status(ACCEPTED);
        } else if (betaInvitation.state == State.INVITED) {        
	        User newUser = new User(email, password, firstname, lastname);
	        updateOneUser(newUser, root);
	        newUser.save();
	        
	        // Beta Handling
	        betaInvitation.createdUser = newUser;
	        betaInvitation.state = State.CREATED;
	        betaInvitation.save();
	        return created(getUserObjectNode(newUser));
        } else {
        	return new errors.Error(errors.Error.Type.BETA_PROCESSING).toResponse();
        }
    }

    /**
     * Delete the user identified by the id parameter.
     * 
     * @param id : the user identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    @Transactional
    public static Result delete(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        User user = User.find.byId(id);
        if (user == null) {
        	return new errors.Error(errors.Error.Type.USER_NOT_FOUND).toResponse();
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
    @Transactional
    public static Result update(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        errors.Error parametersErrors = new errors.Error(Type.PARAMETERS_ERROR);
        String email = root.path("email").textValue();
        String password = root.path("password").textValue();
        String oldPassword = root.path("old_password").textValue();
        String firstname = root.path("first_name").textValue();
        String lastname = root.path("last_name").textValue();
        checkParams(false, email, password, firstname, lastname, parametersErrors);
        
        if (parametersErrors.isParameterError())
        	return parametersErrors.toResponse();
        
        User user = User.find.byId(id);
        if (user == null) {
        	return new errors.Error(errors.Error.Type.USER_NOT_FOUND).toResponse();
        }

        error = Access.hasPermissionOnUser(access, user, Access.UserAccessType.WRITE);
        if (error != null) {
        	return error;
        }

        if (password != null) {
        	if (oldPassword == null) {
        		return new errors.Error(Type.PARAMETERS_ERROR).addParameter("old_password", ParameterType.REQUIRED).toResponse();
        	} else if (!user.password.equals(Utils.Hasher.hash(oldPassword))) {
        		return new errors.Error(errors.Error.Type.WRONG_PASSWORD).toResponse();
        	}
        }

        updateOneUser(user, root);
        user.save();

        return ok(getUserObjectNode(user));
    }

    /**
     * Get the properties of the user identified by the id parameter.
     * 
     * @param id : the user identifier
     * @return An HTTP JSON response containing the properties of the specified user
     */
    @Transactional
    public static Result user(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
        
        User user = User.find.byId(id);
        if (user == null) {
        	return new errors.Error(errors.Error.Type.USER_NOT_FOUND).toResponse();
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
    @Transactional
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
    @Transactional
    private static void updateOneUser(User currentUser, JsonNode currentNode) {
        String password = currentNode.path("password").textValue();
        if (password != null) {
            currentUser.password = Utils.Hasher.hash(password);
        }
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
    @Transactional
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

    @Transactional
    public static ObjectNode getEventsListNode(List<EventUserRelation> eventUserRelations, AccessToken accessToken) {
        ObjectNode result = Json.newObject();

        ArrayNode events = result.putArray("events");
        for (EventUserRelation eur : eventUserRelations) {            
            events.add(Events.getEventObjectNode(eur.event, accessToken, false));
        }

        return result;
    }
    
    /**
     * Get all events of connected user
     * 
     * @return An HTTP JSON response containing the events of the connected
     *         user
     */
    @Transactional
    public static Result events(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }

        error = Access.hasPermissionOnUser(access, access.user, Access.UserAccessType.READ);
        if (error != null) {
        	return error;
        }
        
        List<EventUserRelation> eventUserRelations = EventUserRelation.find.where().eq("email", access.user.email).findList();
        
        return ok(getEventsListNode(eventUserRelations, access));
    }
    
    /**
     * Add a file to the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the file upload success
     */
    @Transactional
    public static Result addProfilePicture(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
    	
        User user = User.find.byId(id);
        if (user == null) {
        	return new errors.Error(errors.Error.Type.USER_NOT_FOUND).toResponse();
        }

    	error = Access.hasPermissionOnUser(access, user, Access.UserAccessType.WRITE);
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
        	  if (user.profilePicture == null) {
        		  user.profilePicture = Image.create(null);
        	  }
        	  Images.replaceImage(user.profilePicture, file, FormatType.PROFILE_PICTURE);
              user.profilePicture.owner = access.user;
              user.profilePicture.save();
          } catch (Image.BadFormat b) {
          	return new errors.Error(errors.Error.Type.BAD_FORMAT_IMAGE).toResponse();
          }
        }

        user.update();

        return ok(Images.getImageObjectNode(user.profilePicture));
    }
    
    /**
     * Add a file to the media identified by the id parameter.
     * @param id : the media identifier
     * @return An HTTP response that specifies if the file upload success
     */
    @Transactional
    public static Result addCoverPicture(Integer id, String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
        	return error;
        }
    	
        User user = User.find.byId(id);
        if (user == null) {
        	return new errors.Error(errors.Error.Type.USER_NOT_FOUND).toResponse();
        }

    	error = Access.hasPermissionOnUser(access, user, Access.UserAccessType.WRITE);
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
        	  if (user.coverPicture == null) {
        		  user.coverPicture = Image.create(null);
        	  }
        	  Images.replaceImage(user.coverPicture, file, FormatType.COVER);
              user.coverPicture.owner = access.user;
              user.coverPicture.save();
          } catch (Image.BadFormat b) {
          	return new errors.Error(errors.Error.Type.BAD_FORMAT_IMAGE).toResponse();
          }
        }

        user.update();

        return ok(Images.getImageObjectNode(user.coverPicture));
    }
}
