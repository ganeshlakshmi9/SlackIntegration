DROP DATABASE IF EXISTS users;
CREATE DATABASE users DEFAULT CHARACTER SET utf8;

USE users;

CREATE TABLE user_log (
  id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_name varchar(100) NOT NULL,
  time_stamp varchar(100) NOT NULL,
  info varchar(100) NOT NULL
);