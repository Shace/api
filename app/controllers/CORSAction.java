package controllers;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

public class CORSAction extends Action.Simple {

    @Override
    public Promise<SimpleResult> call(Context context) throws Throwable {
        context.response().setHeader("Access-Control-Allow-Origin", "*");
        return delegate.call(context);
    }

}