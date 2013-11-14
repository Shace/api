package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import play.db.ebean.Model;

@Entity
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

	@GeneratedValue
	@Column(unique=true)
	public Integer 		id;
	
	@Id
	@Column(length=255, unique=true)
	public String 		token;

	@Column(length=40)
	public String		password;
	
	@Column(length=255)
	public String		name;
	
	@Lob
	public String		description;
	
	@Enumerated(EnumType.ORDINAL)
	public Privacy 		privacy;
	
	public Date			creation;

	public static Finder<String, Event> find = new Finder<String, Event>(
			String.class, Event.class
	);
}
