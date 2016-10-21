# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table Problems (
  id                        bigint auto_increment not null,
  url                       varchar(255),
  constraint pk_Problems primary key (id))
;




# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table Problems;

SET FOREIGN_KEY_CHECKS=1;

