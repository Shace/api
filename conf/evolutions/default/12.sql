# --- !Ups

create table se_report (
  id                        integer not null,
  creation                  timestamp,
  reason                   	text,
  user_id                   integer,
  image_id                  integer,
  type			            integer,
  constraint pk_se_report   primary key (id),
  constraint ck_se_report_type check (type in (0,1,2,3)))
;

create sequence se_report_seq;

alter table se_report add constraint fk_se_report_creator_24 foreign key (user_id) references se_user (id);
create index ix_se_report_creator_24 on se_report (user_id);

alter table se_report add constraint fk_se_report_image_25 foreign key (image_id) references se_image (id);
create index ix_se_report_image_25 on se_report (image_id);


alter table se_image add column owner_id integer default null;
alter table se_image add constraint fk_se_image_owner_26 foreign key (owner_id) references se_user (id);
create index ix_se_image_owner_26 on se_image (owner_id);
alter table se_image add column hash varchar(36) not null;

# --- !Downs