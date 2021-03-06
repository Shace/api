package controllers;

import java.util.List;

import models.AccessToken;
import models.BetaInvitation;
import models.User;
import models.BetaInvitation.State;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;
import Utils.Mailer;
import Utils.Mailer.EmailType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import errors.Error.ParameterType;
import errors.Error.Type;

@CORS
public class BetaInvitations extends Controller {

	public static final Integer MAX_INVITATION_NUMBER = 5;
	
	@BodyParser.Of(BodyParser.Json.class)
    @Transactional
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        JsonNode guestList = root.get("guests");
        if (guestList == null) {
            return new errors.Error(Type.PARAMETERS_ERROR).addParameter("guests", ParameterType.REQUIRED).toResponse();
        }

        BetaInvitation current = BetaInvitation.find.where().eq("createdUser", access.user).findUnique();
	    if (current == null && access.user.isAdmin == false) {
        	return forbidden("No invitations");
	    } else if (current == null) {
	    	current = new BetaInvitation(null, access.user.email, null, null, null, State.CREATED);
	    	current.createdUser = access.user;
	    	current.save();
	    }
		ArrayNode guestsNode = Json.newObject().arrayNode();
    	for (JsonNode guestNode: guestList) {
    		if (current.invitedPeople > MAX_INVITATION_NUMBER && access.user.isAdmin == false) {
    			break;
    		}
    		String mail = guestNode.path("email").textValue();
    		if (mail != null) {
    			BetaInvitation newGuest = BetaInvitation.find.where().eq("email", mail).findUnique();
    			if (newGuest == null) {
    				newGuest = new BetaInvitation(access.user, mail, null, null, null, State.INVITED);
    				newGuest.save();
    				current.invitedPeople++;
    				
    		    	Mailer.get().sendMail(EmailType.BETA_INVITATION, access.getLang(), newGuest.email, ImmutableMap.of("FIRSTNAME", access.user.firstName, "LASTNAME", access.user.lastName));
    				
    				ObjectNode infos = Json.newObject();
    	            infos.put("email", mail);
    	            infos.put("hasAccepted", false);
    	            guestsNode.add(infos);
    			} else if (newGuest.state == State.REQUESTING) {
       				newGuest.originalUser = access.user;
       				newGuest.state = State.CREATED;
       				newGuest.save();
    				current.invitedPeople++;

    				User newUser = new User(newGuest.email, newGuest.password, newGuest.firstName, newGuest.lastName);
    				newUser.password = newGuest.password;
    				newUser.lang = newGuest.lang;
    		        newUser.save();
    				
    		    	Mailer.get().sendMail(EmailType.BETA_REQUEST_ACCEPTED, newUser.lang, newUser.email, ImmutableMap.of("FIRSTNAME", newUser.firstName, "LASTNAME", newUser.lastName));
    				
    				ObjectNode infos = Json.newObject();
    	            infos.put("email", mail);
    	            infos.put("hasAccepted", true);
    	            guestsNode.add(infos);
    			}
    		}
		}
    	current.save();
    	ObjectNode result = Json.newObject();
		result.put("invited", guestsNode);
		result.put("remaining", access.user.isAdmin ? 10 : (MAX_INVITATION_NUMBER - current.invitedPeople));
		return created(result);
    }
    
	@Transactional
    public static Result invitations(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
        if (error != null) {
            return error;
        }
        BetaInvitation current = BetaInvitation.find.where().eq("createdUser", access.user).findUnique();
        if (current == null && access.user.isAdmin == false) {
        	return new errors.Error(errors.Error.Type.NO_INVITATIONS).toResponse();
        } else if (current == null) {
        	current = new BetaInvitation(null, access.user.email, null, null, null, State.CREATED);
        	current.createdUser = access.user;
        	current.save();
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
	result.put("remaining", access.user.isAdmin ? 10 : (MAX_INVITATION_NUMBER - current.invitedPeople));
	return created(result);
    }

	@Transactional
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
	@Transactional
    public static Result validateProcessing(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
        if (error != null) {
            return error;
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        JsonNode guestList = root.get("validated");
        if (guestList == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("validated", ParameterType.REQUIRED).toResponse();
        }

		ArrayNode validatedNode = Json.newObject().arrayNode();
    	for (JsonNode guestNode: guestList) {
    		Integer id = guestNode.path("id").intValue();
    		if (id != null) {
    			BetaInvitation currentGuest = BetaInvitation.find.byId(id);
    			if (currentGuest != null && currentGuest.state == State.REQUESTING) {
    				User newUser = new User(currentGuest.email, currentGuest.password, currentGuest.firstName, currentGuest.lastName);
    				newUser.password = currentGuest.password;
    				newUser.lang = currentGuest.lang;
    		        newUser.save();
    		        
    		        // Beta Handling
    		        currentGuest.createdUser = newUser;
    		        currentGuest.state = State.CREATED;
    		        currentGuest.save();

    		    	Mailer.get().sendMail(EmailType.BETA_REQUEST_ACCEPTED, newUser.lang, newUser.email, ImmutableMap.of("FIRSTNAME", newUser.firstName, "LASTNAME", newUser.lastName));
    				validatedNode.add(id);
    			}
    		}
		}
    	ObjectNode result = Json.newObject();
		result.put("processed", validatedNode);
		return created(result);
    }
}
