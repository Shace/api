package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class User extends Model {
	
	@GeneratedValue
	@Column(unique=true)
	public Integer	id; 
	
	@Column(length=20)
	@Constraints.Email()
	public String	mail;
	
	@Column
	@Constraints.MinLength(6)
	public String	password;
	
	@Column(length=20)
	@Constraints.MinLength(4)
	public String	firstName;
	
	@Column(length=20)
	@Constraints.MinLength(4)
	public String	lastName;
	
	@Formats.DateTime(pattern="dd/MM/yyyy")
	public Date		birthDate;
	
	@Formats.DateTime(pattern="dd/MM/yyyy")
	public Date		inscription;
	
	public static Finder<String, User> find = new Finder<String, User>
	(		
			String.class, User.class
	);
}
