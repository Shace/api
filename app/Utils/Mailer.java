package Utils;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import play.Logger;
import models.AccessToken.Lang;
import models.Email;

import com.typesafe.config.ConfigFactory;

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
				Logger.debug("Getting mail in database");
				Email email = Email.find.where().eq("type", this.type.code).where().eq("lang", this.lang).findUnique();

				if (email != null) {

					Logger.debug("Start sending mail to " + this.email);

					for (Map.Entry<String, String> entry : this.params.entrySet()) {
						email.html = email.html.replace("$$" + entry.getKey() + "$$", entry.getValue());
						email.subject = email.subject.replace("$$" + entry.getKey() + "$$", entry.getValue());
					}

					Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			            protected PasswordAuthentication getPasswordAuthentication() {
			                return new PasswordAuthentication (
			                   smtpEmail, smtpPassword);
			             }
			          });
					session.setDebug(true);

					try{
						MimeMessage message = new MimeMessage(session);

						message.setFrom(new InternetAddress(email.fromEmail));
						message.addRecipient(Message.RecipientType.TO,
								new InternetAddress(this.email));

						message.setSubject(email.subject);
						message.setContent(email.html, "text/html");

						Logger.debug("Mail is ready to go");
						Transport.send(message);
						Logger.debug("Sending mail to " + this.email);
					} catch (MessagingException mex) {
						Logger.error(mex.getMessage());
						return "Run";
					}
				}
			} else if (this.mode == EmailMode.CUSTOM_EMAIL) {
				Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
		            protected PasswordAuthentication getPasswordAuthentication() {
		                return new PasswordAuthentication (
		                   smtpEmail, smtpPassword);
		             }
		          });
				session.setDebug(true);

				try{
					MimeMessage message = new MimeMessage(session);

					message.setFrom(new InternetAddress(this.fromEmail));
					message.addRecipient(Message.RecipientType.TO,
							new InternetAddress(this.email));

					message.setSubject(this.subject);
					message.setContent(this.content, "text/html");

					Logger.debug("Mail is ready to go");
					Transport.send(message);
					Logger.debug("Sending mail to " + this.email);
				} catch (MessagingException mex) {
					Logger.error(mex.getMessage());
					return "Run";
				}
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
	private Properties properties;
	private String smtpEmail;
	private String smtpPassword;

	private Mailer() {
		pool = Executors.newFixedThreadPool(1);
		this.enabled = ConfigFactory.load().getBoolean("mail.enabled");
		
		properties = System.getProperties();
		properties.setProperty("mail.transport.protocol", "smtp");
		properties.setProperty("mail.smtp.host", ConfigFactory.load().getString("smtp.host"));
		properties.put("mail.smtp.port", ConfigFactory.load().getInt("smtp.port"));
		properties.put("mail.smtp.auth", true);
		properties.put("mail.smtp.ssl.enable", true);
		properties.put("mail.smtp.timeout", 5000);
		properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
		
		smtpEmail = ConfigFactory.load().getString("smtp.user");
		smtpPassword = ConfigFactory.load().getString("smtp.password");
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
