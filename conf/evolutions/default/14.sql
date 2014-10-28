# --- !Ups

ALTER TABLE se_image ADD COLUMN latitude FLOAT;
ALTER TABLE se_image ADD COLUMN longitude FLOAT;

UPDATE se_image SET latitude = 0, longitude = 0;

# --- !Downs