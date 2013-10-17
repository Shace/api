# --- !Ups

INSERT INTO event (token, name, description, privacy, creation, id) values ('shace', 'ShaceEvent', 'ShaceEvent first event', 0, NOW(), nextval('event_seq'));

# --- !Downs

DELETE FROM event WHERE token = 'shace';