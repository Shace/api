import static org.junit.Assert.assertEquals;
import static play.test.Helpers.POST;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.status;
import models.AccessToken;
import models.AccessToken.Type;
import models.Event;
import models.Event.Privacy;
import models.EventUserRelation;
import models.Media;
import models.Tag;
import models.User;

import org.junit.Before;
import org.junit.Test;

import play.api.libs.json.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;
import controllers.AccessTokens;
import Utils.Access;

public class TagsController extends WithApplication {

    private Event       publicEvent;
    private Event       privateEvent;
    private User        ownerUser;
    private User        friendUser;
    private User        otherUser;
    private User        adminUser;
    private Media       publicMedia;
    private Media       privateMedia;
    private AccessToken ownerUserToken;
    private AccessToken friendUserToken;
    private AccessToken otherUserToken;
    private AccessToken anonymousUserToken; 
    private AccessToken adminUserToken;

    @Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
        ownerUser = User.create("toto@gmail.com", "secret");
        ownerUserToken = AccessTokens.authenticate(ownerUser.email, "secret", true);

        friendUser = User.create("toto2@gmail.com", "secret2");
        friendUserToken = AccessTokens.authenticate(friendUser.email, "secret2", true);

        otherUser = User.create("toto3@gmail.com", "secret3");
        otherUserToken = AccessTokens.authenticate(otherUser.email, "secret3", true);

        adminUser = User.create("toto4@gmail.com", "secret4");
        adminUser.isAdmin = true;
        adminUser.save();
        adminUserToken = AccessTokens.authenticate(adminUser.email, "secret4", true);

        anonymousUserToken = AccessToken.create(true, null, Type.GUEST);

        publicEvent = new Event(Privacy.PUBLIC, ownerUser);
        publicEvent.token = "event";
        publicEvent.save();
        publicEvent.saveOwnerPermission();

        publicMedia = Media.create("First Photo", ownerUser, publicEvent);

        privateEvent = new Event(Privacy.PRIVATE, ownerUser);
        privateEvent.token = "event2";
        privateEvent.save();
        privateEvent.saveOwnerPermission();

        EventUserRelation relation = new EventUserRelation(privateEvent, friendUser, Access.AccessType.READ);
        relation.save();

        privateMedia = Media.create("First Photo2", ownerUser, privateEvent);
    }

    /**
     * Checks the Tag creation in database through the Tag controller
     */
    @Test
    public void createAndDestroyTag() {
        // Tags public Event
        Integer c1 = standardAddTag("{\"name\":\"test\"}", 201, 1, ownerUserToken.token, publicEvent.token, publicMedia.id);
        standardAddTag("{\"name\":\"test\"}", 400, 1, ownerUserToken.token, publicEvent.token, publicMedia.id);
        Integer c2 = standardAddTag("{\"name\":\"test2\"}", 201, 2, friendUserToken.token, publicEvent.token, publicMedia.id);
        Integer c3 = standardAddTag("{\"name\":\"test3\"}", 201, 3, otherUserToken.token, publicEvent.token, publicMedia.id);
        standardAddTag("{\"name\":\"test\"}", 401, 3, anonymousUserToken.token, publicEvent.token, publicMedia.id);

        // Tags private Event
        Integer c4 = standardAddTag("{\"name\":\"test4\"}", 201, 4, ownerUserToken.token, privateEvent.token, privateMedia.id);
        Integer c5 = standardAddTag("{\"name\":\"test5\"}", 201, 5, friendUserToken.token, privateEvent.token, privateMedia.id);
        standardAddTag("{\"name\":\"test6\"}", 403, 5, otherUserToken.token, privateEvent.token, privateMedia.id);
        standardAddTag("{\"name\":\"test7\"}", 401, 5, anonymousUserToken.token, privateEvent.token, privateMedia.id);

        // Unauthorized deletions
        standardDeleteTag("{}", 403, 5, friendUserToken.token, publicEvent.token, publicMedia.id, c1);
        standardDeleteTag("{}", 403, 5, otherUserToken.token, publicEvent.token, publicMedia.id, c1);
        standardDeleteTag("{}", 401, 5, anonymousUserToken.token, publicEvent.token, publicMedia.id, c1);

        standardDeleteTag("{}", 403, 5, otherUserToken.token, publicEvent.token, publicMedia.id, c2);
        standardDeleteTag("{}", 401, 5, anonymousUserToken.token, publicEvent.token, publicMedia.id, c2);

        standardDeleteTag("{}", 403, 5, friendUserToken.token, publicEvent.token, publicMedia.id, c3);
        standardDeleteTag("{}", 401, 5, anonymousUserToken.token, publicEvent.token, publicMedia.id, c3);

        standardDeleteTag("{}", 403, 5, friendUserToken.token, privateEvent.token, privateMedia.id, c4);
        standardDeleteTag("{}", 403, 5, otherUserToken.token, privateEvent.token, privateMedia.id, c4);
        standardDeleteTag("{}", 401, 5, anonymousUserToken.token, privateEvent.token, privateMedia.id, c4);

        standardDeleteTag("{}", 403, 5, otherUserToken.token, privateEvent.token, privateMedia.id, c5);
        standardDeleteTag("{}", 401, 5, anonymousUserToken.token, privateEvent.token, privateMedia.id, c5);

        // Delete public event
        standardDeleteTag("{}", 204, 4, ownerUserToken.token, publicEvent.token, publicMedia.id, c1);
        standardDeleteTag("{}", 204, 3, friendUserToken.token, publicEvent.token, publicMedia.id, c2);
        standardDeleteTag("{}", 204, 2, otherUserToken.token, publicEvent.token, publicMedia.id, c3);

        // Delete private event
        standardDeleteTag("{}", 204, 1, ownerUserToken.token, privateEvent.token, privateMedia.id, c4);
        standardDeleteTag("{}", 204, 0, friendUserToken.token, privateEvent.token, privateMedia.id, c5);
    }

    @Test
    public void ownerDeletions() {           
        Integer c1 = standardAddTag("{\"name\":\"test\"}", 201, 1, ownerUserToken.token, publicEvent.token, publicMedia.id);
        Integer c2 = standardAddTag("{\"name\":\"test2\"}", 201, 2, friendUserToken.token, publicEvent.token, publicMedia.id);
        Integer c3 = standardAddTag("{\"name\":\"test3\"}", 201, 3, otherUserToken.token, publicEvent.token, publicMedia.id);
        Integer c4 = standardAddTag("{\"name\":\"test4\"}", 201, 4, ownerUserToken.token, privateEvent.token, privateMedia.id);
        Integer c5 = standardAddTag("{\"name\":\"test5\"}", 201, 5, friendUserToken.token, privateEvent.token, privateMedia.id);

        standardDeleteTag("{}", 204, 4, ownerUserToken.token, publicEvent.token, publicMedia.id, c1);
        standardDeleteTag("{}", 204, 3, ownerUserToken.token, publicEvent.token, publicMedia.id, c2);
        standardDeleteTag("{}", 204, 2, ownerUserToken.token, publicEvent.token, publicMedia.id, c3);
        standardDeleteTag("{}", 204, 1, ownerUserToken.token, privateEvent.token, privateMedia.id, c4);
        standardDeleteTag("{}", 204, 0, ownerUserToken.token, privateEvent.token, privateMedia.id, c5);
    }

    @Test
    public void administrativeDeletions() {      
        EventUserRelation relation = new EventUserRelation(publicEvent, otherUser, Access.AccessType.ADMINISTRATE);
        relation.save();

        Integer c1 = standardAddTag("{\"name\":\"test\"}", 201, 1, ownerUserToken.token, publicEvent.token, publicMedia.id);
        Integer c2 = standardAddTag("{\"name\":\"test2\"}", 201, 2, friendUserToken.token, publicEvent.token, publicMedia.id);
        Integer c3 = standardAddTag("{\"name\":\"test3\"}", 201, 3, otherUserToken.token, publicEvent.token, publicMedia.id);

        standardDeleteTag("{}", 204, 2, otherUserToken.token, publicEvent.token, publicMedia.id, c1);
        standardDeleteTag("{}", 204, 1, otherUserToken.token, publicEvent.token, publicMedia.id, c2);
        standardDeleteTag("{}", 204, 0, otherUserToken.token, publicEvent.token, publicMedia.id, c3);
    }

    @Test
    public void administratorDeletions() {
        // Administrator deletions
        Integer c1 = standardAddTag("{\"name\":\"test\"}", 201, 1, ownerUserToken.token, publicEvent.token, publicMedia.id);
        Integer c2 = standardAddTag("{\"name\":\"test2\"}", 201, 2, friendUserToken.token, publicEvent.token, publicMedia.id);
        Integer c3 = standardAddTag("{\"name\":\"test3\"}", 201, 3, otherUserToken.token, publicEvent.token, publicMedia.id);
        Integer c4 = standardAddTag("{\"name\":\"test4\"}", 201, 4, ownerUserToken.token, privateEvent.token, privateMedia.id);
        Integer c5 = standardAddTag("{\"name\":\"test5\"}", 201, 5, friendUserToken.token, privateEvent.token, privateMedia.id);

        standardDeleteTag("{}", 204, 4, adminUserToken.token, publicEvent.token, publicMedia.id, c1);
        standardDeleteTag("{}", 204, 3, adminUserToken.token, publicEvent.token, publicMedia.id, c2);
        standardDeleteTag("{}", 204, 2, adminUserToken.token, publicEvent.token, publicMedia.id, c3);
        standardDeleteTag("{}", 204, 1, adminUserToken.token, privateEvent.token, privateMedia.id, c4);
        standardDeleteTag("{}", 204, 0, adminUserToken.token, privateEvent.token, privateMedia.id, c5);

    }

    /**
     * Simple useful function that generates "add Tag" test from the parameters.
     * @param jsonBody : the request json body
     * @param expectedStatus : the expected Http response status
     * @param expectedNewTagNumber : the expected number of Tag in the table after running this test
     * @param token : the string corresponding to the current connected user (or null if there is not)
     */
    private Integer    standardAddTag(String jsonBody, int expectedStatus, int expectedNewTagNumber, String token, String eventId, Integer mediaId) {
        FakeRequest fakeRequest = new FakeRequest(POST, "/events/" + eventId + "/medias/" + mediaId + "/tags").withJsonBody(Json.parse(jsonBody));
        Result result = callAction(controllers.routes.ref.Tags.add(eventId, mediaId, token), fakeRequest);
        assertEquals(expectedStatus, status(result));
        assertEquals(expectedNewTagNumber, Tag.find.all().size());
        if (status(result) == 201) {
            String id = play.test.Helpers.contentAsString(result).substring(6);
            id = id.substring(0, id.indexOf(','));

            return new Integer(id);
        }

        return 0;
    }

    /**
     * Simple useful function that generates "delete Tag" test from the parameters.
     * @param jsonBody : the request json body
     * @param expectedStatus : the expected Http response status
     * @param expectedNewTagNumber : the expected number of Tag in the table after running this test
     * @param token : the string corresponding to the current connected user (or null if there is not)
     */
    private void    standardDeleteTag(String jsonBody, int expectedStatus, int expectedNewTagNumber, String token, String eventId, Integer mediaId, Integer TagId) {
        FakeRequest fakeRequest = new FakeRequest(play.test.Helpers.DELETE, "/events/" + eventId + "/medias/" + mediaId + "/tags/" + TagId).withJsonBody(Json.parse(jsonBody));
        Result result = callAction(controllers.routes.ref.Tags.delete(eventId, mediaId, TagId, token), fakeRequest);
        assertEquals(expectedStatus, status(result));
        assertEquals(expectedNewTagNumber, Tag.find.all().size());
    }
}
