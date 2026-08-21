CREATE TABLE IF NOT EXISTS Attendance
(
    ed_no        TEXT,
    date         TEXT,
    check_in     TEXT,
    check_out    TEXT,
    homework     TEXT,
    topic_taught TEXT,
    UNIQUE (
            ed_no,
            date
        )
);


DROP TRIGGER IF EXISTS remove_attendance_after_student_delete;

CREATE TRIGGER remove_attendance_after_student_delete
    AFTER DELETE ON StudentData
    FOR EACH ROW
BEGIN
    DELETE FROM Attendance WHERE ed_no = OLD.ed_no;
END;

CREATE TABLE IF NOT EXISTS camera_data (
                             id INTEGER PRIMARY KEY,
                             confidence_threshold REAL,
                             required_checks INTEGER
);

INSERT OR IGNORE INTO camera_data (
    id,
    confidence_threshold,
    required_checks
)
VALUES (
           1,
           40,
           5
       );