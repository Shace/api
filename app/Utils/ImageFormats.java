package Utils;

import java.util.List;

import models.ImageFormat;

public class ImageFormats {
    private static final ImageFormats instance = new ImageFormats();
    
    private ImageFormats() {
        formats = ImageFormat.find.findList();
    }
    
    public static ImageFormats get() {
        return instance;
    }
    
    public List<ImageFormat> formats;
}
