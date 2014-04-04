package controllers;

import java.util.Date;

import models.AccessToken;
import models.AccessToken.Type;
import models.User;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        if (accessToken.user != null && accessToken.type == Type.USER) {
            result.put("user_id", accessToken.user.id);
        } else {
            result.putNull("user_id");
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
        if (access == null)
            return notFound("Token not found");

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
            return badRequest("Expecting Json data");
        }
        String email = json.path("email").textValue();
        String password = json.path("password").textValue();
        boolean autoRenew = json.path("auto_renew").booleanValue();
        if (email == null) {
            return ok(getAccessTokenObjectNode(AccessToken.create(autoRenew, null, Type.GUEST)));
        } else if (password == null) {
            return badRequest("Missing parameter [password]");
        } else {
            AccessToken res = authenticate(email, password, autoRenew);
            if (res == null) {
                return unauthorized("Invalid user or password");
            }
            return ok(getAccessTokenObjectNode(res));
        }
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
}
