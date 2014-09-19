# --- !Ups

ALTER TABLE se_event ADD COLUMN deleted boolean;
UPDATE se_event SET deleted = false;

# --- !Downs