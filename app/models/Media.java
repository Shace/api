package models;

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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import play.db.ebean.Model;
import Utils.JSONable;

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
		
		this.image = Image.create();
	}
	
	@JSONable
	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@JSONable
	@Enumerated(EnumType.ORDINAL)
	public Type			type;

	@JSONable
	@Column(length=255)
	public String		name;
	
	@JSONable
	public String		description;

	@JSONable(defaultField=false)
	public Integer 		rank;
	
	@JSONable
	public Date			creation;
	
	@JSONable(defaultField=false)
	@ManyToOne
	@JoinColumn(name="owner_id")
	public User			owner;
	
	@JSONable(defaultField=false)
	@ManyToOne
	@JoinColumn(name="event_id")
	public Event		event;
	
	@JSONable(defaultField=false)
	@OneToMany(mappedBy="media", cascade=CascadeType.ALL)
	public List<Tag>	tags;

	
	@JSONable(defaultField=false)
    @OneToOne(cascade=CascadeType.ALL)
    @JoinColumn(name="image_id")
	public Image       image;
	
	public Date        original;
	
    @OneToMany(mappedBy="media", cascade=CascadeType.ALL)
    @OrderBy("creation")
    public List<Comment> comments;
	
	public static Finder<Integer, Media> find = new Finder<Integer, Media>(
			Integer.class, Media.class
	);
	
	 public static Media create(String name, User ownerUser, Event ownerEvent) {
		 Media media = new Media(ownerUser, ownerEvent);
		 media.name = name;
		 media.save();
		 return media; 
	 }
}
