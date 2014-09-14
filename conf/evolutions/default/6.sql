
# --- !Ups

ALTER TABLE se_event_user_relation ADD COLUMN email character varying(254);
UPDATE se_event_user_relation seur SET email = (select se_user.email from se_user where se_user.id = seur.user_id);
ALTER TABLE se_event_user_relation DROP COLUMN user_id;

# --- !Downs

