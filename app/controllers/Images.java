package controllers;

import models.Image;
import models.ImageFileRelation;
import play.libs.Json;
import play.mvc.Controller;
import Utils.Storage;

import com.fasterxml.jackson.databind.node.ObjectNode;

@CORS
public class Images extends Controller {
    public static ObjectNode getImageObjectNode(Image image) {
        ObjectNode result = Json.newObject();

        result.put("creation", image.creation.getTime());
        if (image.files != null) {
            for (ImageFileRelation ifr : image.files) {
                if (ifr.file != null && ifr != null)
                    result.put(ifr.format, Storage.getUrl(ifr.file.uid));
            }
        }

        return result;
    }
}
