package models;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.*;

@Entity
@Table(name="se_access_token")
public class AccessToken extends Model {
	
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

	public Date		creation;
	
	public Date		expiration;
	
	@ManyToOne
	public User		user;
	
	public AccessToken(String token, boolean autoRenew, User user) {
		this.token = token;
		this.autoRenew = autoRenew;
		this.creation = new Date();
		this.expiration = new Date(this.creation.getTime() + ((autoRenew) ? autoRenewExpirationTime : temporaryExpirationTime));
		this.user = user;
	}
	
	public static Finder<String, AccessToken> find = new Finder<String, AccessToken>
	(		
		String.class, AccessToken.class
	);
	
	public static AccessToken create(boolean autoRenew, User user) {
		String token = UUID.randomUUID().toString();
		
		AccessToken newAccessToken = new AccessToken(token, autoRenew, user);
		newAccessToken.save();
		
		return newAccessToken;
	}

}
