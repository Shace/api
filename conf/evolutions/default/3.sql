# Add mails

# --- !Ups

INSERT INTO se_email
       (id, type, lang, subject, from_email, html)
       VALUES 	(0, 0, 1, '[Shace] Demande beta envoyée', 'Shace <noreply@shace.io>', '<html>Bonjour $$FIRSTNAME$$,<br/><br/>Votre demande d''accès à la béta privée a été envoyée. Vous recevrez un email de confirmation de votre inscription.<br/><br/>Reprenez le contrôle de vos photos,<br/>L''équipe Shace.</html>'),
       			(1, 0, 2, '[Shace] Beta request sent', 'Shace <noreply@shace.io>', '<html>Hi $$FIRSTNAME$$,<br/><br/>You request to join the beta has been sent. You will receive an email to confirm your registration soon. <br/><br/>Regain control of your photos,<br/>The Shace team.</html>'),
       			(2, 1, 1, '[Shace] Demande beta acceptée', 'Shace <noreply@shace.io>', '<html>Bonjour $$FIRSTNAME$$,<br/><br/>Votre demande d''accès à la béta privée a été acceptée ! Vous pouvez vous connecter en cliquant sur le lien suivant : <a href="http://www.shace.io/#/login">http://www.shace.io/#/login</a>.<br/><br/>Reprenez le contrôle de vos photos,<br/>L''équipe Shace.</html>'),
       			(3, 1, 2, '[Shace] Beta request accepted', 'Shace <noreply@shace.io>', '<html>Hi $$FIRSTNAME$$,<br/><br/>You request to join the beta has been accepted! You can now sign in here: <a href="http://www.shace.io/#/login">http://www.shace.io/#/login</a>.<br/><br/>Regain control of your photos,<br/>The Shace team.</html>'),
 				(4, 2, 1, '[Shace] Invitation beta', 'Shace <noreply@shace.io>', '<html>Bonjour,<br/><br/>Vous avez été invité par $$FIRSTNAME$$ $$LASTNAME$$ à rejoindre l''aventure Shace! Vous pouvez vous inscrire en cliquant sur le lien suivant : <a href="http://www.shace.io/#/signup">http://www.shace.io/#/signup</a>.<br/><br/>Reprenez le contrôle de vos photos,<br/>L''équipe Shace.</html>'),
       			(5, 2, 2, '[Shace] Beta invitation', 'Shace <noreply@shace.io>', '<html>Hi,<br/><br/>You have been invited by $$FIRSTNAME$$ $$LASTNAME$$ to join the Shace adventure! You can now sign up here: <a href="http://www.shace.io/#/signup">http://www.shace.io/#/signup</a>.<br/><br/>Regain control of your photos,<br/>The Shace team.</html>')
       	      ;

# --- !Downs

DELETE FROM se_email;