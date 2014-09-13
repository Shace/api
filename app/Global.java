import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

	public play.libs.F.Promise<play.mvc.Result> onError(play.mvc.Http.RequestHeader arg0, Throwable arg1) {
		Logger.error(arg1.getStackTrace().toString());
		Logger.error(arg1.getMessage());

		return null;
	};
	
}