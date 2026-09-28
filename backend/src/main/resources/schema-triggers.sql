DROP TRIGGER IF EXISTS trg_check_booking_not_null ON booking; //
DROP TRIGGER IF EXISTS trg_check_approval_not_null ON booking_approval // 
DROP TRIGGER IF EXISTS trg_check_club_not_null ON club //
DROP TRIGGER IF EXISTS trg_check_membership_not_null ON club_membership //
DROP TRIGGER IF EXISTS trg_check_user_not_null ON users // 
DROP TRIGGER IF EXISTS trg_check_professor_not_null ON professor //
DROP TRIGGER IF EXISTS trg_check_security_not_null ON security //
DROP TRIGGER IF EXISTS trg_check_student_not_null ON student //
DROP TRIGGER IF EXISTS trg_check_student_council_not_null ON student_council //
DROP TRIGGER IF EXISTS trg_check_floor_manager_not_null ON floor_manager //
DROP TRIGGER IF EXISTS trg_check_room_not_null ON room //

CREATE TRIGGER trg_check_booking_not_null
BEFORE INSERT ON booking
FOR EACH ROW
EXECUTE FUNCTION check_booking_not_null_fields();
//

CREATE TRIGGER trg_check_approval_not_null
BEFORE INSERT ON booking_approval
FOR EACH ROW
EXECUTE FUNCTION check_approval_not_null_fields();
//

CREATE TRIGGER trg_check_club_not_null
BEFORE INSERT ON club
FOR EACH ROW
EXECUTE FUNCTION check_club_not_null_fields();
//

CREATE TRIGGER trg_check_membership_not_null
BEFORE INSERT ON club_membership
FOR EACH ROW
EXECUTE FUNCTION check_membership_not_null_fields();
//

CREATE TRIGGER trg_check_user_not_null
BEFORE INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION check_user_not_null_fields();
//

CREATE TRIGGER trg_check_professor_not_null
BEFORE INSERT ON professor
FOR EACH ROW
EXECUTE FUNCTION check_professor_not_null_fields();
//

CREATE TRIGGER trg_check_security_not_null
BEFORE INSERT ON security
FOR EACH ROW
EXECUTE FUNCTION check_security_not_null_fields();
//

CREATE TRIGGER trg_check_student_not_null
BEFORE INSERT ON student
FOR EACH ROW
EXECUTE FUNCTION check_student_not_null_fields();
//

CREATE TRIGGER trg_check_student_council_not_null
BEFORE INSERT ON student_council
FOR EACH ROW
EXECUTE FUNCTION check_student_council_not_null_fields();
//

CREATE TRIGGER trg_check_floor_manager_not_null
BEFORE INSERT ON floor_manager
FOR EACH ROW
EXECUTE FUNCTION check_floor_manager_not_null_fields();
//

CREATE TRIGGER trg_check_room_not_null
BEFORE INSERT ON room
FOR EACH ROW
EXECUTE FUNCTION check_room_not_null_fields();
//