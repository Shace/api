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
	        	return new errors.Error(errors.Error.Type.ACCESS_TOKEN_REQUIRED).toResponse();
			}
		} else if (authenticationType == AuthenticationType.CONNECTED_USER) {
			if (access == null) {
	        	return new errors.Error(errors.Error.Type.ACCESS_TOKEN_REQUIRED).toResponse();
			} else if (access.user == null) {
	        	return new errors.Error(errors.Error.Type.NEED_AUTHENTICATION).toResponse();
			}
		} else if (authenticationType == AuthenticationType.NOT_CONNECTED_USER) {
			if (access == null) {
	        	return new errors.Error(errors.Error.Type.ACCESS_TOKEN_REQUIRED).toResponse();
			} else if (access.isConnectedUser() && access.user.isAdmin == false) {
	        	return new errors.Error(errors.Error.Type.NEED_ANONYMOUS).toResponse();
			}
		} else if (authenticationType == AuthenticationType.ADMIN_USER) {
			if (access == null) {
	        	return new errors.Error(errors.Error.Type.ACCESS_TOKEN_REQUIRED).toResponse();
			} else if (access.user == null) {
	        	return new errors.Error(errors.Error.Type.NEED_AUTHENTICATION).toResponse();
			} else if (access.user.isAdmin == false) {
	        	return new errors.Error(errors.Error.Type.NEED_ADMIN).toResponse();
			}
		} else {
        	return new errors.Error(errors.Error.Type.ACCESS_TOKEN_ERROR).toResponse();
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
				if (relation != null) {
					res = relation.permission;
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
	        	return new errors.Error(errors.Error.Type.NEED_AUTHENTICATION).toResponse();
			} else if ((accessType == AccessType.READ && event.readingPrivacy == Privacy.PROTECTED) ||
					((accessType == AccessType.WRITE && writingPrivacy == Privacy.PROTECTED))) {
	        	return new errors.Error(errors.Error.Type.NEED_PASSWORD).toResponse();
			} else {
				return new errors.Error(errors.Error.Type.EVENT_FORBIDDEN).toResponse();
			}
		}
	}
	
	static public Result	hasPermissionOnUser(AccessToken access, User user, UserAccessType accessType) {
		if (accessType == UserAccessType.READ) {
			if (access.isConnectedUser() == false) {
				return new errors.Error(errors.Error.Type.NEED_AUTHENTICATION).toResponse();
			} else if (access.user.equals(user)) {
				return null;
			}
		} else if (accessType == UserAccessType.WRITE) {
			if (access.isConnectedUser() == false) {
				return new errors.Error(errors.Error.Type.NEED_AUTHENTICATION).toResponse();
			} else if (access.user.equals(user)) {
				return null;
			}
		} else if (accessType == UserAccessType.ROOT) {
			if (access.isConnectedUser() == false) {
				return new errors.Error(errors.Error.Type.NEED_AUTHENTICATION).toResponse();
			} else if (access.user.isAdmin == true) {
				return null;
			}
		} 
		return new errors.Error(errors.Error.Type.USER_FORBIDDEN).toResponse();
	}
	
	static private	AccessType	max(AccessType l, AccessType r) {
		if (l.compareTo(r) >= 0) {
			return l;
		} else {
			return r;
		}
	}
}
