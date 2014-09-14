package models;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.imgscalr.Scalr;

import play.Logger;
import play.db.ebean.Model;
import Utils.ImageFormats;
import Utils.Storage;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;

@Entity
@Table(name="se_image")
public class Image extends Model {
	
	public static enum FormatType {
		GALLERY,
		COVER,
		PROFILE_PICTURE
	}

    public static class BadFormat extends IOException {
        private static final long serialVersionUID = 2619200964548042413L;
        
        public BadFormat(String message) {
            super(message);
        }
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 6727022222330879650L;

    public Image() {
        this.creation = new Date();
    }
    
    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;
    
    public Date         creation;
    
    @OneToMany(mappedBy="image", cascade=CascadeType.ALL)
    public List<ImageFileRelation>   files;
    
    public static Image create() {
        Image image = new Image();
        image.save();
        
        image.files = new ArrayList<>();
        return image;
    }
    
    /**
     * Generate and store all formats images given a file
     * @param file File containing an image in a valid format
     * @throws BadFormat If a problem occurs with image format or resizing
     */
    public void addFile(File file, FormatType formatType) throws BadFormat {
        try {
            long startTime = System.nanoTime();
            long partialStartTime = System.nanoTime();
            
            final int[] RGB_MASKS = {0xFF0000, 0xFF00, 0xFF};
            final ColorModel RGB_OPAQUE =
                new DirectColorModel(32, RGB_MASKS[0], RGB_MASKS[1], RGB_MASKS[2]);
            
            java.awt.Image img = Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath());
            PixelGrabber pg = new PixelGrabber(img, 0, 0, -1, -1, true);
            pg.grabPixels();
            int width = pg.getWidth(), height = pg.getHeight();

            DataBuffer buffer = new DataBufferInt((int[]) pg.getPixels(), pg.getWidth() * pg.getHeight());
            WritableRaster raster = Raster.createPackedRaster(buffer, width, height, width, RGB_MASKS, null);
           	BufferedImage original = new BufferedImage(RGB_OPAQUE, raster, false, null);
            
            {
            	long partialEstimatedTime = System.nanoTime() - partialStartTime;
                Logger.debug("Reading image : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                partialStartTime = System.nanoTime();
            }
            /* 
             * Handle EXIF orientation
             */
            int orientation = 1;
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
                if (directory != null)
                    orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            } catch (ImageProcessingException e) {
            } catch (MetadataException e) {
            }
            
            {
            	long partialEstimatedTime = System.nanoTime() - partialStartTime;
                Logger.debug("Getting metadata : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                partialStartTime = System.nanoTime();
            }

            /* 
             * Rotate image if needed in EXIF orientation
             */
            if (orientation != 1) {
                original = transformImage(original, orientation);
            }
            
            {
            	long partialEstimatedTime = System.nanoTime() - partialStartTime;
                Logger.debug("Rotate (transform) image : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                partialStartTime = System.nanoTime();
            }
            
            /*
             * Generate all formats of stored images
             */
            for (ImageFormat format : ImageFormats.get().formats) {
            	if (format.type == formatType) {
            		BufferedImage resized = resizeImage(original, format.width, format.height, format.crop);
            		{
                    	long partialEstimatedTime = System.nanoTime() - partialStartTime;
                        Logger.debug("Resize image " + format.name + " : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                        partialStartTime = System.nanoTime();
                    }
            		this.files.add(ImageFileRelation.create(this, models.File.create(Storage.storeImage(resized, format), Storage.getBaseUrl()), format.width, format.height, format.name));
            		{
                    	long partialEstimatedTime = System.nanoTime() - partialStartTime;
                        Logger.debug("Store image " + format.name + " : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                        partialStartTime = System.nanoTime();
                    }
            	}
            }
                        
            this.save();
            
            long estimatedTime = System.nanoTime() - startTime;
            Logger.debug("Time elapsed to store picture : " + Long.toString(estimatedTime / 1000000) + "ms");

        } catch (IOException | InterruptedException e) {
            throw new BadFormat(e.getMessage());
        }
    }
    
    /**
     * Resize an original image and return the new one
     * @param original Original image
     * @param width Width of the new image
     * @param height Height of the new image
     * @param crop If true, original image will be cropped
     * @return resized image
     */
    private BufferedImage resizeImage(BufferedImage original, int width, int height, boolean crop) {
    	if (original.getWidth() < width && original.getHeight() < height) {
    		return original;
    	}
    	BufferedImage resized = null;
    	if (crop && width == height) {
    		if (original.getWidth() > original.getHeight()) {
        		resized = Scalr.resize(original, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_HEIGHT, width, height);
    		} else {
        		resized = Scalr.resize(original, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_WIDTH, width, height);
    		}
    	} else {
    		resized = Scalr.resize(original, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, width, height);
    	}
    	if (resized.getWidth() > width) {
			resized = Scalr.crop(resized, (resized.getWidth() - width) / 2, 0, width, resized.getHeight());
    	}
    	if (resized.getHeight() > height) {
			resized = Scalr.crop(resized, 0, (resized.getHeight() - height) / 2, resized.getWidth(), height);
    	}
    	return resized;
    }
    
    /**
     * Apply an affine transform to an image and return the new one
     * @param image Original image
     * @param transform affine transform to apply
     * @return Transformed image
     */
    public static BufferedImage transformImage(BufferedImage image, int transform) {
        switch (transform) {
        case 1:
            break;
        case 2: // Flip X
            image = Scalr.rotate(image, Scalr.Rotation.FLIP_HORZ);
            break;
        case 3: // PI rotation 
            image = Scalr.rotate(image, Scalr.Rotation.CW_180);
            break;
        case 4: // Flip Y
            image = Scalr.rotate(image, Scalr.Rotation.FLIP_VERT);
            break;
        case 5: // - PI/2 and Flip X
            image = Scalr.rotate(image, Scalr.Rotation.CW_90);
            image = Scalr.rotate(image, Scalr.Rotation.FLIP_HORZ);
            break;
        case 6: // -PI/2 and -width
            image = Scalr.rotate(image, Scalr.Rotation.CW_90);
            break;
        case 7: // PI/2 and Flip
            image = Scalr.rotate(image, Scalr.Rotation.CW_270);
            image = Scalr.rotate(image, Scalr.Rotation.FLIP_HORZ);
            break;
        case 8: // PI / 2
            image = Scalr.rotate(image, Scalr.Rotation.CW_270);
            break;
        }
        return image;
    }
}
