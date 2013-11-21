package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import controllers.Utils;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
@Table(name="se_user")
public class User extends Model {
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = -378338424543301076L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer	id; 
	
	@Column(length=254, unique=true)
	@Constraints.Email()
	public String	email;
	
	@Column(length=40)
	public String	password;
	
	@Column(length=35)
	@Constraints.MinLength(2)
	public String	firstName;
	
	@Column(length=35)
	@Constraints.MinLength(2)
	public String	lastName;
	
	@Formats.DateTime(pattern="dd/MM/yyyy")
	public Date		birthDate;
	
	@Formats.DateTime(pattern="dd/MM/yyyy")
	public Date		inscriptionDate;
	
	@OneToMany(mappedBy="ownerUser", cascade=CascadeType.ALL)
	public List<Media>	medias;
	
	@OneToMany(mappedBy="creator", cascade=CascadeType.ALL)
	public List<MediaTagRelation>	tags;
	
	public User(String email, String password) {
		this.email = email;
		this.password = Utils.hash(password);
		this.inscriptionDate = new Date();
	}
	
	public static Finder<Integer, User> find = new Finder<Integer, User>
	(		
			Integer.class, User.class
	);
	
	public static User create(String email, String password) {
		User newUser = new User(email, password);
		newUser.save();
		
		return newUser;
	}
	
}
