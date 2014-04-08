package Utils;

import play.mvc.Result;
import models.AccessToken;
import models.Event;
import models.Event.Privacy;
import play.mvc.Controller;


public class Access {
	public enum EventAccessType {
		READ,
		WRITE,
		ADMINISTRATE,
		ROOT
	}

	public enum AuthenticationType {
		CONNECTED_USER,
		ANONYMOUS_USER,
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
		} else {
			return Controller.badRequest("Access Token Error");
		}
		return null;
	}
	
	static public Result	hasEventAccess(AccessToken access, Event event, EventAccessType accessType) {
		if (access.user != null && access.user.isAdmin == true) {
			return null;
		}
		if (accessType == EventAccessType.READ) {
			if (event.readingPrivacy == Event.Privacy.PUBLIC) {
				return null;
			} else if (event.readingPrivacy == Privacy.PROTECTED) {
				// TODO : Handle to check through the cookies or something else
			} else if (event.readingPrivacy == Privacy.PRIVATE) {
				if (access.isConnectedUser() == false) {
					return Controller.unauthorized("You need to be authenticated");
				} else if (access.user.id == event.owner.id) {
					return null;
				}
				// TODO : Check if the user is in the list of authorized readers
			}
		} else if (accessType == EventAccessType.WRITE) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (event.writingPrivacy == Event.Privacy.PUBLIC) {
				return null;
			} else if (event.writingPrivacy == Privacy.PROTECTED) {
				// TODO : Handle to check through the cookies or something else
			} else if (event.writingPrivacy == Privacy.PRIVATE) {
				if (access.user.id == event.owner.id) {
					return null;
				}
				// TODO : Check if the user is in the list of authorized writers
			}
		} else if (accessType == EventAccessType.ADMINISTRATE) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.id == event.owner.id) {
				return null;
			}
			// TODO : Check if the user is in the list of authorized administrators
		} else if (accessType == EventAccessType.ROOT) {
			if (access.isConnectedUser() == false) {
				return Controller.unauthorized("You need to be authenticated");
			} else if (access.user.id == event.owner.id) {
				return null;
			}
		}
		return Controller.forbidden("Permission Denied");
	}
}
