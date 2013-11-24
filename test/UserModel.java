import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;

import java.util.Date;

import models.User;

import org.junit.Before;
import org.junit.Test;

import play.test.WithApplication;

public class UserModel extends WithApplication {

	@Before
    public void setUp() {
        start(fakeApplication(inMemoryDatabase()));
    }
	
    @Test
    public void createAndRetrieveUser() {
        User.create("chuck.norris@shace.com", "0000");
        
        /**
         * Check user creation
         */
    	User user = User.find.where().eq("email", "chuck.norris@shace.com").findUnique();

    	assertNotNull(user);
    	assertEquals(user.isAdmin, false);
    	Date now = new Date();
    	
    	if (user.inscriptionDate.getTime() < now.getTime() - 1000
    	        || user.inscriptionDate.getTime() > now.getTime()) {
    	    fail();
    	}
    	
    	
    	/**
    	 * Check user update
    	 */
    	user.firstName = "Chuck";
    	user.lastName = "Norris";
    	user.birthDate = new Date();
    	user.isAdmin = true;
    	
    	user.update();
    	
    	user = User.find.where().eq("email", "chuck.norris@shace.com").findUnique();
    	
    	assertNotNull(user);
        now = new Date();
        
        if (user.inscriptionDate.getTime() < now.getTime() - 1000
                || user.inscriptionDate.getTime() > now.getTime()) {
            fail();
        }
        
        if (user.birthDate.getTime() < now.getTime() - 1000
                || user.birthDate.getTime() > now.getTime()) {
            fail();
        }
        
        assertEquals(user.firstName, "Chuck");
        assertEquals(user.lastName, "Norris");
        assertEquals(user.isAdmin, true);
    }
}
