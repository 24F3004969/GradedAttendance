CREATE TABLE IF NOT EXISTS Attendance
(
    ed_no     TEXT,
    date      TEXT,
    check_in  TEXT,
    check_out TEXT,
    homework  TEXT
);


DROP TRIGGER IF EXISTS remove_attendance_after_student_delete;

CREATE TRIGGER remove_attendance_after_student_delete
    AFTER DELETE ON StudentData
    FOR EACH ROW
BEGIN
    DELETE FROM Attendance WHERE ed_no = OLD.ed_no;
END;
