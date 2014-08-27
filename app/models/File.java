package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.ebean.Model;
import plugins.S3Plugin;

@Entity
@Table(name="se_file")
public class File extends Model {

	public enum Type {
		Local,
		Amazon
	}
	
    /**
     * 
     */
    private static final long serialVersionUID = 6727022222330879650L;

    public File() {
        this.creation = new Date();
        this.type = Type.Local;
    }
    
    @GeneratedValue
    @Column(unique=true)
    @Id
    public Integer      id;
    
    public Date         creation;
    
    public String       uid;
    
    public String       baseURL;
    
    public Type			type;

    public static File create(String uid, String baseURL) {
        models.File file = new models.File();
        file.uid = uid;
        file.baseURL = baseURL;
        if (S3Plugin.amazonS3 == null) {
        	file.type = Type.Local;
        } else {
        	file.type = Type.Amazon;
        }
        file.save();
        return file;
    }
}
