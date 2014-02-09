package models;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_image_file_relation")
public class ImageFileRelation extends Model {
	
	/**
     * 
     */
    private static final long serialVersionUID = -5349729526133819605L;

    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;

    @ManyToOne
	@JoinColumn(name="image_id")
	public Image		image;

    @OneToOne(cascade=CascadeType.ALL)
	public File			file;
	
	public Integer      width;
	
	public Integer      height;
	
	public String       format;
	
	public ImageFileRelation(Image image, File file, Integer width, Integer height, String format) {
		this.image = image;
		this.file = file;
		this.width = width;
		this.height = height;
		this.format = format;
	}
	
	public static ImageFileRelation create(Image image, File file, Integer width, Integer height, String format) {
	    ImageFileRelation imageFileRelation = new ImageFileRelation(image, file, width, height, format);
	    imageFileRelation.save();
	    return imageFileRelation;
	}
	
}
