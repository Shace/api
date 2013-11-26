package Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import play.mvc.Http.Request;

// TODO: doc
public class RequestParameters {
	public enum Depth {
		NONE,
		MINIMAL,
		PARTIAL,
		COMPLETE
	}
	
	public enum Field {
		DEFAULT,
		ALL,
		SPECIFIED
	}

	public RequestParameters(Request request) throws NullPointerException {
		if (request == null) {
			throw new NullPointerException();
		}
		String checkedDepth = request.getQueryString("depth");
		if (checkedDepth != null) {
			depth = Depth.valueOf(checkedDepth);
		} else {
			depth = defaultDepth;
		}

		String checkedFields = request.getQueryString("fields");
		if (checkedFields != null) {
			Field[] fieldsTypes = Field.values();
			fieldType = Field.SPECIFIED;
			for (int i = 0; i < fieldsTypes.length; i++) {
				Field current = fieldsTypes[i];
				if (checkedFields.compareToIgnoreCase("[" + current.toString() + "]") == 0) {
					fieldType = current;
					break;
				}
			}
			if (fieldType == Field.SPECIFIED) {
				requiredFields = Arrays.asList(checkedFields.split(","));
				if (requiredFields.size() == 0) {
					fieldType = Field.DEFAULT;
					requiredFields = null;
				}
			} else {
				requiredFields = null;
			}
		} else {
			fieldType = Field.DEFAULT;
		}
	}
	
	public RequestParameters(Depth depth, Field fieldType, List<String> fields) {
		this.depth = depth;
		this.fieldType = fieldType;
		if (fieldType == Field.SPECIFIED)
			this.requiredFields = fields;
	}

	public RequestParameters	depthField(String field) {
		Depth newDepth;
		Field newFieldType = fieldType();
		List<String> newFields = null;

		if (depth == Depth.MINIMAL || depth == Depth.PARTIAL) {
			newDepth = Depth.MINIMAL;
		} else {
			newDepth = Depth.PARTIAL;
		}
		if (fieldType == Field.SPECIFIED) {
			newFields = childFields(field);
			if (newFields != null) {
				newFieldType = Field.SPECIFIED;
			} else {
				newFieldType = Field.DEFAULT;
			}
		}
		return new RequestParameters(newDepth, newFieldType, newFields);
	}

	public Depth	depth() {
		return depth;
	}
	
	public Field	fieldType() {
		return fieldType;
	}
	
	public List<String>	fields() {
		return requiredFields;
	}
	
	public boolean	isFieldRequired(String fieldName) {
		if (requiredFields == null) {
			return fieldType == Field.ALL;
		} else {
			return requiredFields.contains(fieldName);
		}
	}
	
	public static RequestParameters	create(Request request) {
		try {
			RequestParameters res = new RequestParameters(request);
			return res;
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	private List<String> childFields(String field) {
		if (fieldType != Field.SPECIFIED || requiredFields == null)
			return null;
		List<String> res = new ArrayList<String>();
		field = field.toLowerCase();
		for (String current : requiredFields) {
			current = current.toLowerCase();
			if (current.startsWith(field + ".") && current.length() > field.length() + 1) {
				res.add(current.substring(field.length() + 1));
			}
		}
		if (res.size() == 0)
			return null;
		return res;
	}
	
	private static Depth	defaultDepth = Depth.MINIMAL;

	private Depth			depth;
	private Field			fieldType;
	private List<String>	requiredFields;
}
