# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table assignment (
  assignment_id             bigint auto_increment not null,
  constraint pk_assignment primary key (assignment_id))
;

create table problem (
  problem_id                bigint auto_increment not null,
  url                       varchar(255),
  assignment_assignment_id  bigint,
  constraint pk_problem primary key (problem_id))
;

alter table problem add constraint fk_problem_assignment_1 foreign key (assignment_assignment_id) references assignment (assignment_id) on delete restrict on update restrict;
create index ix_problem_assignment_1 on problem (assignment_assignment_id);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table assignment;

drop table problem;

SET FOREIGN_KEY_CHECKS=1;

