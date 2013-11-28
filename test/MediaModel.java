import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import models.Event;
import models.Event.Privacy;
import models.Media;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.test.WithApplication;

public class MediaModel extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerUser = User.create("toto@gmail.com", "secret");
        ownerEvent = Event.create("First Event", Privacy.PUBLIC, ownerUser);
    }
	
    @Test
    public void createRetrieveDeleteMedia() {
    	Media test = Media.create("First Photo", ownerUser, ownerEvent);
    	assertNotNull(test);

    	Media createdMedia = Media.find.where().eq("name", "First Photo").findUnique();
    	assertNotNull(createdMedia);

    	User user = createdMedia.ownerUser;
    	assertNotNull(user);
    	assertEquals("toto@gmail.com", user.email);

    	Event event = createdMedia.ownerEvent;
    	assertNotNull(event);
    	assertEquals("First Event", event.token);
    	
    	int previousSize = Media.find.all().size();
    	createdMedia.delete();
    	assertEquals(previousSize - 1, Media.find.all().size());
    }
	
     Event	ownerEvent;
    User	ownerUser;
}
