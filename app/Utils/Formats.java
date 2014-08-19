package Utils;

import play.data.validation.Constraints.EmailValidator;


public class Formats {
	public static boolean isValidEmail(String email) {
		EmailValidator validator = new EmailValidator();
		
		return validator.isValid(email);
	}
}
