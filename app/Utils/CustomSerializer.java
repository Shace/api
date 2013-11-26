package Utils;

import java.net.URI;

import play.db.ebean.Model;
import flexjson.JSONSerializer;

public class CustomSerializer {
	
	static public String	serialize(Object obj, String fields) {
		if (fields == null) {
			fields = "";
		} else if (fields.startsWith("\"") && fields.endsWith("\"")) {
			fields = fields.substring(1, fields.length() - 1);
		}
		return new JSONSerializer().
				prettyPrint(true).
				transform(new ModelTransformer(), Model.class).
				transform(new ToStringTransformer(), URI.class).
				include(fields).
				serialize(obj);
	}

}
