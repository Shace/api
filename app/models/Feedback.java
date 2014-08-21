package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

@Entity
@Table(name="se_feedback")
public class Feedback extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5685061530833531545L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	public String		description;
	
	public User			senderUser;
	
	@Column(length=254)
	@Constraints.Email()
	public String		senderEmail;
	
	public boolean		okForAnswer;

	@Formats.DateTime(pattern="dd/MM/yyyy")
	public Date			creationDate;
	
	public boolean		adminRead;
	
	public Feedback(String email, User sender, String description, boolean okForAnswer) {
		this.description = description;
		this.senderUser = sender;
		this.senderEmail = email;
		this.okForAnswer = okForAnswer;
		this.creationDate = new Date();
		this.adminRead = false;
	}

	public static Finder<Integer, Feedback> find = new Finder<Integer, Feedback>
	(		
			Integer.class, Feedback.class
	);

}
