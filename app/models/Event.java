package models;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_event")
public class Event extends Model {

	public enum Privacy {
		PUBLIC,
		PROTECTED,
		PRIVATE
	}
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 3754144269823907391L;
	
	@Column(length=36, unique=true)
	public String		id;
	
	@Id
	@Column(length=255, unique=true)
	public String 		token;

	@Column(length=40)
	public String		password;
	
	@Column(length=255)
	public String		name;
	
	public String		description;
	
	@Enumerated(EnumType.ORDINAL)
	public Privacy 		readingPrivacy;
	
	@Enumerated(EnumType.ORDINAL)
	public Privacy 		writingPrivacy;
	
	public Date			creation;
	
	@OneToMany(mappedBy="event", cascade=CascadeType.ALL)
	public List<Media>	medias;
	
	@ManyToOne
    @JoinColumn(name="owner_id")
    public User         owner;

	public Event(Privacy privacy, User ownerUser) {
		this.id = UUID.randomUUID().toString();
		this.token = this.id;
		this.creation = new Date();
		this.readingPrivacy = privacy;
		this.writingPrivacy = privacy;
		this.owner = ownerUser;
	}
	
	public static Finder<String, Event> find = new Finder<String, Event>(
			String.class, Event.class
	);
	
	public static Event create(Privacy privacy, User ownerUser) {
		Event newEvent = new Event(privacy, ownerUser);
		newEvent.save();

		return newEvent;
	}
}
