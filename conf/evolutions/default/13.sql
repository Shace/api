# --- !Ups

ALTER TABLE se_beta_invitation ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_bucket ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_comment ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_event ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_feedback ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_file ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_image ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_media ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_report ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_tag ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_user ADD COLUMN update_time timestamp DEFAULT NOW();
ALTER TABLE se_user_media_like_relation ADD COLUMN update_time timestamp DEFAULT NOW();

# --- !Downs