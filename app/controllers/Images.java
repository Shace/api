package controllers;

import java.util.List;

import models.File;
import models.Image;
import models.Image.BadFormat;
import models.Image.FormatType;
import models.ImageFileRelation;
import play.libs.Json;
import play.mvc.Controller;
import Utils.Storage;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Controller that handles the different API action applied to an Image
 * @author Loick Michard
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

        result.put("id", image.id);
        result.put("hash", image.hash);
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
    
    public static void	replaceImage(Image image, java.io.File file, FormatType format) throws BadFormat {
		List<ImageFileRelation> fileRelations = ImageFileRelation.find.fetch("file").where().eq("image", image).findList();
		for (ImageFileRelation fileRelation : fileRelations) {
			Storage.deleteFile(fileRelation.file);
		}
		Ebean.delete(fileRelations);
		image.addFile(file, format);
    }
}
