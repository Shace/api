package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_image_format")
public class ImageFormat extends Model {

    /**
     * 
     */
    private static final long serialVersionUID = 4491469185519640828L;

    @Column(unique=true)
    @Id
    public String name;
    
    public int width;
    public int height;
    public boolean crop;
    
    public static Finder<String, ImageFormat> find = new Finder<String, ImageFormat>(
            String.class, ImageFormat.class
    );
}
