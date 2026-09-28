-- SOMETHING TO NOTE:
-- I WAS HAVING ISSUES KEEPING PL/pgSQL FUNCTIONS AND SQL QUERIES IN THE SAME FILE BECAUSE
-- QUERIES USE ';' AS A SEPARATOR, WHILE PL/pgSQL USE IT SYNTACTICALLY AND '$$' AS SEPARATOR
-- TO COMBAT THIS, IN application.properties I'VE DEFINED A COMMON SEPARATOR, '//', HENCE ITS USE.

-- Drop Foreign Key Constraints (mirrors Hibernate's drop behavior)
ALTER TABLE IF EXISTS booking DROP CONSTRAINT IF EXISTS FKBCN; // -- FK booking club name
ALTER TABLE IF EXISTS booking DROP CONSTRAINT IF EXISTS FKBR; // -- FK booking room
ALTER TABLE IF EXISTS booking DROP CONSTRAINT IF EXISTS FKBSE; // -- FK booking student email
ALTER TABLE IF EXISTS booking_approval DROP CONSTRAINT IF EXISTS FKBAB; // -- FK booking approval
ALTER TABLE IF EXISTS club DROP CONSTRAINT IF EXISTS FKFHE; // -- FK faculty head email
ALTER TABLE IF EXISTS club DROP CONSTRAINT IF EXISTS FKPSE; // -- FK poc student email
ALTER TABLE IF EXISTS club_membership DROP CONSTRAINT IF EXISTS FKCNC; // -- FK club name
ALTER TABLE IF EXISTS club_membership DROP CONSTRAINT IF EXISTS FKCMSE; // -- FK club member student email
ALTER TABLE IF EXISTS floor_manager DROP CONSTRAINT IF EXISTS FKFME; // -- FK floor manager email
ALTER TABLE IF EXISTS professor DROP CONSTRAINT IF EXISTS FKPE; // -- FK prof email
ALTER TABLE IF EXISTS room DROP CONSTRAINT IF EXISTS FKRME; // -- FK room manager email
ALTER TABLE IF EXISTS security DROP CONSTRAINT IF EXISTS FKSEE; // -- FK security email
ALTER TABLE IF EXISTS student DROP CONSTRAINT IF EXISTS FKSE; // -- FK student email
ALTER TABLE IF EXISTS student_council DROP CONSTRAINT IF EXISTS FKSCSE; // -- FK student council to student email


-- Drop Tables (Use CASCADE to handle dependencies automatically)
DROP TABLE IF EXISTS booking_approval CASCADE; //
DROP TABLE IF EXISTS booking CASCADE; //
DROP TABLE IF EXISTS club_membership CASCADE; //
DROP TABLE IF EXISTS club CASCADE; //
DROP TABLE IF EXISTS room CASCADE; //
DROP TABLE IF EXISTS student_council CASCADE; //
DROP TABLE IF EXISTS floor_manager CASCADE; //
DROP TABLE IF EXISTS professor CASCADE; //
DROP TABLE IF EXISTS security CASCADE; //
DROP TABLE IF EXISTS student CASCADE; //
DROP TABLE IF EXISTS users CASCADE; //


-- Create Tables
CREATE TABLE users (
    email VARCHAR(255) NOT NULL,
    phone BIGINT,
    name VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (email)
); //

CREATE TABLE student (
    email VARCHAR(255) NOT NULL,
    regno BIGINT UNIQUE,
    PRIMARY KEY (email),
    CONSTRAINT FKSE FOREIGN KEY (email) REFERENCES users(email)
); //

CREATE TABLE professor (
    email VARCHAR(255) NOT NULL,
    is_cultural BOOLEAN,
    PRIMARY KEY (email),
    CONSTRAINT FKPE FOREIGN KEY (email) REFERENCES users(email)
); //

CREATE TABLE floor_manager (
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (email),
    CONSTRAINT FKFME FOREIGN KEY (email) REFERENCES users(email)
); //

CREATE TABLE security (
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (email),
    CONSTRAINT FKSEE FOREIGN KEY (email) REFERENCES users(email)
); //

CREATE TABLE student_council (
    email VARCHAR(255) NOT NULL,
    position VARCHAR(255),
    -- Note: regno is inherited from student, no need to redefine column here
    PRIMARY KEY (email),
    -- Constraint links StudentCouncil to Student (which links to Users)
    CONSTRAINT FKSCSE FOREIGN KEY (email) REFERENCES student(email)
); //

CREATE TABLE room (
    block VARCHAR(255) NOT NULL,
    room VARCHAR(255) NOT NULL, -- Matches Room.java @Column(name="room")
    manager_email VARCHAR(255),
    PRIMARY KEY (block, room),
    CONSTRAINT FKRME FOREIGN KEY (manager_email) REFERENCES floor_manager(email)
); //

CREATE TABLE club (
    name VARCHAR(255) NOT NULL,
    faculty_head_email VARCHAR(255) UNIQUE,
    poc_student_email VARCHAR(255) UNIQUE,
    PRIMARY KEY (name),
    CONSTRAINT FKFHE FOREIGN KEY (faculty_head_email) REFERENCES professor(email),
    CONSTRAINT FKPSE FOREIGN KEY (poc_student_email) REFERENCES student(email)
); //

CREATE TABLE club_membership (
    club_name VARCHAR(255) NOT NULL,
    stu_email VARCHAR(255) NOT NULL,
    PRIMARY KEY (club_name, stu_email),
    CONSTRAINT FKCNC FOREIGN KEY (club_name) REFERENCES club(name),
    CONSTRAINT FKCMSE FOREIGN KEY (stu_email) REFERENCES student(email)
); //

CREATE TABLE booking (
    start_time TIMESTAMP(6) NOT NULL,
    block VARCHAR(255) NOT NULL,
    room_no VARCHAR(255) NOT NULL, -- Matches Booking.java @Column(name="room_no")
    end_time TIMESTAMP(6) NOT NULL,
    club_name VARCHAR(255),
    overall_status VARCHAR(255) CHECK (overall_status IN ('PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED')),
    purpose VARCHAR(255),
    student_email VARCHAR(255),
    PRIMARY KEY (start_time, block, room_no),
    CONSTRAINT FKBCN FOREIGN KEY (club_name) REFERENCES club(name),
    -- References room table using its composite key (block, room) but matches booking columns (block, room_no)
    CONSTRAINT FKBR FOREIGN KEY (block, room_no) REFERENCES room(block, room),
    CONSTRAINT FKBSE FOREIGN KEY (student_email) REFERENCES student(email)
); //

CREATE TABLE booking_approval (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    approval_time TIMESTAMP(6),
    start_time TIMESTAMP(6),
    approval_status VARCHAR(255) CHECK (approval_status IN ('PENDING','APPROVED','REJECTED')),
    approver_email VARCHAR(255),
    approver_role VARCHAR(255) CHECK (approver_role IN ('FACULTY_HEAD','STUDENT_COUNCIL','CULTURAL_PROF','FLOOR_MANAGER','SECURITY')),
    block VARCHAR(255),
    comments VARCHAR(255),
    room_no VARCHAR(255),
    PRIMARY KEY (id),
    -- Composite Foreign Key referencing booking's composite primary key
    CONSTRAINT FKBAB FOREIGN KEY (start_time, block, room_no) REFERENCES booking(start_time, block, room_no)
); //