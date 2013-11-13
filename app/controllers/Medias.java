package controllers;

import play.mvc.*;

public class Medias extends Controller {

	/**
	 * List all visible medias
	 */
    public static Result medias() {
    	return TODO;
    }

    /**
     * Add a media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result add() {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Delete a media
     */
    public static Result delete(String uri) {
    	return TODO;
    }
    
    /**
     * Update a media
     */
    @BodyParser.Of(BodyParser.Json.class)
    public static Result update(String uri) {
    	//JsonNode json = request().body().asJson();
		return TODO;
    }
    
    /**
     * Get media information
     */
    public static Result media(String uri) {
    	return TODO;
    }
}
