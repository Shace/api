package models;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.imgscalr.Scalr;

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
    public void addFile(File file) throws BadFormat {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null)
                throw new BadFormat("Error with original image");

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

            /* 
             * Rotate image if needed in EXIF orientation
             */
            if (orientation != 1) {
                original = transformImage(original, orientation);
            }
            
            /*
             * Generate all formats of stored images
             */
            for (ImageFormat format : ImageFormats.get().formats) {
                BufferedImage resized = resizeImage(original, format.width, format.height, format.crop);
                this.files.add(ImageFileRelation.create(this, models.File.create(Storage.storeImage(resized, format), Storage.getBaseUrl()), format.width, format.height, format.name));
            }
            this.save();
        } catch (IOException e) {
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
        return Scalr.resize(original, Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, width, height);
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
