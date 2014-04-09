import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import models.Event;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.test.WithApplication;

public class EventModel extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerUser = User.create("toto@gmail.com", "secret");
    }
	
    @Test
    public void createRetrieveDeleteEvent() {
    	Event test = new Event(Event.Privacy.PUBLIC, ownerUser);
    	test.token = "event test token";
    	test.save();
    	assertNotNull(test);

    	Event createdEvent = Event.find.byId("event test token");
    	assertNotNull(createdEvent);

    	User user = createdEvent.owner;
    	assertNotNull(user);
    	assertEquals("toto@gmail.com", user.email);

    	int previousSize = Event.find.all().size();
    	createdEvent.delete();
    	assertEquals(previousSize - 1, Event.find.all().size());
    }

    User	ownerUser;
}
