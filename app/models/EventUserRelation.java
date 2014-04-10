package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_event_user_relation")
public class EventUserRelation extends Model {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7487557111657046789L;

	@ManyToOne
	@JoinColumn(name="event_token")
	public Event	event;
	
	@ManyToOne
	@JoinColumn(name="user_id")
	public User		user;
	
	@Enumerated(EnumType.ORDINAL)
	public Event.AccessType	permission;
	
	public EventUserRelation(Event event, User user, Event.AccessType permission) {
		this.event = event;
		this.user = user;
		this.permission = permission;
	}
}
