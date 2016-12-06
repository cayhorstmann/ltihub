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

create table submission (
  submission_id             bigint auto_increment not null,
  student_id                bigint,
  canvas_assignment_id      bigint,
  score                     varchar(255),
  problem_problem_id        bigint,
  constraint pk_submission primary key (submission_id))
;

alter table problem add constraint fk_problem_assignment_1 foreign key (assignment_assignment_id) references assignment (assignment_id) on delete restrict on update restrict;
create index ix_problem_assignment_1 on problem (assignment_assignment_id);
alter table submission add constraint fk_submission_problem_2 foreign key (problem_problem_id) references problem (problem_id) on delete restrict on update restrict;
create index ix_submission_problem_2 on submission (problem_problem_id);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table assignment;

drop table problem;

drop table submission;

SET FOREIGN_KEY_CHECKS=1;

