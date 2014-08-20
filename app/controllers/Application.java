package controllers;

import models.AccessToken.Lang;
import play.mvc.Controller;
import play.mvc.Result;
import Utils.Mailer;
import Utils.Mailer.EmailType;

import com.google.common.collect.ImmutableMap;

/**
 * Main class that represent the application
 * @author Loick Michard
 * @category controllers
 */
@CORS
public class Application extends Controller {

    public static Result index() {
        return ok("Shace Event API");
    }

    public static Result checkPreFlight(String opt) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        response().setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
        response().setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization, X-Auth-Token");

        return ok();
    }
}