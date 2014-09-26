package Utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import models.File.Type;
import models.Image;
import models.ImageFormat;
import play.Logger;
import play.Play;
import plugins.S3Plugin;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class Storage {
    /**
     * Store image on Shace depending on LOCAL_STORAGE value
     * @param image Image to store
     * @param format Format of the image to store
     * @return The name of the created image
     * @throws IOException
     */
    public static String storeImage(BufferedImage image, ImageFormat format) throws IOException {
        String extension = UUID.randomUUID() + ".jpg";
        /* If you think it could generate two times the same UUID, you will be hit by a meteorite soon */

        if (S3Plugin.amazonS3 == null) {
            storeLocal(image, extension);
        } else {
            extension = format.name + "/" + extension;
            storeAmazonS3(image, extension);
        }
        
        return extension;
    }
    
    /**
     * Delete a file from Shace depending on LOCAL_STORAGE value
     * @param file File to delete
     */
    public static boolean deleteFile(models.File file) {
    	if (file != null && file.type == Type.Local) {

//    		File toDelete = new File("C:/Users/samuel/Documents/Dev/ShaceEvent/medias/test.jpg");
    		File toDelete = new File(Play.application().configuration().getString("storage.path") + File.separator + file.uid);
    		Logger.debug(Play.application().configuration().getString("storage.path") + File.separator + file.uid);
    		Logger.debug("Exists : " + toDelete.exists());
    		
    		boolean res = false;
    		try {
    			res = toDelete.delete();
    		} catch (Exception e) {
    			Logger.debug("Exception : " + e);
    		}
    		Logger.debug("Return : " + res);
    		return res;
    	}
    	return false;
    }
    
    private static void storeLocal(BufferedImage image, String extension) throws IOException {
        long partialStartTime = System.nanoTime();
        File file = new File(Play.application().configuration().getString("storage.path"), extension);
        //ImageIO.write(image, "JPEG", file);
        
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            throw new Image.BadFormat("Error with image writer");
        ImageWriter writer = (ImageWriter) writers.next();
        FileOutputStream outStream = new FileOutputStream(file);
        ImageOutputStream ios = ImageIO.createImageOutputStream(outStream);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.9f);
        
        {
        	long partialEstimatedTime = System.nanoTime() - partialStartTime;
            Logger.debug("Compress image : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
            partialStartTime = System.nanoTime();
        }

        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();
        outStream.close();
    }
    
    private static void storeAmazonS3(BufferedImage image, String extension) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext())
            throw new Image.BadFormat("Error with image writer");
        ImageWriter writer = (ImageWriter) writers.next();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1.0f);

        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        
        byte[] buffer = os.toByteArray();
        InputStream is = new ByteArrayInputStream(buffer);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(buffer.length);
        meta.setContentType("image/jpeg");

        /*
         * Send image to Amazon E3
         */
        PutObjectRequest putObjectRequest = new PutObjectRequest(S3Plugin.s3Bucket, extension, is, meta);
        putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
        S3Plugin.amazonS3.putObject(putObjectRequest);
    }

    public static String getUrl(String baseURL, String uid) {
        return baseURL + uid;
    }
    
    public static String getBaseUrl() {
        if (S3Plugin.amazonS3 == null)
            return "http:" + Play.application().configuration().getString("storage.baseurl");
        else
            return "https://s3.amazonaws.com/" + S3Plugin.s3Bucket + "/";
    }
}
