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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_event")
public class Event extends Model {

	public enum Privacy {
		PUBLIC,
		PROTECTED,
		PRIVATE,
		NOT_SET
	}
	
	public enum AccessType {
		NONE,
		READ,
		WRITE,
		ADMINISTRATE,
		ROOT
	}
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 3754144269823907391L;
	
	@Id
	@Column(length=36, unique=true)
	public String		id;

	@Column(length=255, unique=true)
	public String 		token;
	
	@Column(length=255)
	public String		name;
	
	public String		description;
	
	@Enumerated(EnumType.ORDINAL)
	public Privacy 		readingPrivacy;
	
	@Enumerated(EnumType.ORDINAL)
	public Privacy 		writingPrivacy;

	@Column(length=40)
	public String		readingPassword;

	@Column(length=40)
	public String		writingPassword;

	@OneToMany(mappedBy="event")
	public List<EventUserRelation> permissions;
		
	public Date			creation;
	
	@OneToMany(mappedBy="event", cascade=CascadeType.ALL)
	public List<Media>	medias;
	
    @OneToOne(cascade=CascadeType.ALL)
	public Bucket       root;
    
    private User         owner;

	public Event(Privacy readingPrivacy, User ownerUser) {
		this.id = UUID.randomUUID().toString();
		this.token = this.id;
		this.creation = new Date();
		this.readingPrivacy = readingPrivacy;
		this.writingPrivacy = Privacy.NOT_SET;
		this.owner = ownerUser;
		this.root = new Bucket(0, null);
		this.root.save();
	}
	
	public void	saveOwnerPermission() {
		if (this.owner != null) {
			setPermission(this.owner, AccessType.ROOT).save();
		}
	}
	
	public boolean	hasPermission(User user, AccessType permission) {
		if (user != null) {
			for (EventUserRelation relation : permissions) {
				if (user.equals(relation.user) &&
						this.equals(relation.event) &&
						permission.compareTo(relation.permission) <= 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	public EventUserRelation	setPermission(User user, AccessType permission) {
		for (EventUserRelation relation : permissions) {
			if (user.equals(relation.user) && relation.permission == permission) {
				return relation;
			}
		}
		
		EventUserRelation res = new EventUserRelation(this, user, permission);
		permissions.add(res);
		return res;
	}
	
	public AccessType	getPermission(User user) {
		AccessType res = AccessType.NONE;
		for (EventUserRelation relation : permissions) {
			if (relation.user.equals(user) && relation.permission.compareTo(res) > 0 &&
					!(relation.permission.compareTo(Event.AccessType.READ) <= 0 && readingPrivacy != Privacy.PRIVATE) &&
					!(relation.permission.compareTo(Event.AccessType.WRITE) <= 0 && writingPrivacy != Privacy.PRIVATE)) {
				res = relation.permission;
			}
		}
		return res;
	}
	
	public static Finder<String, Event> find = new Finder<String, Event>(
			String.class, Event.class
	);
	
	public static Event create(Privacy readingPrivacy, User ownerUser) {
		Event newEvent = new Event(readingPrivacy, ownerUser);
		newEvent.save();
		
		newEvent.root.event = newEvent;
		newEvent.root.save();
		return newEvent;
	}
}
