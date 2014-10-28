package models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import play.db.ebean.Model;

@Entity
@Table(name="se_report")
public class Report extends Model {

	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = -5720223418902866639L;

	public enum Type {
		Spam,
		Copycat,
		Explicit,
		Personal
	}
	
	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@Enumerated(EnumType.ORDINAL)
	public Type			type;

	@Column(columnDefinition = "TEXT")
	public String		reason;
	
    @ManyToOne
    @JoinColumn(name="user_id")
    public User         creator;
    
    public Date         creation;
    
    @OneToOne
    @JoinColumn(name="image_id")
    public Image		image;
    
    @Version
    Timestamp updateTime;
    
	public Report(User creator, Type type, String reason) {
		this.creator = creator;
		this.creation = new Date();
		this.type = type;
		this.reason = reason;
	}
	
	public static Finder<Integer, Report> find = new Finder<Integer, Report>(
			Integer.class, Report.class
	);
	
	 public static Report create(User creator, Type type, String reason) {
		 Report report = new Report(creator, type, reason);
		 report.save();
		 return report; 
	 }
}
