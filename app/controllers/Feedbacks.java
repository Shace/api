package controllers;

import java.util.List;

import models.AccessToken;
import models.BetaInvitation;
import models.Feedback;
import models.User;
import models.BetaInvitation.State;
import Utils.Access;
import Utils.Mailer;
import Utils.Mailer.EmailType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import errors.Error.ParameterType;
import errors.Error.Type;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

@CORS
public class Feedbacks extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
    public static Result add(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ANONYMOUS_USER);
        if (error != null) {
            return error;
        }

        JsonNode root = request().body().asJson();
        if (root == null) {
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        JsonNode email = root.get("email");
        if (access.isConnectedUser() == false && email == null) {
            return new errors.Error(Type.PARAMETERS_ERROR).addParameter("email", ParameterType.REQUIRED).toResponse();
        }
        
        JsonNode description = root.get("description");
        if (description == null) {
            return new errors.Error(Type.PARAMETERS_ERROR).addParameter("description", ParameterType.REQUIRED).toResponse();
        }
        
        JsonNode okForAnswer = root.get("okForAnswer");
        if (okForAnswer == null) {
            return new errors.Error(Type.PARAMETERS_ERROR).addParameter("okForAnswer", ParameterType.REQUIRED).toResponse();
        }
        
        Feedback feedback = new Feedback(email == null ? access.user.email : email.textValue(), access.user, description.textValue(), okForAnswer.booleanValue());
        feedback.save();
		return created();
    }
    
    public static Result adminList(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
        if (error != null) {
            return error;
        }
        List<Feedback> feedbackList = Feedback.find.findList();
		ArrayNode feedbacksNode = Json.newObject().arrayNode();
    	for (Feedback feedback : feedbackList) {
    		ObjectNode infos = Json.newObject();
    		infos.put("id", feedback.id);
    		infos.put("senderEmail", feedback.senderEmail);
    		if (feedback.senderUser != null) {
        		infos.put("senderUser", feedback.senderUser.id);
    		}
    		infos.put("description", feedback.description);
    		infos.put("okForAnswer", feedback.okForAnswer);
    		infos.put("creationDate", feedback.creationDate.getTime());
    		infos.put("adminRead", feedback.adminRead);
    		feedbacksNode.add(infos);
		}
    	ObjectNode result = Json.newObject();
		result.put("feedbacks", feedbacksNode);
		return created(result);
    }
    
    public static Result processingList(String accessToken) {
        AccessToken access = AccessTokens.access(accessToken);
        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
        if (error != null) {
            return error;
        }
        List<Feedback> feedbackList = Feedback.find.where().eq("adminRead", false).findList();
		ArrayNode feedbacksNode = Json.newObject().arrayNode();
    	for (Feedback feedback : feedbackList) {
    		ObjectNode infos = Json.newObject();
    		infos.put("id", feedback.id);
    		infos.put("senderEmail", feedback.senderEmail);
    		if (feedback.senderUser != null) {
        		infos.put("senderUser", feedback.senderUser.id);
    		}
    		infos.put("description", feedback.description);
    		infos.put("okForAnswer", feedback.okForAnswer);
    		infos.put("creationDate", feedback.creationDate.getTime());
    		infos.put("adminRead", feedback.adminRead);    		
    		feedbacksNode.add(infos);
		}
    	ObjectNode result = Json.newObject();
		result.put("processing", feedbacksNode);
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
        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
        }
        
        JsonNode feedbackList = root.get("validated");
        if (feedbackList == null) {
        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("validated", ParameterType.REQUIRED).toResponse();
        }

		ArrayNode validatedNode = Json.newObject().arrayNode();
    	for (JsonNode feedbackNode: feedbackList) {
    		JsonNode id = feedbackNode.get("id");
    		if (id != null) {
    			Feedback currentFeedback = Feedback.find.byId(id.intValue());
    			if (currentFeedback != null && currentFeedback.adminRead == false) {
    		        currentFeedback.adminRead = true;
    		        currentFeedback.save();

    		        String answer = feedbackNode.get("answer").textValue();
    		        if (answer != null && currentFeedback.okForAnswer == true) {
    		        	String subject = "[Shace] Re : Feedback";
    		        	String content = answer + "<br/><br/><br/>Feedback :<br/><i>" + currentFeedback.description + "</i>";
        		    	Mailer.get().sendMail(currentFeedback.senderEmail, subject, content, "Shace <noreply@shace.io>");
    		        }
    				validatedNode.add(id);
    			}
    		}
		}
    	ObjectNode result = Json.newObject();
		result.put("processed", validatedNode);
		return created(result);
    }
}
