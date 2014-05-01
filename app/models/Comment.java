package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_comment")
public class Comment extends Model {

    /**
     * Unique version uid for serialization
     */
    private static final long serialVersionUID = -6011507996193307620L;

    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;
    
    public Date         creation;
    
    public String       message;
    
    @ManyToOne
    @JoinColumn(name="owner_id")
    public User         owner;
    
    @ManyToOne
    @JoinColumn(name="media_id")
    public Media        media;
    
    public Comment(User owner, Media media, String message) {
        this.creation = new Date();
        this.owner = owner;
        this.media = media;
        this.message = message;
    }
    
    public static Finder<Integer, Comment> find = new Finder<Integer, Comment>(
            Integer.class, Comment.class
    );
    
    public static Comment create(User owner, Media media, String message) {
        Comment comment = new Comment(owner, media, message);
        comment.save();

        return comment;
    }
}
