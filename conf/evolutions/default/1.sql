# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table assignment (
  id                            bigserial not null,
  context_id                    varchar(255),
  resource_link_id              varchar(255),
  tool_consumer_id              varchar(255),
  duration                      integer not null,
  constraint pk_assignment primary key (id)
);

create table assignment_work (
  id                            bigserial not null,
  student_id                    varchar(255),
  tool_consumer_id              varchar(255),
  context_id                    varchar(255),
  start_time                    timestamptz,
  problem_group                 integer not null,
  constraint pk_assignment_work primary key (id)
);

create table oauth (
  oauth_consumer_key            varchar(255) not null,
  shared_secret                 varchar(255),
  constraint pk_oauth primary key (oauth_consumer_key)
);

create table problem (
  id                            bigserial not null,
  url                           varchar(255),
  assignment_id                 bigint,
  problem_group                 integer not null,
  weight                        float,
  constraint pk_problem primary key (id)
);

create table problem_work (
  id                            bigserial not null,
  student_id                    varchar(255),
  tool_consumer_id              varchar(255),
  context_id                    varchar(255),
  highest_score                 float not null,
  state                         TEXT,
  client_stamp                  bigint not null,
  problem_id                    bigint,
  last_detail_id                bigint,
  constraint uq_problem_work_last_detail_id unique (last_detail_id),
  constraint pk_problem_work primary key (id)
);

create table submission (
  id                            bigserial not null,
  student_id                    varchar(255),
  tool_consumer_id              varchar(255),
  context_id                    varchar(255),
  problem_id                    bigint,
  score                         float not null,
  submitted_at                  timestamptz,
  script                        TEXT,
  previous_id                   bigint,
  constraint uq_submission_previous_id unique (previous_id),
  constraint pk_submission primary key (id)
);

alter table problem add constraint fk_problem_assignment_id foreign key (assignment_id) references assignment (id) on delete restrict on update restrict;
create index ix_problem_assignment_id on problem (assignment_id);

alter table problem_work add constraint fk_problem_work_problem_id foreign key (problem_id) references problem (id) on delete restrict on update restrict;
create index ix_problem_work_problem_id on problem_work (problem_id);

alter table problem_work add constraint fk_problem_work_last_detail_id foreign key (last_detail_id) references submission (id) on delete restrict on update restrict;

alter table submission add constraint fk_submission_problem_id foreign key (problem_id) references problem (id) on delete restrict on update restrict;
create index ix_submission_problem_id on submission (problem_id);

alter table submission add constraint fk_submission_previous_id foreign key (previous_id) references submission (id) on delete restrict on update restrict;


# --- !Downs

alter table if exists problem drop constraint if exists fk_problem_assignment_id;
drop index if exists ix_problem_assignment_id;

alter table if exists problem_work drop constraint if exists fk_problem_work_problem_id;
drop index if exists ix_problem_work_problem_id;

alter table if exists problem_work drop constraint if exists fk_problem_work_last_detail_id;

alter table if exists submission drop constraint if exists fk_submission_problem_id;
drop index if exists ix_submission_problem_id;

alter table if exists submission drop constraint if exists fk_submission_previous_id;

drop table if exists assignment cascade;

drop table if exists assignment_work cascade;

drop table if exists oauth cascade;

drop table if exists problem cascade;

drop table if exists problem_work cascade;

drop table if exists submission cascade;

