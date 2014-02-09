package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_file")
public class File extends Model {

    /**
     * 
     */
    private static final long serialVersionUID = 6727022222330879650L;

    public File() {
        this.creation = new Date();
    }
    
    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;
    
    public Date         creation;
    
    public String       uid;

    public static File create(String uid) {
        models.File file = new models.File();
        file.uid = uid;
        file.save();
        return file;
    }
}
