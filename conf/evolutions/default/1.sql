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

create table event (
  token                     varchar(255) not null,
  id                        integer,
  password                  varchar(40),
  name                      varchar(255),
  description               text,
  privacy                   integer,
  creation                  timestamp,
  constraint ck_event_privacy check (privacy in (0,1,2)),
  constraint uq_event_id unique (id),
  constraint pk_event primary key (token))
;

create table media (
  id                        integer not null,
  type                      integer,
  name                      varchar(255),
  description               text,
  uri                       varchar(255),
  rank                      integer,
  creation                  timestamp,
  owner_user_id             integer,
  owner_event_token         varchar(255),
  constraint ck_media_type check (type in (0,1)),
  constraint pk_media primary key (id))
;

create table shace_user (
  id                        integer not null,
  email                     varchar(254),
  password                  varchar(40),
  first_name                varchar(35),
  last_name                 varchar(35),
  birth_date                timestamp,
  inscription_date          timestamp,
  constraint uq_shace_user_email unique (email),
  constraint pk_shace_user primary key (id))
;

create sequence se_access_token_seq;

create sequence event_seq;

create sequence media_seq;

create sequence shace_user_seq;

alter table se_access_token add constraint fk_se_access_token_user_1 foreign key (user_id) references shace_user (id);
create index ix_se_access_token_user_1 on se_access_token (user_id);
alter table media add constraint fk_media_ownerUser_2 foreign key (owner_user_id) references shace_user (id);
create index ix_media_ownerUser_2 on media (owner_user_id);
alter table media add constraint fk_media_ownerEvent_3 foreign key (owner_event_token) references event (token);
create index ix_media_ownerEvent_3 on media (owner_event_token);



# --- !Downs

drop table if exists se_access_token cascade;

drop table if exists event cascade;

drop table if exists media cascade;

drop table if exists shace_user cascade;

drop sequence if exists se_access_token_seq;

drop sequence if exists event_seq;

drop sequence if exists media_seq;

drop sequence if exists shace_user_seq;

