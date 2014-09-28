package models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import play.db.ebean.Model;
import Utils.JSONable;
import Utils.Slugs;

@Entity
@Table(name="se_tag")
public class Tag extends Model {
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 7317993794965281617L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@Column(length=255)
	public String		name;
	
	@Column(length=255)
	public String		slug;
	
	public Tag(String name, User creator, Media media) {
		this.name = name;
		this.creator = creator;
		this.media = media;
		this.slug = Slugs.toSlug(name);
		this.creation = new Date();
	}
	
	@ManyToOne
    @JoinColumn(name="media_id")
    public Media        media;

    @ManyToOne
    @JoinColumn(name="user_id")
    public User         creator;
    
    public Date         creation;
	
    @Version
    Timestamp updateTime;
    
	public static Finder<Integer, Tag> find = new Finder<Integer, Tag>(
			Integer.class, Tag.class
	);
	
	 public static Tag create(String name, User creator, Media media) {
		 Tag tag = new Tag(name, creator, media);
		 tag.save();
		 return tag; 
	 }
}
