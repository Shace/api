package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import models.AccessToken.Lang;
import play.db.ebean.Model;

@Entity
@Table(name="se_email")
public class Email extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6510342715172506918L;
	
	@GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;
	
	public Lang			lang;
	
	public int			type;
	
	public String		subject;
	
	public String		fromEmail;
	
	@Lob
	public String		html;
	
	public static Finder<Integer, Email> find = new Finder<Integer, Email>(
            Integer.class, Email.class
    );
	
}
