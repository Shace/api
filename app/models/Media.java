package models;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Version;

import play.db.ebean.Model;

@Entity
@Table(name="se_media")
public class Media extends Model {
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 8168675220916133843L;

	public enum Type {
		IMAGE,
		VIDEO,
		NONE
	}

	public Media(User ownerUser, Event ownerEvent) {
		this.name = "";
		this.description = "";
		this.owner = ownerUser;
		this.event = ownerEvent;
		this.creation = new Date();
		this.type = Type.IMAGE;
		this.rank = 0;
		this.valid = true;
		
		this.image = Image.create(ownerUser);
	}
	
	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@Enumerated(EnumType.ORDINAL)
	public Type			type;

	@Column(length=255)
	public String		name;
	
	public String		description;

	public Integer 		rank;
	
	public Date			creation;
	
	@ManyToOne
	@JoinColumn(name="owner_id")
	public User			owner;
	
	@ManyToOne
	@JoinColumn(name="event_id")
	public Event		event;
	
	@OneToMany(mappedBy="media", cascade=CascadeType.ALL)
	public List<Tag>	tags;

	
    @OneToOne(cascade=CascadeType.ALL)
    @JoinColumn(name="image_id")
	public Image       image;
	
	public Date        original;
	
	public boolean		valid;
	
    @OneToMany(mappedBy="media", cascade=CascadeType.ALL)
    @OrderBy("creation")
    public List<Comment> comments;
    
	@ManyToMany(mappedBy = "medias")
	public List<Bucket>		buckets;
	
	@Version
    Timestamp updateTime;
	
	public static Finder<Integer, Media> find = new Finder<Integer, Media>(
			Integer.class, Media.class
	);
	
	 public static Media create(String name, User ownerUser, Event ownerEvent) {
		 Media media = new Media(ownerUser, ownerEvent);
		 media.name = name;
		 media.valid = true;
		 media.save();
		 return media; 
	 }
}
