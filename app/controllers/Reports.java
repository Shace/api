package controllers;

import java.util.List;

import models.AccessToken;
import models.Image;
import models.Report;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Access;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import errors.Error.ParameterType;
import errors.Error.Type;

@CORS
public class Reports extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
	@Transactional
	public static Result addOnImage(String accessToken, String imageHash) {
		AccessToken access = AccessTokens.access(accessToken);
		Result error = Access.checkAuthentication(access, Access.AuthenticationType.CONNECTED_USER);
		if (error != null) {
			return error;
		}

		JsonNode root = request().body().asJson();
		if (root == null) {
			return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
		}

		Image	currentImage = Image.find.where().eq("hash", imageHash).findUnique();
		if (currentImage == null) {
			return new errors.Error(errors.Error.Type.IMAGE_NOT_FOUND).toResponse();
		}

		Report existing = Report.find.where().eq("creator", access.user).eq("image", currentImage).findUnique();
		if (existing != null) {
			return new errors.Error(Type.ONLY_ONE_REPORT).toResponse();
		}
		String typeStr = root.path("type").textValue();
		if (typeStr == null) {
			return new errors.Error(Type.PARAMETERS_ERROR).addParameter("type", ParameterType.REQUIRED).toResponse();
		}

		Report.Type type;
		try {
			type = Report.Type.valueOf(typeStr);
		} catch (Exception e) {
			return new errors.Error(Type.PARAMETERS_ERROR).addParameter("type", ParameterType.FORMAT).toResponse();
		}

		String reason = root.path("reason").textValue();
		Report report = new Report(access.user, type, reason == null ? "" : reason);
		report.image = currentImage;
		report.save();
		return created();
	}

	@Transactional
	public static Result adminList(String accessToken) {
		AccessToken access = AccessTokens.access(accessToken);
		Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
		if (error != null) {
			return error;
		}

		String sql   
		= " select id, type, reason, user_id, creation, image_id"  
				+ " from se_report"
				+ " order by image_id, creation";

		RawSql rawSql = RawSqlBuilder.parse(sql)
				.columnMapping("user_id", "creator.id")
				.columnMapping("image_id", "image.id")
				.create();
		Query<Report> query = Ebean.find(Report.class);
		query.setRawSql(rawSql);

		List<Report> imageList = query.findList();

		ObjectNode currentNode = null;
		ArrayNode currentReportArray = null;

		Integer currentImageId = -1;
		ArrayNode imagesNode = Json.newObject().arrayNode();
		for (Report report : imageList) {
			if (!currentImageId.equals(report.image.id)) {
				if (currentNode != null) {
					currentNode.put("reports", currentReportArray);
					imagesNode.add(currentNode);
				}
				currentNode = Json.newObject();
				currentImageId = report.image.id;
				currentNode.put("image", Images.getImageObjectNode(report.image));
				currentReportArray = Json.newObject().arrayNode();
			}
			ObjectNode infos = Json.newObject();
			infos.put("id", report.id);
			infos.put("type", report.type.toString().toLowerCase());
			infos.put("sender", report.creator.id);
			infos.put("reason", report.reason);
			infos.put("creation_date", report.creation.getTime());
//			infos.put("adminRead", feedback.adminRead);
			currentReportArray.add(infos);
		}
		currentNode.put("reports", currentReportArray);
		imagesNode.add(currentNode);

		ObjectNode result = Json.newObject();
		result.put("image_reports", imagesNode);
		return created(result);
	}
	//    
	//	@BodyParser.Of(BodyParser.Json.class)
	//	@Transactional
	//    public static Result validateProcessing(String accessToken) {
	//        AccessToken access = AccessTokens.access(accessToken);
	//        Result error = Access.checkAuthentication(access, Access.AuthenticationType.ADMIN_USER);
	//        if (error != null) {
	//            return error;
	//        }
	//
	//        JsonNode root = request().body().asJson();
	//        if (root == null) {
	//        	return new errors.Error(errors.Error.Type.JSON_REQUIRED).toResponse();
	//        }
	//        
	//        JsonNode feedbackList = root.get("validated");
	//        if (feedbackList == null) {
	//        	return new errors.Error(Type.PARAMETERS_ERROR).addParameter("validated", ParameterType.REQUIRED).toResponse();
	//        }
	//
	//		ArrayNode validatedNode = Json.newObject().arrayNode();
	//    	for (JsonNode feedbackNode: feedbackList) {
	//    		JsonNode id = feedbackNode.get("id");
	//    		if (id != null) {
	//    			Feedback currentFeedback = Feedback.find.byId(id.intValue());
	//    			if (currentFeedback != null && currentFeedback.adminRead == false) {
	//    		        currentFeedback.adminRead = true;
	//    		        currentFeedback.save();
	//
	//    		        String answer = feedbackNode.get("answer").textValue();
	//    		        if (answer != null && currentFeedback.okForAnswer == true) {
	//    		        	String subject = "[Shace] Re : Feedback";
	//    		        	String content = answer + "<br/><br/><br/>Feedback :<br/><i>" + currentFeedback.description + "</i>";
	//        		    	Mailer.get().sendMail(currentFeedback.senderEmail, subject, content, "Shace <noreply@shace.io>");
	//    		        }
	//    				validatedNode.add(id);
	//    			}
	//    		}
	//		}
	//    	ObjectNode result = Json.newObject();
	//		result.put("processed", validatedNode);
	//		return created(result);
	//    }
}
