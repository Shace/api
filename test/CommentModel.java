import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import models.Comment;
import models.Event;
import models.Event.Privacy;
import models.Media;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.test.WithApplication;

public class CommentModel extends WithApplication {

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
    public void createRetrieveDeleteComment() {
        Comment test = Comment.create(ownerUser, media, "First comment");
        assertNotNull(test);

        Comment createdComment = Comment.find.where().eq("message", "First comment").findUnique();
        assertNotNull(createdComment);

        User user = createdComment.owner;
        assertNotNull(user);
        assertEquals("toto@gmail.com", user.email);

        Media media = createdComment.media;
        assertNotNull(media);
        assertEquals("First Photo", media.name);
        
        int previousSize = Comment.find.all().size();
        test.delete();
        assertEquals(previousSize - 1, Comment.find.all().size());
    }
}
