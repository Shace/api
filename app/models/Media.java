package models;

import java.util.Date;

import java.util.List;
import java.net.URI;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import Utils.JSONable;


import play.db.ebean.Model;
import models.Event;

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
		this.ownerUser = ownerUser;
		this.ownerEvent = ownerEvent;
		this.creation = new Date();
		this.type = Type.NONE;
		this.uri = URI.create("");
		this.rank = 0;
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
	@Lob
	public String		description;
	
	@JSONable
	public URI			uri;

	@JSONable(defaultField=false)
	public Integer 		rank;
	
	@JSONable
	public Date			creation;
	
	@JSONable(defaultField=false)
	@ManyToOne
	@JoinColumn(name="owner_user_id")
	public User			ownerUser;
	
	@JSONable(defaultField=false)
	@ManyToOne
	@JoinColumn(name="owner_event_id")
	public Event		ownerEvent;
	
	@JSONable(defaultField=false)
	@OneToMany(mappedBy="media", cascade=CascadeType.ALL)
	public List<MediaTagRelation>	tags;

	
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
