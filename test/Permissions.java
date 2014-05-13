import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.status;

import java.util.HashMap;
import java.util.Map;

import models.AccessToken;
import models.AccessToken.Type;
import models.AccessTokenEventRelation;
import models.Event;
import models.Event.Privacy;
import models.EventUserRelation;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import play.test.WithApplication;
import Utils.Access;
import controllers.AccessTokens;


public class Permissions extends WithApplication {
	
	private class UserInfo {
		public Map<Access.AccessType, Integer>	access;
		public Access.AccessType					maxPermission;
		
		public UserInfo(Map<Access.AccessType, Integer> access, Access.AccessType maxPermission) {
			this.access = access;
			this.maxPermission = maxPermission;
		}
	}
	

	private Event       event;
    private User        ownerUser;
    private User        userWrite;
    private User        userRead;
    private User        otherUser;
    private User        adminUser;
    private AccessToken ownerUserToken;
    private AccessToken userReadToken;
    private AccessToken userWriteToken;
    private AccessToken otherUserToken;
    private AccessToken anonymousUserToken; 
    private AccessToken adminUserToken;
    private Map<AccessToken, UserInfo> users;

    @Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));

        ownerUser = User.create("toto1@gmail.com", "secret");
        ownerUserToken = AccessTokens.authenticate(ownerUser.email, "secret", true);

        adminUser = User.create("toto2@gmail.com", "secret");
        adminUserToken = AccessTokens.authenticate(adminUser.email, "secret", true);

        userWrite = User.create("toto4@gmail.com", "secret");
        userWriteToken = AccessTokens.authenticate(userWrite.email, "secret", true);

        userRead = User.create("toto3@gmail.com", "secret");
        userReadToken = AccessTokens.authenticate(userRead.email, "secret", true);

        otherUser = User.create("toto5@gmail.com", "secret");
        otherUserToken = AccessTokens.authenticate(otherUser.email, "secret", true);

        anonymousUserToken = AccessToken.create(true, null, Type.GUEST);

        event = new Event(Privacy.PUBLIC, ownerUser);
        event.token = "event";
        event.save();
        event.saveOwnerPermission();

        EventUserRelation relation = new EventUserRelation(event, adminUser, Access.AccessType.ADMINISTRATE);
        relation.save();
        
        event = Event.find.byId(event.id);
    
    }
    
    /*
     * Test the permission on an event with public reading privacy and writing not set
     */
    @Test
    public void testPublicEventNotSet() {
    	// Modify Event
    	event.writingPrivacy = Event.Privacy.NOT_SET;
    	event.readingPrivacy = Event.Privacy.PUBLIC;
    	
    	// Initialize actions
    	users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
    	
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, null, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.WRITE));

    	Integer[] otherReturnValues = {null, null, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.WRITE));

    	Integer[] anonymousReturnValues = {null, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.READ));

    	testActions();
    }
    
    /*
     * Test the permission on an event with public reading and writing privacy
     */
    @Test
    public void testPublicEventSet() {
    	// Modify Event
    	event.writingPrivacy = Event.Privacy.PUBLIC;
    	event.readingPrivacy = Event.Privacy.PUBLIC;
    	
    	// Initialize actions
    	users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
    	
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, null, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.WRITE));

    	Integer[] otherReturnValues = {null, null, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.WRITE));

    	Integer[] anonymousReturnValues = {null, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.READ));

    	testActions();
    }

    /*
     * Test the permission on an event with public reading and protected writing privacy
     */
    @Test
    public void testPublicProtectedEvent() {
    	// Modify Event
    	AccessTokenEventRelation readRelation = new AccessTokenEventRelation(event, userReadToken, Access.AccessType.READ);
    	readRelation.save();

    	AccessTokenEventRelation writeRelation = new AccessTokenEventRelation(event, userWriteToken, Access.AccessType.WRITE);
    	writeRelation.save();
    	
    	event.writingPrivacy = Event.Privacy.PROTECTED;
        event.save();
        event = Event.find.byId(event.id);
    	
    	// Initialize actions
    	users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
    	
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {null, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.READ));

    	Integer[] anonymousReturnValues = {null, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.READ));

    	testActions();
    }

    /*
     * Test the permission on an event with public reading and private writing privacy
     */
    @Test
    public void testPublicPrivateEvent() {
    	// Modify Event
        EventUserRelation readRelation = new EventUserRelation(event, userRead, Access.AccessType.READ);
        readRelation.save();
        EventUserRelation writeRelation = new EventUserRelation(event, userWrite, Access.AccessType.WRITE);
        writeRelation.save();
        
        event.writingPrivacy = Event.Privacy.PRIVATE;
        event.save();
        event = Event.find.byId(event.id);

    	// Initialize actions
        users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};
    	
    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
    	
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {null, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.READ));

    	Integer[] anonymousReturnValues = {null, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.READ));

    	testActions();
    }

    /*
     * Test the permission on an event with protected reading privacy and writing not set
     */
    @Test
    public void testProtectedEventNotSet() {
    	// Modify Event
    	AccessTokenEventRelation readRelation = new AccessTokenEventRelation(event, userReadToken, Access.AccessType.READ);
    	readRelation.save();

    	AccessTokenEventRelation writeRelation = new AccessTokenEventRelation(event, userWriteToken, Access.AccessType.WRITE);
    	writeRelation.save();
    	
    	event.writingPrivacy = Event.Privacy.NOT_SET;
    	event.readingPrivacy = Event.Privacy.PROTECTED;
        event.save();
        event = Event.find.byId(event.id);

    	// Initialize actions
    	users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
        
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {403, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.NONE));

    	Integer[] anonymousReturnValues = {403, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.NONE));

    	testActions();
    }

    /*
     * Test the permission on an event with protected reading and writing privacy
     */
    @Test
    public void testProtectedEventSet() {
    	// Modify Event
    	AccessTokenEventRelation readRelation = new AccessTokenEventRelation(event, userReadToken, Access.AccessType.READ);
    	readRelation.save();

    	AccessTokenEventRelation writeRelation = new AccessTokenEventRelation(event, userWriteToken, Access.AccessType.WRITE);
    	writeRelation.save();
    	
    	event.writingPrivacy = Event.Privacy.PROTECTED;
    	event.readingPrivacy = Event.Privacy.PROTECTED;
        event.save();
        event = Event.find.byId(event.id);

    	// Initialize actions
    	users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));

    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {403, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.NONE));

    	Integer[] anonymousReturnValues = {403, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.NONE));

    	testActions();
    }

    /*
     * Test the permission on an event with protected reading and private writing privacy
     */
    @Test
    public void testProtectedPrivateEvent() {
    	// Modify Event
    	AccessTokenEventRelation readRelation = new AccessTokenEventRelation(event, userReadToken, Access.AccessType.READ);
    	readRelation.save();
    	
        EventUserRelation writeRelation = new EventUserRelation(event, userWrite, Access.AccessType.WRITE);
        writeRelation.save();

    	event.writingPrivacy = Event.Privacy.PRIVATE;
    	event.readingPrivacy = Event.Privacy.PROTECTED;
        event.save();
        event = Event.find.byId(event.id);

    	// Initialize actions
    	users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
        
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReadReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReadReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {403, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.NONE));

    	Integer[] anonymousReturnValues = {403, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.NONE));

    	testActions();
    }

    /*
     * Test the permission on an event with private reading privacy and writing not set
     */
    @Test
    public void testPrivateEventNotSet() {
    	// Modify Event
        EventUserRelation readingRelation = new EventUserRelation(event, userRead, Access.AccessType.READ);
        readingRelation.save();
        
        EventUserRelation writingRelation = new EventUserRelation(event, userWrite, Access.AccessType.WRITE);
        writingRelation.save();
        
        event.writingPrivacy = Event.Privacy.NOT_SET;
        event.readingPrivacy = Event.Privacy.PRIVATE;
        event.save();
        event = Event.find.byId(event.id);
        
    	// Initialize actions
        users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
    	
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {403, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.NONE));

    	Integer[] anonymousReturnValues = {401, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.NONE));

    	testActions();
    }

    /*
     * Test the permission on an event with private reading and writing privacy
     */
    @Test
    public void testPrivateEventSet() {
    	// Modify Event
        EventUserRelation readingRelation = new EventUserRelation(event, userRead, Access.AccessType.READ);
        readingRelation.save();
        
        EventUserRelation writingRelation = new EventUserRelation(event, userWrite, Access.AccessType.WRITE);
        writingRelation.save();
        
        event.writingPrivacy = Event.Privacy.PRIVATE;
        event.readingPrivacy = Event.Privacy.PRIVATE;
        event.save();
        event = Event.find.byId(event.id);
        
    	// Initialize actions
        users = new HashMap<>();
    	Access.AccessType[] accessTypes = {Access.AccessType.READ, Access.AccessType.WRITE, Access.AccessType.ADMINISTRATE, Access.AccessType.ROOT};

    	Integer[] ownerReturnValues = {null, null, null, null};
    	users.put(ownerUserToken, arraysToMap(accessTypes, ownerReturnValues, Access.AccessType.ROOT));

    	Integer[] adminReturnValues = {null, null, null, 403};
    	users.put(adminUserToken, arraysToMap(accessTypes, adminReturnValues, Access.AccessType.ADMINISTRATE));
    	
    	Integer[] friendWriteReturnValues = {null, null, 403, 403};
    	users.put(userWriteToken, arraysToMap(accessTypes, friendWriteReturnValues, Access.AccessType.WRITE));

    	Integer[] friendReturnValues = {null, 403, 403, 403};
    	users.put(userReadToken, arraysToMap(accessTypes, friendReturnValues, Access.AccessType.READ));

    	Integer[] otherReturnValues = {403, 403, 403, 403};
    	users.put(otherUserToken, arraysToMap(accessTypes, otherReturnValues, Access.AccessType.NONE));

    	Integer[] anonymousReturnValues = {401, 401, 401, 401};
    	users.put(anonymousUserToken, arraysToMap(accessTypes, anonymousReturnValues, Access.AccessType.NONE));

    	testActions();
    }
    
    /*
     * Test different actions
     */
    private void testActions() {
    	for (Map.Entry<AccessToken, UserInfo> userEntry : users.entrySet()) {
    		for (Map.Entry<Access.AccessType, Integer> rightEntry : userEntry.getValue().access.entrySet()) {
    			Result result = Access.hasPermissionOnEvent(userEntry.getKey(), event, rightEntry.getKey());
    			if (rightEntry.getValue() != null && result != null) {
    				assertEquals(rightEntry.getValue(), new Integer(status(result)));
    			} else {
    				assertEquals(rightEntry.getValue(), result);
    			}
    		}
        	assertEquals(userEntry.getValue().maxPermission, Access.getPermissionOnEvent(userEntry.getKey(), event));
    	}
    }
    
    private UserInfo arraysToMap(Access.AccessType[] types, Integer[] values, Access.AccessType maxRight) {
    	Map<Access.AccessType, Integer> res = new HashMap<>();
    	
    	for (int i = 0; i < types.length && i < values.length; ++i) {
    		res.put(types[i], values[i]);
    	}
    	return new UserInfo(res, maxRight);
    }
}
