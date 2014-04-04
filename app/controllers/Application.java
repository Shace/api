package controllers;

import models.Event;
import models.Media;
import models.User;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

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
    
    public static Result initDevEnv() {
    	if (Play.application().isProd() == true) {
    		return notFound();
    	}
    	User tmpUser = User.find.where().eq("email", "dev@shace.com").findUnique();
    	if (tmpUser == null) {
    		tmpUser = new User("dev@shace.com", "password");
    	}
    	// ...
    	tmpUser.save();

    	Event tmpEvent = Event.find.byId("dev event");
    	if (tmpEvent == null) {
	    	tmpEvent = new Event("dev event", Event.Privacy.PUBLIC, tmpUser);
    	}
    	tmpEvent.description = "Development Description";
    	tmpEvent.name = "Development Event Example";
    	tmpEvent.save();

    	Media tmpMedia = Media.find.where().eq("name", "dev media").findUnique();
    	if (tmpMedia == null)
    		tmpMedia = new Media(tmpUser, tmpEvent);
    	tmpMedia.name = "dev media";
    	tmpMedia.description = "Development Description";
    	tmpMedia.save();
    	return ok("Development Environment Init");
    }
  
}