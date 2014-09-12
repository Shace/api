package Utils;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jetty.util.log.Log;

import play.Logger;
import play.api.Play;
import models.AccessToken.Lang;
import models.Email;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import com.typesafe.plugin.MailerAPI;
import com.typesafe.plugin.MailerPlugin;

public class Mailer {
	
	public enum EmailType {
		BETA_REQUEST_SENT			(0),
		BETA_REQUEST_ACCEPTED		(1),
		BETA_INVITATION				(2)
		;

		public int code;

		private EmailType(int code) {
			this.code = code;
		}

	}
	
	private enum EmailMode {
		DB_EMAIL,
		CUSTOM_EMAIL
	}
	
	private final class Task implements Callable<String> {
		
			EmailType 			type;
			Lang				lang;
			Map<String, String>	params;
			String				email;
			EmailMode			mode;
			String				subject;
			String				content;
			String				fromEmail;
			
		
			public Task(EmailType type, Lang lang, String email, Map<String, String> params) {
				this.lang = lang;
				if (this.lang == Lang.NONE) {
					this.lang = Lang.EN;
				}
				this.type = type;
				this.params = params;
				this.email = email;
				this.mode = EmailMode.DB_EMAIL;
			}
		
			public Task(String email, String subject, String content, String fromEmail) {
				this.email = email;
				this.mode = EmailMode.CUSTOM_EMAIL;
				this.subject = subject;
				this.content = content;
				this.fromEmail = fromEmail;
			}
		
			public String call() {
				if (this.mode == EmailMode.DB_EMAIL) {
					Email email = Email.find.where().eq("type", this.type.code).where().eq("lang", this.lang).findUnique();
					
					if (email != null) {
				    	MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
				    	
				    	for (Map.Entry<String, String> entry : this.params.entrySet()) {
				    		email.html = email.html.replace("$$" + entry.getKey() + "$$", entry.getValue());
				    		email.subject = email.subject.replace("$$" + entry.getKey() + "$$", entry.getValue());
				    	}
				    	
				    	mail.setSubject(email.subject);
				    	String recipient = "";
				    	if (this.params.containsKey("FIRSTNAME")) {
				    		recipient += this.params.get("FIRSTNAME") + " ";
				    	}
				    	if (this.params.containsKey("LASTNAME")) {
				    		recipient += this.params.get("LASTNAME") + " ";
				    	}
				    	recipient += "<" + this.email + ">";
				    	mail.setRecipient(recipient, this.email);
				    	mail.setFrom(email.fromEmail);
				    	mail.sendHtml(email.html);
				    	
				    	Logger.debug("Sending mail to " + this.email);
					}
				} else if (this.mode == EmailMode.CUSTOM_EMAIL) {
			    	MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
			    	mail.setSubject(this.subject);
			    	mail.setRecipient("<" + this.email + ">", this.email);
			    	mail.setFrom(this.fromEmail);
			    	mail.sendHtml(this.content);
				}
				return "Run";
			}
		}
	
    private static final Mailer instance = new Mailer(); 
    private boolean enabled = false;

    public static Mailer get() { 
        return instance; 
    }
    
    private ExecutorService pool;
    
    private Mailer() {
    	pool = Executors.newFixedThreadPool(1);
        this.enabled = ConfigFactory.load().getBoolean("mail.enabled");
    }
    
    public void sendMail(EmailType type, Lang lang, String email, Map<String, String> params) {
    	if (enabled) {
    		pool.submit(new Task(type, lang, email, params));
    	}
    }

    public void sendMail(String email, String subject, String content, String fromEmail) {
    	if (enabled) {
    		pool.submit(new Task(email, subject, content, fromEmail));
    	}
    }
}
