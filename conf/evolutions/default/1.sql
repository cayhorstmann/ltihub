# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table Assignment (
  assignment_id             bigint auto_increment not null,
  constraint pk_Assignment primary key (assignment_id))
;

create table Problems (
  id                        bigint auto_increment not null,
  url                       varchar(255),
  assignment_id             bigint,
  constraint pk_Problems primary key (id))
;

alter table Problems add constraint fk_Problems_assignment_1 foreign key (assignment_id) references Assignment (assignment_id) on delete restrict on update restrict;
create index ix_Problems_assignment_1 on Problems (assignment_id);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table Assignment;

drop table Problems;

SET FOREIGN_KEY_CHECKS=1;

