# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table assignment (
  assignment_id             bigserial not null,
  context_id                varchar(255),
  resource_link_id          varchar(255),
  tool_consumer_instance_gu_id varchar(255),
  duration                  bigint,
  constraint pk_assignment primary key (assignment_id))
;

create table problem (
  problem_id                bigserial not null,
  url                       varchar(255),
  assignment_assignment_id  bigint,
  problem_group             integer not null
  constraint pk_problem primary key (problem_id))
;

create table submission (
  submission_id             bigserial not null,
  student_id                varchar(255),
  assignment_id             bigint,
  correct                   bigint,
  maxscore                  bigint,
  activity                  varchar(255),
  submitted_at              timestamp,
  content                   varchar(255),
  previous                  varchar(255),
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

