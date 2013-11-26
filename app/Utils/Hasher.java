package Utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

//TODO: Doc

public class Hasher {

	public static String hash(String str) {
    	MessageDigest cript;
		try {
			cript = MessageDigest.getInstance("SHA-1");
			cript.reset();
	        cript.update(str.getBytes("utf8"));
	    	return new String(Hex.encodeHex(cript.digest()));
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

}
