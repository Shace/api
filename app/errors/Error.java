package errors;

import java.util.ArrayList;
import java.util.List;

import play.libs.Json;
import play.mvc.Result;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Error {
	private enum Returns {
		BAD_REQUEST,
		NOT_FOUND,
		UNAUTHORIZED,
		FORBIDDEN
	}
	
	private class Parameter {
		public ParameterType 	type;
		public String			field;
	}
	
	public enum ParameterType {
		REQUIRED,
		DUPLICATE,
		FORMAT
	}
	
	public enum Type {

		JSON_REQUIRED			(100, Returns.BAD_REQUEST, "Unexpected format, JSON required"),
		PARAMETERS_ERROR		(101, Returns.BAD_REQUEST, "Error in content parameters"),
		
		TOKEN_NOT_FOUND			(200, Returns.NOT_FOUND, "Token not found"),
		INVALID_IDS				(201, Returns.UNAUTHORIZED, "Invalid user or password"),
		ALREADY_CONNECTED		(202, Returns.BAD_REQUEST, "User already connected"),
		BETA_PROCESSING			(203, Returns.UNAUTHORIZED, "Your request to join the beta is still beeing processed"),
		NO_INVITATIONS			(204, Returns.NOT_FOUND, "No invitations found"),
		REQUEST_BETA_SENT		(205, Returns.FORBIDDEN, "A request to the beta has been sent"),
		ACCESS_TOKEN_REQUIRED	(206, Returns.FORBIDDEN, "Access token required"),
		NEED_AUTHENTICATION		(207, Returns.UNAUTHORIZED, "You need to be authenticated"),
		NEED_ANONYMOUS			(208, Returns.UNAUTHORIZED, "You cannot be connected"),
		NEED_ADMIN				(209, Returns.FORBIDDEN, "You need to be administrator"),
		ACCESS_TOKEN_ERROR		(210, Returns.BAD_REQUEST, "Access token error"),
		FORBIDDEN_TOKEN			(211, Returns.FORBIDDEN, "This token is forbidden"),
		
		LANGUAGE_NOT_FOUND		(300, Returns.NOT_FOUND, "Language not found"),
		
		EVENT_NOT_FOUND			(400, Returns.NOT_FOUND, "Event not found"),
		MEDIA_NOT_FOUND			(401, Returns.NOT_FOUND, "Media not found"),
		COMMENT_NOT_FOUND		(402, Returns.NOT_FOUND, "Comment not found"),
		NEED_ADMINISTRATE		(403, Returns.FORBIDDEN, "You need administrate rights"),
		NO_PASSWORD				(404, Returns.BAD_REQUEST, "You cannot request an access with a password on this event"),
		WRONG_PASSWORD			(405, Returns.FORBIDDEN, "Wrong password"),
		READING_TOO_STRONG		(406, Returns.BAD_REQUEST, "The writing privacy cannot match the reading privacy"),
		EMPTY_MEDIA_LIST		(407, Returns.BAD_REQUEST, "Empty / invalid media list"),
		NEED_OWNER				(408, Returns.FORBIDDEN, "You need owner rights"),
		BAD_FORMAT_IMAGE		(409, Returns.BAD_REQUEST, "Bad image format"),
		TAG_NOT_FOUND			(410, Returns.NOT_FOUND, "Tag not found"),
		NEED_PASSWORD			(411, Returns.FORBIDDEN, "You need a password for this event"),
		EVENT_FORBIDDEN			(412, Returns.FORBIDDEN, "You dont have the required permission for this event"),
		USER_FORBIDDEN			(413, Returns.FORBIDDEN, "You dont have the required permission for this user"),
		ONLY_ONE_REPORT			(414, Returns.FORBIDDEN, "Only one report per media per user is authorized"),
		IMAGE_NOT_FOUND			(415, Returns.NOT_FOUND, "Image not found"),
		
		USER_NOT_FOUND			(500, Returns.NOT_FOUND, "User not found")
		
		
		;

		public int code;
		public String description;
		public Returns returns;

		private Type(int code, Returns returns, String description) {
			this.code = code;
			this.description = description;
			this.returns = returns;
		}

	}

	private Type 			type;
	private List<Parameter>	parameters = new ArrayList<Parameter>();

	public Error(Type type) {
		this.setType(type);
	}

	public Result toResponse() {
		ObjectNode result = Json.newObject();

		ObjectNode error = Json.newObject();
		result.put("error", error);
		error.put("code", type.code);
		error.put("type", type.description);
		
		if (this.parameters.size() > 0) {
			ObjectNode params = Json.newObject();
			for (Parameter param : this.parameters) {
				params.put(param.field, param.type.toString().toLowerCase());
			}
			error.put("parameters", params);
		}

		if (type.returns == Returns.BAD_REQUEST) {
			return play.mvc.Results.badRequest(result);
		}
		switch (type.returns) {
			case BAD_REQUEST:
				return play.mvc.Results.badRequest(result);
			case FORBIDDEN:
				return play.mvc.Results.forbidden(result);
			case NOT_FOUND:
				return play.mvc.Results.notFound(result);
			case UNAUTHORIZED:
				return play.mvc.Results.unauthorized(result);
			default:
				return play.mvc.Results.badRequest(result);
		}
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public Error addParameter(String field, ParameterType type) {
		Parameter param = new Parameter();
		param.field = field;
		param.type = type;
		this.parameters.add(param);
		
		return this;
	}
	
	public boolean isParameterError() {
		return this.parameters.size() > 0;
	}
}
