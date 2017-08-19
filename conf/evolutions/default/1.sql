# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table assignment (
  assignment_id             bigserial not null,
  context_id                text,
  resource_link_id          text,
  tool_consumer_instance_gu_id text,
  duration                  bigint,
  constraint pk_assignment primary key (assignment_id))
;

create table problem (
  problem_id                bigserial not null,
  url                       text,
  assignment_assignment_id  bigint,
  constraint pk_problem primary key (problem_id))
;

create table submission (
  submission_id             bigserial not null,
  student_id                text,
  assignment_id             bigint,
  correct                   bigint,
  maxscore                  bigint,
  activity                  text,
  submitted_at              timestamp,
  content                   text,
  problem_problem_id        bigint,
  constraint pk_submission primary key (submission_id))
;

alter table problem add constraint fk_problem_assignment_1 foreign key (assignment_assignment_id) references assignment (assignment_id);
create index ix_problem_assignment_1 on problem (assignment_assignment_id);
alter table submission add constraint fk_submission_problem_2 foreign key (problem_problem_id) references problem (problem_id);
create index ix_submission_problem_2 on submission (problem_problem_id);



# --- !Downs

drop table if exists assignment cascade;

drop table if exists problem cascade;

drop table if exists submission cascade;

