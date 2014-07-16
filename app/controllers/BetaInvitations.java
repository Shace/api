package controllers;

import java.util.List;

import models.AccessToken;
import models.BetaInvitation;
import models.BetaInvitation.State;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@CORS
public class BetaInvitations extends Controller {

	public static Integer invitationNumber = 5;
	
	@BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSon required");
        }
        
        JsonNode guestList = root.get("guests");
        if (guestList == null) {
            return badRequest("Missing parameter [guests]");
        }

        BetaInvitation current = BetaInvitation.find.where().eq("createdUser", access.user).findUnique();
		ArrayNode guestsNode = Json.newObject().arrayNode();
    	for (JsonNode guestNode: guestList) {
    		if (current.invitedPeople > invitationNumber) {
    			break;
    		}
    		String mail = guestNode.path("email").textValue();
    		if (mail != null) {
    			BetaInvitation newGuest = BetaInvitation.find.where().eq("email", mail).findUnique();
    			if (newGuest == null) {
    				newGuest = new BetaInvitation(access.user, mail, State.INVITED);
    				newGuest.save();
    				current.invitedPeople++;
    				guestsNode.add(mail);
    			}
    		}
		}
    	current.save();
    	ObjectNode result = Json.newObject();
		result.put("invited", guestsNode);
		result.put("remaining", invitationNumber - current.invitedPeople);
		return created(result);
    }
    
    public static Result invitations(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }
        BetaInvitation current = BetaInvitation.find.where().eq("createdUser", access.user).findUnique();
        if (current == null) {
        	return forbidden("No invitations");
        }
        List<BetaInvitation> guestList = BetaInvitation.find.where().eq("originalUser", access.user).findList();
		ArrayNode guestsNode = Json.newObject().arrayNode();
    	for (BetaInvitation guest: guestList) {
    		ObjectNode infos = Json.newObject();
    		infos.put("email", guest.email);
    		infos.put("hasAccepted", guest.createdUser != null);
    		guestsNode.add(infos);
		}
    	ObjectNode result = Json.newObject();
		result.put("invited", guestsNode);
		result.put("remaining", invitationNumber - current.invitedPeople);
		return created(result);
    }

    public static Result processingList(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
        if (error != null) {
            return error;
        }
        List<BetaInvitation> guestList = BetaInvitation.find.where().eq("state", State.REQUESTING).findList();
		ArrayNode guestsNode = Json.newObject().arrayNode();
    	for (BetaInvitation guest: guestList) {
    		ObjectNode infos = Json.newObject();
    		infos.put("id", guest.id);
    		infos.put("email", guest.email);
    		guestsNode.add(infos);
		}
    	ObjectNode result = Json.newObject();
		result.put("processing", guestsNode);
		return created(result);
    }
    
	@BodyParser.Of(BodyParser.Json.class)
    public static Result validateProcessing(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
        if (error != null) {
            return error;
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
            return badRequest("Unexpected format, JSon required");
        }
        
        JsonNode guestList = root.get("validated");
        if (guestList == null) {
            return badRequest("Missing parameter [validated]");
        }

		ArrayNode validatedNode = Json.newObject().arrayNode();
    	for (JsonNode guestNode: guestList) {
    		Integer id = guestNode.path("id").intValue();
    		if (id != null) {
    			BetaInvitation currentGuest = BetaInvitation.find.where().eq("id", id).findUnique();
    			if (currentGuest != null && currentGuest.state == State.REQUESTING) {
    				currentGuest.state = State.INVITED;
    				currentGuest.save();
    				validatedNode.add(id);
    			}
    		}
		}
    	ObjectNode result = Json.newObject();
		result.put("processed", validatedNode);
		return created(result);
    }
}
