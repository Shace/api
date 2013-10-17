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

create sequence event_seq;




# --- !Downs

drop table if exists event cascade;

drop sequence if exists event_seq;

