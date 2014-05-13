package Utils;

import models.AccessToken;
import models.AccessTokenEventRelation;
import models.Event;
import models.Event.Privacy;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;


public class Access {
	
	public enum UserAccessType {
		READ,
		WRITE,
		ROOT
	}

	public enum AuthenticationType {
		CONNECTED_USER,
		ANONYMOUS_USER,
		NOT_CONNECTED_USER,
		ADMIN_USER,
		NO_ACCESS_TOKEN
	}
	
	public enum AccessType {
        NONE,
        READ,
        WRITE,
        ADMINISTRATE,
        ROOT
    }

	static public Result	checkAuthentication(AccessToken access, AuthenticationType authenticationType) {
		if (authenticationType == AuthenticationType.NO_ACCESS_TOKEN) {
			return null;
		} else if (authenticationType == AuthenticationType.ANONYMOUS_USER) {
			if (access == null) {
				return Controller.forbidden("Access Token Required");
			}
		} else if (authenticationType == AuthenticationType.CONNECTED_USER) {
			if (access == null) {
				return Controller.forbidden("Access Token Required");
			} else if (access.user == null) {
				return Controller.unauthorized("You need to be authenticated");
			}
		} else if (authenticationType == AuthenticationType.NOT_CONNECTED_USER) {
			if (access == null) {
				return Controller.forbidden("Access Token Required");
			} else if (access.isConnectedUser() && access.user.isAdmin == false) {
				return Controller.unauthorized("You cannot be connected");
			}
		} else if (authenticationType == AuthenticationType.ADMIN_USER) {
			if (access == null) {
				return Controller.forbidden("Access Token Required");
			} else if (access.user == null) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.isAdmin == false) {
	            return Controller.forbidden("You need to be administrator");
			}
		} else {
			return Controller.badRequest("Access Token Error");
		}
		return null;
	}
	
	static public AccessType	getPermissionOnEvent(AccessToken access, Event event) {
		if (access.isConnectedUser() && access.user.isAdmin == true) {
			return AccessType.ROOT;
		}

		AccessType res =AccessType.NONE;
		AccessTokenEventRelation relation = AccessTokenEventRelation.find.where().eq("accessToken", access).eq("event", event).findUnique();

		if (access.isConnectedUser()) {
			res = event.getPermission(access.user);
			Privacy writingPrivacy = event.writingPrivacy;
			if (writingPrivacy == Privacy.NOT_SET) {
				writingPrivacy = event.readingPrivacy;
			}
			
			if (writingPrivacy == Event.Privacy.PROTECTED) {
				if (relation != null && relation.permission == AccessType.WRITE) {
					res = max(res, AccessType.WRITE);
				}
			} else if (writingPrivacy == Event.Privacy.PUBLIC) {
				res = max(res, AccessType.WRITE);
			}
		}

		if (res.compareTo(AccessType.READ) < 0) {
			if (event.readingPrivacy == Event.Privacy.PROTECTED) {
				if (relation != null && relation.permission == AccessType.READ) {
					res = AccessType.READ;
				}
			} else if (event.readingPrivacy == Event.Privacy.PUBLIC) {
				res = AccessType.READ;
			}
		}
		return res;
	}
	
	static public Result	hasPermissionOnEvent(AccessToken access, Event event, AccessType accessType) {
		AccessType max = getPermissionOnEvent(access, event);
		
		if (max.compareTo(accessType) >= 0) {
			return null;
		} else {
			Privacy writingPrivacy = event.writingPrivacy;
			if (writingPrivacy == Privacy.NOT_SET) {
				writingPrivacy = event.readingPrivacy;
			}

			if ((accessType.compareTo(AccessType.WRITE) >= 0 && access.isConnectedUser() == false) ||
					(event.readingPrivacy == Privacy.PRIVATE && access.isConnectedUser() == false)) {
				return Controller.unauthorized("You need to be authenticated");
			} else if ((accessType == AccessType.READ && event.readingPrivacy == Privacy.PROTECTED) ||
					((accessType == AccessType.WRITE && writingPrivacy == Privacy.PROTECTED))) {
				return Controller.forbidden("You need a password for this event");
			} else {
				return Controller.forbidden("You don't have the required permission on this event");
			}
		}
	}
	
	static public Result	hasPermissionOnUser(AccessToken access, User user, UserAccessType accessType) {
		if (accessType == UserAccessType.READ) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.equals(user)) {
				return null;
			}
		} else if (accessType == UserAccessType.WRITE) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.equals(user)) {
				return null;
			}
		} else if (accessType == UserAccessType.ROOT) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.isAdmin == true) {
				return null;
			}
		} 
		return Controller.forbidden("You don't have the required permission on this user");
	}
	
	static private	AccessType	max(AccessType l, AccessType r) {
		if (l.compareTo(r) >= 0) {
			return l;
		} else {
			return r;
		}
	}
}
