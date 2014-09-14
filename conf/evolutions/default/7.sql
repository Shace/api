# --- !Ups

INSERT INTO se_email
       (id, type, lang, subject, from_email, html)
       VALUES 	(6, 3, 1, '[Shace] Invitation évènement', 'Shace <noreply@shace.io>', '<html>Bonjour $$USER_FIRSTNAME$$,<br/><br/>Vous avez été invité par $$FIRSTNAME$$ $$LASTNAME$$ à rejoindre l''évènement $$EVENT$$ ! Vous pouvez y accéder en cliquant sur le lien suivant : <a href="http://www.shace.io/#/events/$$TOKEN$$">http://www.shace.io/#/events/$$TOKEN$$</a>.<br/><br/>Reprenez le contrôle de vos photos,<br/>L''équipe Shace.</html>'),
       			(7, 3, 2, '[Shace] Event invitation', 'Shace <noreply@shace.io>', '<html>Hi $$USER_FIRSTNAME$$,<br/><br/>You have been invited by $$FIRSTNAME$$ $$LASTNAME$$ to join the event $$EVENT$$! You can access to the event here: <a href="http://www.shace.io/#/events/$$TOKEN$$">http://www.shace.io/#/events/$$TOKEN$$</a>.<br/><br/>Regain control of your photos,<br/>The Shace team.</html>'),
       			(8, 4, 1, '[Shace] Invitation évènement', 'Shace <noreply@shace.io>', '<html>Bonjour,<br/><br/>Vous avez été invité par $$FIRSTNAME$$ $$LASTNAME$$ à rejoindre l''évènement $$EVENT$$ ! Vous pouvez vous inscrire en cliquant sur le lien suivant : <a href="http://www.shace.io/#/signup">http://www.shace.io/#/signup</a>.<br />Vous pouvez y accéder en cliquant sur le lien suivant : <a href="http://www.shace.io/#/events/$$TOKEN$$">http://www.shace.io/#/events/$$TOKEN$$</a>.<br/><br/>Reprenez le contrôle de vos photos,<br/>L''équipe Shace.</html>'),
       			(9, 4, 2, '[Shace] Event invitation', 'Shace <noreply@shace.io>', '<html>Hi,<br/><br/>You have been invited by $$FIRSTNAME$$ $$LASTNAME$$ to join the event $$EVENT$$! You can now sign up here: <a href="http://www.shace.io/#/signup">http://www.shace.io/#/signup</a>.<br/>You can access to the event here: <a href="http://www.shace.io/#/events/$$TOKEN$$">http://www.shace.io/#/events/$$TOKEN$$</a>.<br/><br/>Regain control of your photos,<br/>The Shace team.</html>')
       ;

# --- !Downs
