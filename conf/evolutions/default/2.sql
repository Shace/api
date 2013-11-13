# Password for loick.michard@gmail.com : 123456

# --- !Ups

INSERT INTO event (token, name, description, privacy, creation, id) values ('shace', 'ShaceEvent', 'ShaceEvent first event', 0, NOW(), nextval('event_seq'));
INSERT INTO shace_user (id, email, password, first_name, last_name, birth_date, inscription_date) values (nextval('shace_user_seq'), 'loick.michard@gmail.com', '7c4a8d09ca3762af61e59520943dc26494f8941b', 'Loick', 'Michard', NOW(), NOW());

# --- !Downs

DELETE FROM event WHERE token = 'shace';
DELETE FROM shace_user WHERE email = 'loick.michard@gmail.com';