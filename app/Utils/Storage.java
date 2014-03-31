package Utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import models.Image;
import play.Play;

public class Storage {
    public static String storeImage(BufferedImage image) throws IOException {
        String extension = UUID.randomUUID() + ".jpg";
        File file = new File(Play.application().configuration().getString("storage.path"), extension);        
        /* If you think it could generate two times the same UUID, you will be hit by a meteorite soon */
                
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            throw new Image.BadFormat("Error with image writer");
        ImageWriter writer = (ImageWriter) writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(new FileOutputStream(file));
        writer.setOutput(ios);
        
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1.0f);

        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        
        return extension;
    }

    public static String getUrl(String uid) {
        return Play.application().configuration().getString("storage.baseurl") + uid;
    }
}
