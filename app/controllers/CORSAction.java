package controllers;

import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

/**
 * Special action to handle CORS request
 * @author Loick Michard
 * @category controllers
 */
public class CORSAction extends play.mvc.Action.Simple {
	
    public F.Promise<Result> call(Http.Context context) throws Throwable {
        context.response().setHeader("Access-Control-Allow-Origin", "*");
        return delegate.call(context);
    }

}