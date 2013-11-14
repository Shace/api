# Password for loick.michard@gmail.com : 123456

# --- !Ups

INSERT INTO event
	(token, name, description, privacy, creation, id)
	VALUES ('shace', 'ShaceEvent', 'ShaceEvent first event', 0, NOW(), NEXTVAL('event_seq'));

INSERT INTO shace_user
	(id, email, password, first_name, last_name, birth_date, inscription_date)
	VALUES (NEXTVAL('shace_user_seq'), 'loick.michard@gmail.com', '7c4a8d09ca3762af61e59520943dc26494f8941b', 'Loick', 'Michard', NOW(), NOW());

INSERT INTO media
	(id, type, name, description, uri, rank, creation, owner_user_id, owner_event_token)
    VALUES (NEXTVAL('media_seq'), 0, 'First Image', 'Here is the first image of shace event', '/first-image.jpg', 0, NOW(), (SELECT MAX(id) FROM shace_user), 'shace');


# --- !Downs

DELETE FROM event WHERE token = 'shace';
DELETE FROM shace_user WHERE email = 'loick.michard@gmail.com';
DELETE FROM media WHERE name = 'First Image';
