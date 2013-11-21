# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table se_access_token (
  token                     varchar(40) not null,
  auto_renew                boolean,
  creation                  timestamp,
  expiration                timestamp,
  user_id                   integer,
  type                      integer,
  constraint ck_se_access_token_type check (type in (0,1)),
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

create table se_user (
  id                        integer not null,
  email                     varchar(254),
  password                  varchar(40),
  first_name                varchar(35),
  last_name                 varchar(35),
  birth_date                timestamp,
  inscription_date          timestamp,
  is_admin                  boolean,
  constraint uq_se_user_email unique (email),
  constraint pk_se_user primary key (id))
;

create sequence se_access_token_seq;

create sequence se_event_seq;

create sequence se_media_seq;

create sequence se_user_seq;

alter table se_access_token add constraint fk_se_access_token_user_1 foreign key (user_id) references se_user (id);
create index ix_se_access_token_user_1 on se_access_token (user_id);
alter table se_media add constraint fk_se_media_ownerUser_2 foreign key (owner_user_id) references se_user (id);
create index ix_se_media_ownerUser_2 on se_media (owner_user_id);
alter table se_media add constraint fk_se_media_ownerEvent_3 foreign key (owner_event_id) references se_event (token);
create index ix_se_media_ownerEvent_3 on se_media (owner_event_id);



# --- !Downs

drop table if exists se_access_token cascade;

drop table if exists se_event cascade;

drop table if exists se_media cascade;

drop table if exists se_user cascade;

drop sequence if exists se_access_token_seq;

drop sequence if exists se_event_seq;

drop sequence if exists se_media_seq;

drop sequence if exists se_user_seq;

