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
@Table(name="se_access_token_event_relation")
public class AccessTokenEventRelation extends Model {

	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 4565841567189890217L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer	id; 
	
	@ManyToOne
	@JoinColumn(name="event_id")
	public Event	event;
	
	@ManyToOne
	@JoinColumn(name="accessToken_id")
	public AccessToken		accessToken;
	
	@Enumerated(EnumType.ORDINAL)
	public Access.AccessType	permission;

	public AccessTokenEventRelation(Event event, AccessToken accessToken, Access.AccessType permission) {
		this.event = event;
		this.accessToken = accessToken;
		this.permission = permission;
	}
	
	public static Finder<Integer, AccessTokenEventRelation> find = new Finder<Integer, AccessTokenEventRelation>(
			Integer.class, AccessTokenEventRelation.class
	);
}
