package models;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_tag")
public class Tag extends Model {
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 7317993794965281617L;

	@GeneratedValue
	@Column(unique=true)
	@Id
	public Integer 		id;
	
	@Column(length=255)
	public String		name;
	
	@Column(length=255)
	public String		slug;

	@OneToMany(mappedBy="tag", cascade=CascadeType.ALL)
	public List<MediaTagRelation>	medias;
	
	public Tag(String name, String slug) {
		this.name = name;
		this.slug = slug;
	}

	
	public static Finder<Integer, Tag> find = new Finder<Integer, Tag>(
			Integer.class, Tag.class
	);
	
	 public static Tag create(String name, String slug) {
		 Tag tag = new Tag(name, slug);
		 tag.save();
		 return tag; 
	 }
}
