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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import org.imgscalr.Scalr;

import play.Logger;
import play.db.ebean.Model;
import Utils.ImageFormats;
import Utils.Storage;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;

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

    public Image(User owner) {
        this.creation = new Date();
        this.owner = owner;
        this.hash = UUID.randomUUID().toString();
        this.latitude = 0;
        this.longitude = 0;
    }
    
    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer       id;
    
    @Column(unique=true)
    public String		hash;
    
    public Date         creation;
    
	@ManyToOne
	@JoinColumn(name="owner_id")
	public User			owner;

	@Version
    Timestamp updateTime;
	
    @OneToMany(mappedBy="image", cascade=CascadeType.ALL)
    public List<ImageFileRelation>   files;
    
    public static Image create(User owner) {
        Image image = new Image(owner);
        image.save();
        
        image.files = new ArrayList<>();
        return image;
    }
    
    @OneToMany(mappedBy="image", cascade=CascadeType.ALL)
    @OrderBy("creation")
    public List<Report> reports;

    public float latitude;
    
    public float longitude;
    
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
//                Logger.debug("Reading image : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                partialStartTime = System.nanoTime();
            }
            /* 
             * Handle EXIF orientation
             */
            int orientation = 1;
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                readGPSInfo(metadata);
                Logger.debug("Lat : " + this.latitude + " | Lon : " + this.longitude);
                Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
                if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
            } catch (Exception e) {
                Logger.debug(e.toString());
            }
            
            {
            	long partialEstimatedTime = System.nanoTime() - partialStartTime;
//                Logger.debug("Getting metadata : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
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
//                Logger.debug("Rotate (transform) image : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
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
//                        Logger.debug("Resize image " + format.name + " : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                        partialStartTime = System.nanoTime();
                    }
            		this.files.add(ImageFileRelation.create(this, models.File.create(Storage.storeImage(resized, format), Storage.getBaseUrl()), format.width, format.height, format.name));
            		{
                    	long partialEstimatedTime = System.nanoTime() - partialStartTime;
//                        Logger.debug("Store image " + format.name + " : " + Long.toString(partialEstimatedTime / 1000000) + "ms");
                        partialStartTime = System.nanoTime();
                    }
            	}
            }
                        
            this.save();
            
            long estimatedTime = System.nanoTime() - startTime;
//            Logger.debug("Time elapsed to store picture : " + Long.toString(estimatedTime / 1000000) + "ms");

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
    	if (!crop && original.getWidth() < width && original.getHeight() < height) {
    		return original;
    	}
    	BufferedImage resized = null;
    	if (crop) {
    		if ((float)original.getWidth() / (float)width > (float)original.getHeight() / (float)height) {
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
    
    private static float convertToDegree(String stringDMS){
    	float result = 0;
    	String[] DMS = stringDMS.split(" ", 3);

    	try {
    		String[] stringD = DMS[0].split("/", 2);
    		double D0 = Double.parseDouble(stringD[0]);
    		double D1 = Double.parseDouble(stringD[1]);
    		double floatD = D0/D1;

    		String[] stringM = DMS[1].split("/", 2);
    		double M0 = Double.parseDouble(stringM[0]);
    		double M1 = Double.parseDouble(stringM[1]);
    		double floatM = M0/M1;

    		String[] stringS = DMS[2].split("/", 2);
    		Double S0 = Double.parseDouble(stringS[0]);
    		Double S1 = Double.parseDouble(stringS[1]);
    		Double floatS = S0/S1;

    		result = (float) (floatD + (floatM / 60) + (floatS / 3600));
    		return result;
    	} catch (Exception e) {
    		Logger.debug(e.toString());
    		return 0;
    	}
    };

    private void	readGPSInfo(Metadata metadata) {
        Directory gpsDir = metadata.getDirectory(GpsDirectory.class);
        if (gpsDir != null) {
        	String lat = gpsDir.getString(GpsDirectory.TAG_GPS_LATITUDE);
        	String latRef = gpsDir.getString(GpsDirectory.TAG_GPS_LATITUDE_REF);
        	String lon = gpsDir.getString(GpsDirectory.TAG_GPS_LONGITUDE);
        	String lonRef = gpsDir.getString(GpsDirectory.TAG_GPS_LONGITUDE_REF);

        	float latitude = 0, longitude = 0;

        	if (lat != null && latRef != null && lon != null && lonRef != null)
        	{
        		if (latRef.equals("N")){
        			latitude = convertToDegree(lat);
        		} else {
        			latitude = 0 - convertToDegree(lat);
        		}
        		if (lonRef.equals("E")){
        			longitude = convertToDegree(lon);
        		} else{
        			longitude = 0 - convertToDegree(lon);
        		}
        	}
        	this.latitude = latitude;
        	this.longitude = longitude;
        }
    }
    	
	public static Finder<String, Image> find = new Finder<String, Image>(
			String.class, Image.class
	);
}
