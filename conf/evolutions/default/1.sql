# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table se_access_token (
  token                     varchar(40) not null,
  auto_renew                tinyint(1) default 0,
  creation                  bigint,
  expiration                bigint,
  user_id                   integer,
  type                      integer,
  constraint ck_se_access_token_type check (type in (0,1)),
  constraint pk_se_access_token primary key (token))
;

create table se_event (
  token                     varchar(255) not null,
  password                  varchar(40),
  name                      varchar(255),
  description               varchar(255),
  privacy                   integer,
  creation                  datetime,
  owner_id                  integer,
  constraint ck_se_event_privacy check (privacy in (0,1,2)),
  constraint pk_se_event primary key (token))
;

create table se_file (
  id                        integer auto_increment not null,
  creation                  datetime,
  uid                       varchar(255),
  constraint pk_se_file primary key (id))
;

create table se_image (
  id                        integer auto_increment not null,
  creation                  datetime,
  constraint pk_se_image primary key (id))
;

create table se_image_file_relation (
  id                        integer auto_increment not null,
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
  crop                      tinyint(1) default 0,
  constraint pk_se_image_format primary key (name))
;

create table se_media (
  id                        integer auto_increment not null,
  type                      integer,
  name                      varchar(255),
  description               varchar(255),
  rank                      integer,
  creation                  datetime,
  owner_id                  integer,
  event_id                  varchar(255),
  image_id                  integer,
  constraint ck_se_media_type check (type in (0,1,2)),
  constraint pk_se_media primary key (id))
;

create table se_media_tag_relation (
  media_id                  integer,
  tag_id                    integer,
  user_id                   integer)
;

create table se_tag (
  id                        integer auto_increment not null,
  name                      varchar(255),
  slug                      varchar(255),
  constraint pk_se_tag primary key (id))
;

create table se_user (
  id                        integer auto_increment not null,
  email                     varchar(254),
  password                  varchar(40),
  first_name                varchar(35),
  last_name                 varchar(35),
  birth_date                datetime,
  inscription_date          datetime,
  is_admin                  tinyint(1) default 0,
  constraint uq_se_user_email unique (email),
  constraint pk_se_user primary key (id))
;

alter table se_access_token add constraint fk_se_access_token_user_1 foreign key (user_id) references se_user (id) on delete restrict on update restrict;
create index ix_se_access_token_user_1 on se_access_token (user_id);
alter table se_event add constraint fk_se_event_owner_2 foreign key (owner_id) references se_user (id) on delete restrict on update restrict;
create index ix_se_event_owner_2 on se_event (owner_id);
alter table se_image_file_relation add constraint fk_se_image_file_relation_image_3 foreign key (image_id) references se_image (id) on delete restrict on update restrict;
create index ix_se_image_file_relation_image_3 on se_image_file_relation (image_id);
alter table se_image_file_relation add constraint fk_se_image_file_relation_file_4 foreign key (file_id) references se_file (id) on delete restrict on update restrict;
create index ix_se_image_file_relation_file_4 on se_image_file_relation (file_id);
alter table se_media add constraint fk_se_media_owner_5 foreign key (owner_id) references se_user (id) on delete restrict on update restrict;
create index ix_se_media_owner_5 on se_media (owner_id);
alter table se_media add constraint fk_se_media_event_6 foreign key (event_id) references se_event (token) on delete restrict on update restrict;
create index ix_se_media_event_6 on se_media (event_id);
alter table se_media add constraint fk_se_media_image_7 foreign key (image_id) references se_image (id) on delete restrict on update restrict;
create index ix_se_media_image_7 on se_media (image_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_media_8 foreign key (media_id) references se_media (id) on delete restrict on update restrict;
create index ix_se_media_tag_relation_media_8 on se_media_tag_relation (media_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_tag_9 foreign key (tag_id) references se_tag (id) on delete restrict on update restrict;
create index ix_se_media_tag_relation_tag_9 on se_media_tag_relation (tag_id);
alter table se_media_tag_relation add constraint fk_se_media_tag_relation_creator_10 foreign key (user_id) references se_user (id) on delete restrict on update restrict;
create index ix_se_media_tag_relation_creator_10 on se_media_tag_relation (user_id);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table se_access_token;

drop table se_event;

drop table se_file;

drop table se_image;

drop table se_image_file_relation;

drop table se_image_format;

drop table se_media;

drop table se_media_tag_relation;

drop table se_tag;

drop table se_user;

SET FOREIGN_KEY_CHECKS=1;

