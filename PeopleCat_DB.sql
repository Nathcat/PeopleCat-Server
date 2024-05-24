CREATE DATABASE `PeopleCat`;
USE `PeopleCat`;

CREATE TABLE `users` (
    UserID INT NOT NULL AUTO_INCREMENT,
    Username varchar(255) NOT NULL,
    Password varchar(255) NOT NULL,
    DisplayName varchar(255),
    DateCreated varchar(255),
    ProfilePicturePath varchar(255),

    PRIMARY KEY (UserID)
);

CREATE TABLE `chats` (
    ChatID INT NOT NULL AUTO_INCREMENT,
    Name varchar(255) NOT NULL,
    KeyID INT,
    JoinCode varchar(255) NOT NULL,

    PRIMARY KEY (ChatID)
);