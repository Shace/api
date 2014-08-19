package controllers;

import java.util.Date;

import models.AccessToken;
import models.AccessToken.Lang;
import models.AccessToken.Type;
import models.BetaInvitation;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import errors.Error.ParameterType;

/**
 * Controller that handles the different API action applied to the AccessToken
 * @author Loick Michard
 * @category controllers
 */
@CORS
public class AccessTokens extends Controller {

    /**
     * Convert an AccessToken to a Json object.
     * 
     * @param accessToken An AccessToken object to convert
     * @return The JSON object containing the access token information
     */
    private static ObjectNode getAccessTokenObjectNode(AccessToken accessToken) {
        ObjectNode result = Json.newObject();

        result.put("token", accessToken.token);
        result.put("auto_renew", accessToken.autoRenew);
        result.put("expiration", accessToken.expiration);
        result.put("creation", accessToken.creation);
        result.put("type", accessToken.type.toString().toLowerCase());
        if (accessToken.user != null && accessToken.user.lang != null) {
        	result.put("lang", accessToken.user.lang.toString().toLowerCase());
        } else {
        	result.put("lang", accessToken.lang.toString().toLowerCase());
        }
        if (accessToken.user != null && accessToken.type == Type.USER) {
            result.put("user_id", accessToken.user.id);
        } else {
            result.put("user_id", -1);
        }

        return result;
    }

    /**
     * Delete the AccessToken identified by the accessToken parameter.
     * 
     * @param accessToken The access token identifier
     * @return An HTTP response that specifies if the deletion succeeded or not
     */
    public static Result delete(String accessToken) {
        AccessToken access = AccessToken.find.byId(accessToken);
        if (access == null) {
        	return new errors.Error(errors.Error.Type.TOKEN_NOT_FOUND).toResponse();
        }

        access.delete();
        return noContent();
    }

    /**
     * Ask for an access token. Guest token will be created if no email
     * parameter in JSON.
     * 
     * @return An HTTP JSON response containing the properties of the created
     *         access token
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result accessToken() {
        JsonNode json = request().body().asJson();

        if (json == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        String email = json.path("email").textValue();
        String password = json.path("password").textValue();
        boolean autoRenew = json.path("auto_renew").booleanValue();
        if (email == null) {
            return ok(getAccessTokenObjectNode(AccessToken.create(autoRenew, null, Type.GUEST)));
        } else if (password == null) {
            return new errors.Error(errors.Error.Type.PARAMETERS_ERROR).addParameter("password", ParameterType.REQUIRED).toResponse();
        } else {
            AccessToken res = authenticate(email, password, autoRenew);
            if (res == null) {
            	return new errors.Error(errors.Error.Type.INVALID_IDS).toResponse();
            }
            return ok(getAccessTokenObjectNode(res));
        }
    }
    
    /**
     * Update a token with a user connection
     * 
     * @param accessToken An access token corresponding to the user, null if authentication failed
     * @return An HTTP JSON response containing the properties of the updated
     *         access token
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result connection(String accessToken) {
        AccessToken access = AccessToken.find.byId(accessToken);
        if (access == null) {
        	return new errors.Error(errors.Error.Type.TOKEN_NOT_FOUND).toResponse();
        }
        if (access.type == Type.USER) {
        	return new errors.Error(errors.Error.Type.ALREADY_CONNECTED).toResponse();
        }

        JsonNode json = request().body().asJson();
        if (json == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        String email = json.path("email").textValue();
        String password = json.path("password").textValue();
        boolean autoRenew = json.path("auto_renew").booleanValue();

        errors.Error parametersErrors = new errors.Error(errors.Error.Type.PARAMETERS_ERROR);
        if (email == null) {
            parametersErrors.addParameter("email", ParameterType.REQUIRED);
        }
        if (password == null) {
        	parametersErrors.addParameter("password", ParameterType.REQUIRED);
        }
        if (parametersErrors.isParameterError()) {
        	return parametersErrors.toResponse();
        }
        
        User user = Users.authenticate(email, password);
        if (user == null) {
            BetaInvitation betaInvitation = BetaInvitation.find.where().eq("email", email).findUnique();
            if (betaInvitation != null) {
            	return new errors.Error(errors.Error.Type.BETA_PROCESSING).toResponse();
            }
        	return new errors.Error(errors.Error.Type.INVALID_IDS).toResponse();
        }
        
        access.user = user;
        access.type = Type.USER;
        access.autoRenew = autoRenew;
        if (access.autoRenew) {
            access.expiration = new Date().getTime() + AccessToken.autoRenewExpirationTime;
        }
        access.save();
        
        return ok(getAccessTokenObjectNode(access));
    }

    /**
     * Create an access token with an associate user
     * 
     * @param email The user email
     * @param password The user password
     * @param autoRenew Create an auto renew token if true
     * @return An access token corresponding to the user, null if authentication failed
     */
    public static AccessToken authenticate(String email, String password, boolean autoRenew) {
        User user = Users.authenticate(email, password);
        if (user == null) {
            return null;
        }
        return AccessToken.create(autoRenew, user, Type.USER);
    }

    /**
     * Get an AccessToken object according to his id
     * 
     * @param accessToken The access token id
     * @return the corresponding AccessToken object
     */
    public static AccessToken access(String accessToken) {

        if (accessToken == null) {
            return null;
        }
        
        AccessToken token = Ebean.find(AccessToken.class).fetch("user").where().eq("token", accessToken).where().gt("expiration", new Date().getTime()).setMaxRows(1).findUnique();

        if (token == null) {
            return null;
        }

        if (token.autoRenew) {
            token.expiration = new Date().getTime() + AccessToken.autoRenewExpirationTime;
            SqlUpdate update = Ebean.createSqlUpdate("UPDATE se_access_token SET expiration=:expiration WHERE token=:token")
                    .setParameter("expiration",token.expiration)
                    .setParameter("token", token.token);
            update.execute();
        }
        return token;
    }
    
    public static Result changeLanguage(String language, String accessToken) {
    	 AccessToken access = AccessTokens.access(accessToken);
         Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
         if (error != null) {
         	return error;
         }
         
         Lang newLang = access.lang;
         if (language.equalsIgnoreCase(Lang.EN.toString())) {
        	 newLang = Lang.EN;
         } else if (language.equalsIgnoreCase(Lang.FR.toString())) {
        	 newLang = Lang.FR;
         } else {
         	return new errors.Error(errors.Error.Type.LANGUAGE_NOT_FOUND).toResponse();
         }
         if (access.user != null) {
        	 //access.user.refresh();
        	 //access.user.lang = newLang;
        	 //access.user.save();
        	 
        	 String s = "UPDATE se_user set lang = :lang where id = :id";
             SqlUpdate update = Ebean.createSqlUpdate(s);
             update.setParameter("lang", newLang);
             update.setParameter("id", access.user.id);
             Ebean.execute(update);
         }
         //access.lang = newLang;
         //access.save();
         
         String s = "UPDATE se_access_token set lang = :lang where token = :token";
         SqlUpdate update = Ebean.createSqlUpdate(s);
         update.setParameter("lang", newLang);
         update.setParameter("token", access.token);
         Ebean.execute(update);
         
         return ok();
    }
}
