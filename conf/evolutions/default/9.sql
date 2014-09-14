# --- !Ups

ALTER TABLE se_event ADD COLUMN start_date timestamp;
ALTER TABLE se_event ADD COLUMN finish_date timestamp;

create table se_user_media_like_relation (
  id                        integer not null,
  media_id                  integer,
  user_id                   integer,
  creation                  timestamp,
  constraint pk_se_user_media_like_relation primary key (id))
;

create sequence se_user_media_like_relation_seq;


# --- !Downs