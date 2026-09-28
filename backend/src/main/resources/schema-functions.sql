CREATE OR REPLACE FUNCTION check_booking_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  -- Check essential fields derived from the composite primary key and relationships
  IF NEW.block IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "block" cannot be NULL.';
  END IF;
  IF NEW.room_no IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "room_no" cannot be NULL.';
  END IF;
  IF NEW.start_time IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "start_time" cannot be NULL.';
  END IF;

  -- Check other logically required fields for a valid initial booking request
  IF NEW.end_time IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "end_time" cannot be NULL.';
  END IF;
  IF NEW.purpose IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "purpose" cannot be NULL.';
  END IF;
  IF NEW.student_email IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "student_email" cannot be NULL.';
  END IF;
  IF NEW.club_name IS NULL THEN
    RAISE EXCEPTION 'Booking cannot be inserted: "club_name" cannot be NULL';
  END IF;
  -- Note: overall_status is usually set by the application logic/default, not checked on initial insert.

  -- If all checks pass, allow the insertion
  RETURN NEW;
END;
$$ LANGUAGE plpgsql; 
//

CREATE OR REPLACE FUNCTION check_approval_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  -- Check Foreign Key components referencing the booking
  IF NEW.block IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "block" cannot be NULL.'; 
  END IF;
  IF NEW.room_no IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "room_no" cannot be NULL.'; 
  END IF;
  IF NEW.start_time IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "start_time" cannot be NULL.'; 
  END IF;

  -- Check essential approval details
  IF NEW.approver_email IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "approver_email" cannot be NULL.'; 
  END IF;
  IF NEW.approver_role IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "approver_role" cannot be NULL.'; 
  END IF;
  IF NEW.approval_status IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "approval_status" cannot be NULL.'; 
  END IF;
  IF NEW.approval_time IS NULL THEN
    RAISE EXCEPTION 'Booking Approval cannot be inserted: "approval_time" cannot be NULL.'; 
  END IF;

  -- Comments are optional, so not checked here.
  -- ID is auto-generated, not checked in BEFORE INSERT.
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
//

CREATE OR REPLACE FUNCTION check_club_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.name IS NULL THEN
    RAISE EXCEPTION 'Club cannot be inserted: "name" cannot be NULL.'; 
  END IF;
  IF NEW.poc_student_email IS NULL THEN
    RAISE EXCEPTION 'Club cannot be inserted: "poc_student_email" cannot be NULL.'; 
  END IF;
  IF NEW.faculty_head_email IS NULL THEN
    RAISE EXCEPTION 'Club cannot be inserted: "faculty_head_email" cannot be NULL.'; 
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
//

CREATE OR REPLACE FUNCTION check_membership_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.club_name IS NULL THEN
    RAISE EXCEPTION 'Club Membership cannot be inserted: "club_name" cannot be NULL.';
  END IF;
  IF NEW.stu_email IS NULL THEN
    RAISE EXCEPTION 'Club Membership cannot be inserted: "stu_email" cannot be NULL.'; 
  END IF;

  RETURN NEW; 
END;
$$ LANGUAGE plpgsql;
//

CREATE OR REPLACE FUNCTION check_user_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.name IS NULL THEN
    RAISE EXCEPTION 'User cannot be inserted: "name" cannot be NULL.';
  END IF;
  IF NEW.email IS NULL THEN
    RAISE EXCEPTION 'User cannot be inserted: "email" cannot be NULL.'; 
  END IF;
  IF NEW.password IS NULL THEN
    RAISE EXCEPTION 'User cannot be inserted: "password" cannot be NULL.'; 
  END IF;
  -- phone nullables shouldn't matter all that much

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
//

CREATE OR REPLACE FUNCTION check_professor_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.email IS NULL THEN
    RAISE EXCEPTION 'Professor cannot be inserted: "email" cannot be NULL.'; 
  END IF;
  IF NEW.is_cultural IS NULL THEN
    RAISE EXCEPTION 'Professor cannot be inserted: "is_cultural" cannot be NULL.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
//

CREATE OR REPLACE FUNCTION check_security_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.email IS NULL THEN
    RAISE EXCEPTION 'Security personnel cannot be inserted: "email" cannot be NULL.'; 
  END IF;

  RETURN NEW; 
END;
$$ LANGUAGE plpgsql; 
//


CREATE OR REPLACE FUNCTION check_student_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.email IS NULL THEN
    RAISE EXCEPTION 'Student cannot be inserted: "email" cannot be NULL.'; 
  END IF;
  IF NEW.regno IS NULL THEN
    RAISE EXCEPTION 'Student cannot be inserted: "regno" cannot be NULL.'; 
  END IF;

  RETURN NEW; 
END;
$$ LANGUAGE plpgsql
//

CREATE OR REPLACE FUNCTION check_student_council_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.email IS NULL THEN
    RAISE EXCEPTION 'Student Council member cannot be inserted: "email" cannot be NULL.';
  END IF;
  IF NEW.position IS NULL THEN
    RAISE EXCEPTION 'Student Council member cannot be inserted: "position" cannot be NULL.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql
//

CREATE OR REPLACE FUNCTION check_floor_manager_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.email IS NULL THEN
    RAISE EXCEPTION 'Floor manager cannot be inserted: "email" cannot be NULL.'; 
  END IF;

  RETURN NEW; 
END;
$$ LANGUAGE plpgsql
//

CREATE OR REPLACE FUNCTION check_room_not_null_fields()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.block IS NULL THEN
    RAISE EXCEPTION 'Room cannot be inserted: "block" cannot be NULL.';
  END IF;
  IF NEW.room IS NULL THEN
    RAISE EXCEPTION 'Room cannot be inserted: "room" (number/name) cannot be NULL.';
  END IF;
  IF NEW.manager_email IS NULL THEN
    RAISE EXCEPTION 'Room cannot be inserted: "manager_email" cannot be NULL.';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql
//