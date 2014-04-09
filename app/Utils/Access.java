package Utils;

import play.mvc.Result;
import models.AccessToken;
import models.Event;
import models.Event.Privacy;
import models.User;
import play.mvc.Controller;


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
	
	static public Event.AccessType	getPermissionOnEvent(AccessToken access, Event event) {
		if (access.isConnectedUser() && access.user.isAdmin == true) {
			return Event.AccessType.ROOT;
		}
		Event.AccessType res = Event.AccessType.NONE;

		if (access.isConnectedUser()) {
			res = event.getPermission(access.user);
		}
		if (event.writingPrivacy == Event.Privacy.PUBLIC) {
			return max(Event.AccessType.WRITE, res);
		} else if (event.writingPrivacy == Event.Privacy.PROTECTED) {
			// TODO : Handle to check through the cookies or something else
		} else if (event.readingPrivacy == Event.Privacy.PUBLIC) {
			return max(Event.AccessType.READ, res);
		} else if (event.readingPrivacy == Event.Privacy.PROTECTED) {
			// TODO : Handle to check through the cookies or something else
		}
		return res;
	}
	
	static public Result	hasPermissionOnEvent(AccessToken access, Event event, Event.AccessType accessType) {
		if (access.isConnectedUser() && access.user.isAdmin == true) {
			return null;
		} else if (accessType == Event.AccessType.READ) {
			if (event.readingPrivacy == Event.Privacy.PUBLIC) {
				return null;
			} else if (event.readingPrivacy == Privacy.PROTECTED) {
				// TODO : Handle to check through the cookies or something else
			} else if (event.readingPrivacy == Privacy.PRIVATE) {
				if (access.isConnectedUser() == false) {
					return Controller.unauthorized("You need to be authenticated");
				} else if (event.hasPermission(access.user, accessType)) {
					return null;
				}
			}
		} else if (accessType == Event.AccessType.WRITE) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (event.writingPrivacy == Event.Privacy.PUBLIC) {
				return null;
			} else if (event.writingPrivacy == Privacy.PROTECTED) {
				// TODO : Handle to check through the cookies or something else
			} else if (event.writingPrivacy == Privacy.PRIVATE) {
				if (event.hasPermission(access.user, accessType)) {
					return null;
				}
			}
		} else if (accessType == Event.AccessType.ADMINISTRATE) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (event.hasPermission(access.user, accessType)) {
				return null;
			}
		} else if (accessType == Event.AccessType.ROOT) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (event.hasPermission(access.user, accessType)) {
				return null;
			}
		}
		return Controller.forbidden("You don't have the required permission on this event");
	}
	
	static public Result	hasPermissionOnUser(AccessToken access, User user, UserAccessType accessType) {
		if (accessType == UserAccessType.READ) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.id == user.id) {
				return null;
			}
		} else if (accessType == UserAccessType.WRITE) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.id == user.id) {
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
	
	static private	Event.AccessType	max(Event.AccessType l, Event.AccessType r) {
		if (l.compareTo(r) >= 0) {
			return l;
		} else {
			return r;
		}
	}
}
