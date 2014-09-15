# --- !Ups

ALTER TABLE se_event ADD COLUMN link_access integer;
ALTER TABLE se_event ADD CONSTRAINT ck_se_event_link_access check (link_access in (0,1,2));
UPDATE se_event SET link_access = 0;

# --- !Downs

# --- !Downs