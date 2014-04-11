package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import play.db.ebean.Model;
import Utils.JSONable;

@Entity
@Table(name="se_bucket")
public class Bucket extends Model implements Comparable<Bucket> {

    /**
     * Unique version uid for serialization
     */
    private static final long serialVersionUID = 1969483314601386091L;

    @JSONable
    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;
    
    @ManyToMany(cascade=CascadeType.ALL)
    public List<Media>  medias;
    
    @Column(length=255)
    public String       name;

    @ManyToOne
    @JoinColumn(name="parent_id")
    public Bucket       parent;

    @OneToMany(mappedBy="parent", cascade=CascadeType.ALL)
    @OrderBy("first")
    public List<Bucket> children;
    
    public Integer      level;

    public Date         first;

    public Date         last;
    
    public Integer      size;

    @ManyToOne
    @JoinColumn(name="event_id")
    public Event        event;

    public Bucket(int level, Event event) {
        this.name = "";
        this.parent = null;
        this.level = level;
        this.first = this.last = new Date();
        this.size = 0;
        this.event = event;
    }
    
    public static Finder<Integer, Bucket> find = new Finder<Integer, Bucket>(
            Integer.class, Bucket.class
    );

    @Override
    public int compareTo(Bucket o) {
        if (this.first.getTime() < o.first.getTime()) {
            return -1;
        }
        return 1;
    }
}
