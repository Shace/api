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

import play.db.ebean.Model;
import Utils.ImageFormats;
import Utils.Storage;

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

    public void addFile(File file) throws BadFormat {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null)
                throw new BadFormat();
            for (ImageFormat format : ImageFormats.get().formats) {
                BufferedImage resized = resizeImage(original, format.width, format.height, format.crop);
                this.files.add(ImageFileRelation.create(this, models.File.create(Storage.storeImage(resized)), format.width, format.height, format.name));
            }
            this.save();
        } catch (IOException e) {
            throw new BadFormat(e.getMessage());
        }
    }

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
}
