package models;

import java.util.Date;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="se_access_token")
public class AccessToken extends Model {
	
	public enum Type {
		GUEST,
		USER
	}
	
	public enum Lang {
		NONE,
		FR,
		EN
	}
	
	/**
	 * Unique version uid for serialization
	 */
	private static final long serialVersionUID = 6753521864589882221L;
	
	/**
	 * Expiration time of an autoRenew token in milliseconds
	 */
	public static final long autoRenewExpirationTime = 7 * 24 * 60 * 60 * 1000;
	
	/**
	 * Expiration time of a temporary token in milliseconds
	 */
	public static final long temporaryExpirationTime = 24 * 60 * 60 * 1000;

	@Column(length=40, unique=true)
	@Id
	public String	token;

	public boolean 	autoRenew;

	//public Date		creation;
	
	//public Date		expiration;
	
	public long creation;
	public long expiration;
	
	@ManyToOne(cascade=CascadeType.ALL)
	public User		user;
	
	public Type		type;
	public Lang		lang;

	
	public AccessToken(String token, boolean autoRenew, User user, Type type) {
		this.token = token;
		this.autoRenew = autoRenew;
		this.creation = new Date().getTime();
		this.expiration = this.creation + ((autoRenew) ? autoRenewExpirationTime : temporaryExpirationTime);
		this.user = user;
		this.type = type;
		if (this.user != null) {
			this.lang = this.user.lang;
		} else {
			this.lang = Lang.NONE;
		}
	}
	
	public static Finder<String, AccessToken> find = new Finder<String, AccessToken>
	(		
		String.class, AccessToken.class
	);
	
	public static AccessToken create(boolean autoRenew, User user, Type type) {
		String token = UUID.randomUUID().toString();
		
		AccessToken newAccessToken = new AccessToken(token, autoRenew, user, type);
		newAccessToken.save();
		
		return newAccessToken;
	}

	public boolean isGuest() {
		return this.type == Type.GUEST;
	}
	
	public boolean isConnectedUser() {
		return this.type == Type.USER;
	}
	
	public Lang getLang() {
    	if (user != null && user.lang != null) {
        	return user.lang;
        } else {
        	return lang;
        }
    }
}
