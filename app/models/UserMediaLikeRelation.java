package models;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
@Table(name="se_user_media_like_relation")
public class UserMediaLikeRelation extends Model {

	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = -8031350561040341298L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	public UserMediaLikeRelation(User creator, Media media) {
		this.creator = creator;
		this.media = media;
		this.creation = new Date();
	}
	
	@ManyToOne
    @JoinColumn(name="media_id")
    public Media        media;

    @ManyToOne
    @JoinColumn(name="user_id")
    public User         creator;
    
    public Date         creation;
    
    @Version
    Timestamp updateTime;
    
	public static Finder<Integer, UserMediaLikeRelation> find = new Finder<Integer, UserMediaLikeRelation>(
			Integer.class, UserMediaLikeRelation.class
	);
	
	 public static UserMediaLikeRelation create(User creator, Media media) {
		 UserMediaLikeRelation like = new UserMediaLikeRelation(creator, media);
		 like.save();
		 return like; 
	 }
}
