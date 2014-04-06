package models;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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
             * Handle EXIF orientation (and EXIF information, date, ... in the future
             */
            int orientation = 1;
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            } catch (ImageProcessingException e) {
                e.printStackTrace();
            } catch (MetadataException e) {
                e.printStackTrace();
            }

            /* 
             * Rotate image if needed in EXIF orientation
             */
            if (orientation != 1) {
                original = transformImage(original, getExifTransformation(orientation, original.getWidth(), original.getHeight()));
            }
            
            /*
             * Generate all formats of stored images
             */
            for (ImageFormat format : ImageFormats.get().formats) {
                BufferedImage resized = resizeImage(original, format.width, format.height, format.crop);
                this.files.add(ImageFileRelation.create(this, models.File.create(Storage.storeImage(resized)), format.width, format.height, format.name));
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
        if (!crop) {
            if (original.getWidth() > width || original.getHeight() > height) {
                if ((float)original.getWidth() / (float)width > (float)original.getHeight() / (float)height) {
                    height = (original.getHeight() * width) / original.getWidth();
                } else {
                    width = (original.getWidth() * height) / original.getHeight();
                }
            }
        } else {
            if (original.getWidth() > width && original.getHeight() > height) {
                if ((original.getWidth() * height) / width > original.getHeight()) {
                    original = original.getSubimage((original.getWidth() - (original.getHeight() * width) / height) / 2, 0, (original.getHeight() * width) / height, original.getHeight());
                } else {
                    original = original.getSubimage(0, (original.getHeight() - (original.getWidth() * height) / width) / 2, original.getWidth(), (original.getWidth() * height) / width);
                }
            } else if (original.getWidth() > width) {
                original = original.getSubimage((original.getWidth() - width) / 2, 0, width, original.getHeight());
            } else if (original.getHeight() > height) {
                original = original.getSubimage(0, (original.getHeight() - height) / 2, original.getWidth(), height);
            }
        }
        width = Math.min(width, original.getWidth());
        height = Math.min(height, original.getHeight());
        java.awt.Image scaled = original.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        BufferedImage bufferedScaled = new BufferedImage(scaled.getWidth(null),  scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
        bufferedScaled.getGraphics().drawImage(scaled, 0, 0, null);

        return bufferedScaled;
    }
    
    /**
     * Compute an affine transformation given orientation EXIF parameter
     * Look at http://chunter.tistory.com/143 for information
     * @param orientation EXIF orientation parameter
     * @param width Width of original image
     * @param height Height of original image
     * @return an affine transformation
     */
    public static AffineTransform getExifTransformation(int orientation, int width, int height) {

        AffineTransform t = new AffineTransform();

        switch (orientation) {
        case 1:
            break;
        case 2: // Flip X
            t.scale(-1.0, 1.0);
            t.translate(-width, 0);
            break;
        case 3: // PI rotation 
            t.translate(width, height);
            t.rotate(Math.PI);
            break;
        case 4: // Flip Y
            t.scale(1.0, -1.0);
            t.translate(0, -height);
            break;
        case 5: // - PI/2 and Flip X
            t.rotate(-Math.PI / 2);
            t.scale(-1.0, 1.0);
            break;
        case 6: // -PI/2 and -width
            t.translate(height, 0);
            t.rotate(Math.PI / 2);
            break;
        case 7: // PI/2 and Flip
            t.scale(-1.0, 1.0);
            t.translate(-height, 0);
            t.translate(0, width);
            t.rotate(  3 * Math.PI / 2);
            break;
        case 8: // PI / 2
            t.translate(0, width);
            t.rotate(  3 * Math.PI / 2);
            break;
        }

        return t;
    }
    
    /**
     * Apply an affine transform to an image and return the new one
     * @param image Original image
     * @param transform affine transform to apply
     * @return Transformed image
     */
    public static BufferedImage transformImage(BufferedImage image, AffineTransform transform) {

        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);

        BufferedImage destinationImage = op.createCompatibleDestImage(image,  (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
        Graphics2D g = destinationImage.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
        destinationImage = op.filter(image, destinationImage);
        
        return destinationImage;
    }
}
