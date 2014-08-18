package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
@Table(name="se_beta_invitation")
public class BetaInvitation extends Model {

	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 4792826268773294046L;

	public enum State {
		INVITED,
		REQUESTING,
		CREATED
	}
	
	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@Column
	@OneToOne(cascade=CascadeType.ALL)
	public User			originalUser;
	
	@Column
	@OneToOne(cascade=CascadeType.ALL)
	public User			createdUser;
	
	@Column(length=255)
	public String		email;
	
	@Column(length=40)
	public String	password;
	
	@Column(length=35)
	@Constraints.MinLength(2)
	public String	firstName;
	
	@Column(length=35)
	@Constraints.MinLength(2)
	public String	lastName;
	
	@Formats.DateTime(pattern="dd/MM/yyyy")
	public Date		invitationDate;

	@Column
	public Integer 		invitedPeople;
	
	@Column
	public State		state;
	
	public BetaInvitation(User user, String mail, String password, String firstname, String lastname, State state) {
		this.originalUser = user;
		this.createdUser = null;
		this.email = mail;
		this.password = Utils.Hasher.hash(password);
		this.firstName = firstname;
		this.lastName = lastname;
		this.invitationDate = new Date();
		this.invitedPeople = 0;
		this.state = state;
	}
		
	public static Finder<Integer, BetaInvitation> find = new Finder<Integer, BetaInvitation>(
			Integer.class, BetaInvitation.class
	);
}
