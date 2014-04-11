# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table se_access_token (
  token                     varchar(40) not null,
  auto_renew                boolean,
  creation                  bigint,
  expiration                bigint,
  user_id                   integer,
  type                      integer,
  constraint ck_se_access_token_type check (type in (0,1)),
  constraint pk_se_access_token primary key (token))
;

create table se_bucket (
  id                        integer not null,
  name                      varchar(255),
  parent_id                 integer,
  level                     integer,
  first                     timestamp,
  last                      timestamp,
  size                      integer,
  event_id                  varchar(255),
  constraint pk_se_bucket primary key (id))
;

create table se_event (
  token                     varchar(255) not null,
  id                        varchar(36),
  password                  varchar(40),
  name                      varchar(255),
  description               varchar(255),
  reading_privacy           integer,
  writing_privacy           integer,
  creation                  timestamp,
  root_id                   integer,
  constraint ck_se_event_reading_privacy check (reading_privacy in (0,1,2)),
  constraint ck_se_event_writing_privacy check (writing_privacy in (0,1,2)),
  constraint uq_se_event_id unique (id),
  constraint pk_se_event primary key (token))
;

create table se_event_user_relation (
  id                        integer not null,
  event_token               varchar(255),
  user_id                   integer,
  permission                integer,
  constraint ck_se_event_user_relation_permission check (permission in (0,1,2,3,4)),
  constraint pk_se_event_user_relation primary key (id))
;

create table se_file (
  id                        integer not null,
  creation                  timestamp,
  uid                       varchar(255),
  base_url                  varchar(255),
  constraint pk_se_file primary key (id))
;

create table se_image (
  id                        integer not null,
  creation                  timestamp,
  constraint pk_se_image primary key (id))
;

create table se_image_file_relation (
  id                        integer not null,
  image_id                  integer,
  file_id                   integer,
  width                     integer,
  height                    integer,
  format                    varchar(255),
  constraint pk_se_image_file_relation primary key (id))
;

create table se_image_format (
  name                      varchar(255) not null,
  width                     integer,
  height                    integer,
  crop                      boolean,
  constraint pk_se_image_format primary key (name))
;

create table se_media (
  id                        integer not null,
  type                      integer,
  name                      varchar(255),
  description               varchar(255),
  rank                      integer,
  creation                  timestamp,
  owner_id                  integer,
  event_id                  varchar(255),
  image_id                  integer,
  original                  timestamp,
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
  is_admin                  boolean,
  constraint uq_se_user_email unique (email),
  constraint pk_se_user primary key (id))
;


create table se_bucket_media (
  se_bucket_id                   integer not null,
  se_media_id                    integer not null,
  constraint pk_se_bucket_media primary key (se_bucket_id, se_media_id))
;
create sequence se_access_token_seq;

create sequence se_bucket_seq;

create sequence se_event_seq;

create sequence se_event_user_relation_seq;

create sequence se_file_seq;

create sequence se_image_seq;

create sequence se_image_file_relation_seq;

create sequence se_image_format_seq;

create sequence se_media_seq;

create sequence se_tag_seq;

create sequence se_user_seq;

alter table se_access_token add constraint fk_se_access_token_user_1 foreign key (user_id) references se_user (id);
create index ix_se_access_token_user_1 on se_access_token (user_id);
alter table se_bucket add constraint fk_se_bucket_parent_2 foreign key (parent_id) references se_bucket (id);
create index ix_se_bucket_parent_2 on se_bucket (parent_id);
alter table se_bucket add constraint fk_se_bucket_event_3 foreign key (event_id) references se_event (token);
create index ix_se_bucket_event_3 on se_bucket (event_id);
alter table se_event add constraint fk_se_event_root_4 foreign key (root_id) references se_bucket (id);
create index ix_se_event_root_4 on se_event (root_id);
alter table se_event_user_relation add constraint fk_se_event_user_relation_even_5 foreign key (event_token) references se_event (token);
create index ix_se_event_user_relation_even_5 on se_event_user_relation (event_token);
alter table se_event_user_relation add constraint fk_se_event_user_relation_user_6 foreign key (user_id) references se_user (id);
create index ix_se_event_user_relation_user_6 on se_event_user_relation (user_id);
alter table se_image_file_relation add constraint fk_se_image_file_relation_imag_7 foreign key (image_id) references se_image (id);
create index ix_se_image_file_relation_imag_7 on se_image_file_relation (image_id);
alter table se_image_file_relation add constraint fk_se_image_file_relation_file_8 foreign key (file_id) references se_file (id);
create index ix_se_image_file_relation_file_8 on se_image_file_relation (file_id);
alter table se_media add constraint fk_se_media_owner_9 foreign key (owner_id) references se_user (id);
create index ix_se_media_owner_9 on se_media (owner_id);
alter table se_media add constraint fk_se_media_event_10 foreign key (event_id) references se_event (token);
create index ix_se_media_event_10 on se_media (event_id);
alter table se_media add constraint fk_se_media_image_11 foreign key (image_id) references se_image (id);
create index ix_se_media_image_11 on se_media (image_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_medi_12 foreign key (media_id) references se_media (id);
create index ix_se_media_tag_relation_medi_12 on se_media_tag_relation (media_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_tag_13 foreign key (tag_id) references se_tag (id);
create index ix_se_media_tag_relation_tag_13 on se_media_tag_relation (tag_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_crea_14 foreign key (user_id) references se_user (id);
create index ix_se_media_tag_relation_crea_14 on se_media_tag_relation (user_id);



alter table se_bucket_media add constraint fk_se_bucket_media_se_bucket_01 foreign key (se_bucket_id) references se_bucket (id);

alter table se_bucket_media add constraint fk_se_bucket_media_se_media_02 foreign key (se_media_id) references se_media (id);

# --- !Downs

drop table if exists se_access_token cascade;

drop table if exists se_bucket cascade;

drop table if exists se_bucket_media cascade;

drop table if exists se_event cascade;

drop table if exists se_event_user_relation cascade;

drop table if exists se_file cascade;

drop table if exists se_image cascade;

drop table if exists se_image_file_relation cascade;

drop table if exists se_image_format cascade;

drop table if exists se_media cascade;

drop table if exists se_media_tag_relation cascade;

drop table if exists se_tag cascade;

drop table if exists se_user cascade;

drop sequence if exists se_access_token_seq;

drop sequence if exists se_bucket_seq;

drop sequence if exists se_event_seq;

drop sequence if exists se_event_user_relation_seq;

drop sequence if exists se_file_seq;

drop sequence if exists se_image_seq;

drop sequence if exists se_image_file_relation_seq;

drop sequence if exists se_image_format_seq;

drop sequence if exists se_media_seq;

drop sequence if exists se_tag_seq;

drop sequence if exists se_user_seq;

