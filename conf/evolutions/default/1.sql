# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

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

create table user (
  id                        integer,
  mail                      varchar(20),
  password                  varchar(255),
  first_name                varchar(20),
  last_name                 varchar(20),
  birth_date                timestamp,
  inscription               timestamp,
  constraint uq_user_id unique (id))
;

create sequence event_seq;




# --- !Downs

drop table if exists event cascade;

drop table if exists user cascade;

drop sequence if exists event_seq;

