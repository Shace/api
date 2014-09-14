package controllers;

import models.File;
import models.Image;
import models.ImageFileRelation;
import play.libs.Json;
import play.mvc.Controller;
import Utils.Storage;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Controller that handles the different API action applied to an Image
 * @author Loick Michard
 * @category controllers
 */
@CORS
public class Images extends Controller {
    
    /**
     * Convert an Image to a JSON object.
     * 
     * @param image A Image object to convert
     * @return The JSON object containing the user information
     */
    public static ObjectNode getImageObjectNode(Image image) {
        ObjectNode result = Json.newObject();

        result.put("creation", image.creation.getTime());
        if (image.files != null) {
            for (ImageFileRelation ifr : image.files) {
                if (ifr != null && ifr.file != null) {
                	if (ifr.file.type == File.Type.Amazon) {
                		result.put(ifr.format, Storage.getUrl(ifr.file.baseURL, ifr.file.uid));
                	} else if (ifr.file.type == File.Type.Local) {
                		result.put(ifr.format, Storage.getUrl(Storage.getBaseUrl(), ifr.file.uid));
                	}
                }
            }
        }

        return result;
    }
}
