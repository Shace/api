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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.ebean.Model;
import Utils.Access;

@Entity
@Table(name="se_event")
public class Event extends Model {

	public enum Privacy {
		PUBLIC,
		PROTECTED,
		PRIVATE,
		NOT_SET
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
	
	public Date			startDate;
	
	public Date			finishDate;
	
	@OneToMany(mappedBy="event", cascade=CascadeType.ALL)
	public List<Media>	medias;
	
    @OneToOne(cascade=CascadeType.ALL)
	public Bucket       root;
    
    private User         owner;
    
    @OneToOne(cascade=CascadeType.ALL)
    @JoinColumn(name="cover_image_id")
	public Image       coverImage;

	public Event(Privacy readingPrivacy, User ownerUser) {
		this.id = UUID.randomUUID().toString();
		this.token = this.id;
		this.creation = new Date();
		this.startDate= null;
		this.finishDate= null;
		this.creation = new Date();
		this.readingPrivacy = readingPrivacy;
		this.writingPrivacy = Privacy.NOT_SET;
		this.owner = ownerUser;
		this.root = new Bucket(0, null);
		this.root.save();
		this.coverImage = Image.create();
	}
	
	public void	saveOwnerPermission() {
		if (this.owner != null) {
			setPermission(this.owner, Access.AccessType.ROOT).save();
		}
	}
	
	public boolean	hasPermission(User user, Access.AccessType permission) {
		if (user != null) {
			for (EventUserRelation relation : permissions) {
				if (user.email.equals(relation.email) &&
						this.equals(relation.event) &&
						permission.compareTo(relation.permission) <= 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	public EventUserRelation	setPermission(User user, Access.AccessType permission) {
		for (EventUserRelation relation : permissions) {
			if (user.email.equals(relation.email) && relation.permission == permission) {
				return relation;
			}
		}
		
		EventUserRelation res = new EventUserRelation(this, user.email, permission);
		permissions.add(res);
		return res;
	}
	
	public Access.AccessType	getPermission(User user) {
		Access.AccessType res = Access.AccessType.NONE;
		for (EventUserRelation relation : permissions) {
			if (relation.email.equals(user.email) && relation.permission.compareTo(res) > 0) {
				if (relation.permission.compareTo(Access.AccessType.ADMINISTRATE) >= 0) {
					res = relation.permission;
				} else if (relation.permission.compareTo(Access.AccessType.READ) <= 0 && readingPrivacy == Privacy.PRIVATE) {
					res = relation.permission;
				} else if (relation.permission.compareTo(Access.AccessType.WRITE) <= 0 && 
							(writingPrivacy == Privacy.PRIVATE || (writingPrivacy == Privacy.NOT_SET && readingPrivacy == Privacy.PRIVATE))) {
					res = relation.permission;
				}
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
