# --- !Ups

ALTER TABLE se_feedback ADD COLUMN answer TEXT;

UPDATE se_feedback SET answer = NULL;

# --- !Downs