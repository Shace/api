# Password for loick.michard@gmail.com : 123456

# --- !Ups

INSERT INTO se_event
	(token, name, description, privacy, creation, id)
	VALUES ('shace', 'ShaceEvent', 'ShaceEvent first event', 0, NOW(), NEXTVAL('se_event_seq'));

INSERT INTO se_user
	(id, email, password, first_name, last_name, birth_date, inscription_date, is_admin)
	VALUES (NEXTVAL('se_user_seq'), 'loick.michard@gmail.com', '7c4a8d09ca3762af61e59520943dc26494f8941b', 'Loick', 'Michard', NOW(), NOW(), false),
			(NEXTVAL('se_user_seq'), 'admin@shace.com', '7c4a8d09ca3762af61e59520943dc26494f8941b', 'Admin', 'Admin', NOW(), NOW(), true);

INSERT INTO se_media
	(id, type, name, description, uri, rank, creation, owner_user_id, owner_event_id)
    VALUES (NEXTVAL('se_media_seq'), 0, 'First Image', 'Here is the first image of shace event', '/first-image.jpg', 0, NOW(), (SELECT MAX(id) FROM se_user), 'shace');


# --- !Downs

DELETE FROM se_access_token;
DELETE FROM se_media WHERE name = 'First Image';
DELETE FROM se_event WHERE token = 'shace';
DELETE FROM se_user WHERE email = 'loick.michard@gmail.com';
