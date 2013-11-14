package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import play.db.ebean.Model;

@Entity
public class Media extends Model {
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 8168675220916133843L;

	public enum Type {
		IMAGE,
		VIDEO
	}

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@Enumerated(EnumType.ORDINAL)
	public Type			type;

	@Column(length=255)
	public String		name;
	
	@Lob
	public String		description;
	
	public URI			uri;

	public Integer 		rank;
	
	public Date			creation;
	
	@ManyToOne
	public User			ownerUser;
	
	@ManyToOne
	public Event		ownerEvent;

	public Media(String name, User ownerUser, Event ownerEvent) {
		this.name = name;
		this.ownerUser = ownerUser;
		this.ownerEvent = ownerEvent;
		this.creation = new Date();
	}

	
	public static Finder<Integer, Media> find = new Finder<Integer, Media>(
			Integer.class, Media.class
	);
	
	 public static Media create(String name, User ownerUser, Event ownerEvent) {
		 Media media = new Media(name, ownerUser, ownerEvent);
		 media.save();
		 return media; 
	 }
}
