# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table se_access_token (
  token                     varchar(40) not null,
  auto_renew                boolean,
  creation                  timestamp,
  expiration                timestamp,
  user_id                   integer,
  constraint pk_se_access_token primary key (token))
;

create table se_event (
  token                     varchar(255) not null,
  id                        integer,
  password                  varchar(40),
  name                      varchar(255),
  description               text,
  privacy                   integer,
  creation                  timestamp,
  constraint ck_se_event_privacy check (privacy in (0,1,2)),
  constraint uq_se_event_id unique (id),
  constraint pk_se_event primary key (token))
;

create table se_media (
  id                        integer not null,
  type                      integer,
  name                      varchar(255),
  description               text,
  uri                       varchar(255),
  rank                      integer,
  creation                  timestamp,
  owner_user_id             integer,
  owner_event_id            varchar(255),
  constraint ck_se_media_type check (type in (0,1,2)),
  constraint pk_se_media primary key (id))
;

create table se_media_tag_relation (
  media_id                  integer,
  tag_id                    integer,
  user_id                   integer)
;

create table se_tag (
  id                        integer not null,
  name                      varchar(255),
  slug                      varchar(255),
  constraint pk_se_tag primary key (id))
;

create table se_user (
  id                        integer not null,
  email                     varchar(254),
  password                  varchar(40),
  first_name                varchar(35),
  last_name                 varchar(35),
  birth_date                timestamp,
  inscription_date          timestamp,
  constraint uq_se_user_email unique (email),
  constraint pk_se_user primary key (id))
;

create sequence se_access_token_seq;

create sequence se_event_seq;

create sequence se_media_seq;

create sequence se_tag_seq;

create sequence se_user_seq;

alter table se_access_token add constraint fk_se_access_token_user_1 foreign key (user_id) references se_user (id);
create index ix_se_access_token_user_1 on se_access_token (user_id);
alter table se_media add constraint fk_se_media_ownerUser_2 foreign key (owner_user_id) references se_user (id);
create index ix_se_media_ownerUser_2 on se_media (owner_user_id);
alter table se_media add constraint fk_se_media_ownerEvent_3 foreign key (owner_event_id) references se_event (token);
create index ix_se_media_ownerEvent_3 on se_media (owner_event_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_media_4 foreign key (media_id) references se_media (id);
create index ix_se_media_tag_relation_media_4 on se_media_tag_relation (media_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_tag_5 foreign key (tag_id) references se_tag (id);
create index ix_se_media_tag_relation_tag_5 on se_media_tag_relation (tag_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_creat_6 foreign key (user_id) references se_user (id);
create index ix_se_media_tag_relation_creat_6 on se_media_tag_relation (user_id);



# --- !Downs

drop table if exists se_access_token cascade;

drop table if exists se_event cascade;

drop table if exists se_media cascade;

drop table if exists se_media_tag_relation cascade;

drop table if exists se_tag cascade;

drop table if exists se_user cascade;

drop sequence if exists se_access_token_seq;

drop sequence if exists se_event_seq;

drop sequence if exists se_media_seq;

drop sequence if exists se_tag_seq;

drop sequence if exists se_user_seq;

