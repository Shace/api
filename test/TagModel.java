import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import models.Event;
import models.Event.Privacy;
import models.Media;
import models.Tag;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.test.WithApplication;

public class TagModel extends WithApplication {

    Event   ownerEvent;
    User    ownerUser;
    Media   media;
  
    @Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerUser = User.create("toto@gmail.com", "secret");
        ownerEvent = new Event(Privacy.PUBLIC, ownerUser);
        ownerEvent.token = "First Event";
        ownerEvent.save();
        
        media = Media.create("First Photo", ownerUser, ownerEvent);
    }
    
    @Test
    public void createRetrieveDeleteTag() {
        Tag test = Tag.create("First tag", ownerUser, media);
        assertNotNull(test);

        Tag createdTag = Tag.find.where().eq("name", "First tag").findUnique();
        assertNotNull(createdTag);

        User user = createdTag.creator;
        assertNotNull(user);
        assertEquals("toto@gmail.com", user.email);

        Media media = createdTag.media;
        assertNotNull(media);
        assertEquals("First Photo", media.name);
        
        int previousSize = Tag.find.all().size();
        test.delete();
        assertEquals(previousSize - 1, Tag.find.all().size());
    }
}
