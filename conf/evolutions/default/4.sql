
# --- !Ups

ALTER TABLE se_media ADD COLUMN valid BOOLEAN DEFAULT TRUE NOT NULL;

# --- !Downs

ALTER TABLE se_media DROP COLUMN valid;
