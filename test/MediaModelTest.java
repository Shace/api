import models.*;
import org.junit.*;
import static org.junit.Assert.*;
import play.test.WithApplication;
import static play.test.Helpers.*;

public class MediaModelTest extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerEvent = Event.create("First Event", "First Shace's Event", "This is the first Event of the awesome shace app");
        ownerUser = User.create("toto@gmail.com", "secret");
    }
	
    @Test
    public void createAndRetrieveMedia() {
    	Media.create("First Photo", ownerUser, ownerEvent);
    	Media createdMedia = Media.find.where().eq("name", "First Photo").findUnique();

    	assertNotNull(createdMedia);

    	User user = createdMedia.ownerUser;
    	assertNotNull(user);
    	assertEquals("toto@gmail.com", user.email);

    	Event event = createdMedia.ownerEvent;
    	assertNotNull(event);
    	assertEquals("First Event", event.token);
    }
    
    Event	ownerEvent;
    User	ownerUser;
}
