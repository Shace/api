package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;
import Utils.Access;

@Entity
@Table(name="se_event_user_relation")
public class EventUserRelation extends Model {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7487557111657046789L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer	id; 
	
	@ManyToOne
	@JoinColumn(name="event_token")
	public Event	event;
	
	@ManyToOne
	@JoinColumn(name="user_id")
	public User		user;
	
	@Enumerated(EnumType.ORDINAL)
	public Access.AccessType	permission;
	
	public EventUserRelation(Event event, User user, Access.AccessType permission) {
		this.event = event;
		this.user = user;
		this.permission = permission;
	}
	
	public static Finder<Event, EventUserRelation> find = new Finder<Event, EventUserRelation>(
			Event.class, EventUserRelation.class
	);
	
}
