package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_media_tag_relation")
public class MediaTagRelation extends Model {
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 2432905791500607570L;

	@ManyToOne
	@JoinColumn(name="media_id")
	public Media		media;

	@ManyToOne
	@JoinColumn(name="tag_id")
	public Tag			tag;
	
	@ManyToOne
	@JoinColumn(name="user_id")
	public User			creator;
	
	public MediaTagRelation(Media media, Tag tag, User user) {
		this.media = media;
		this.tag = tag;
		this.creator = user;
	}
	
	public static MediaTagRelation create(Media media, Tag tag, User user) {
		 MediaTagRelation mediaTagRelation = new MediaTagRelation(media, tag, user);
		 mediaTagRelation.save();
		 return mediaTagRelation;
	 }
}
