CREATE TABLE IF NOT EXISTS ExamScheduler
(
    exam_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    topic_id   INTEGER,
    subject    TEXT NOT NULL,
    class      TEXT NOT NULL,
    exam_date  TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time   TEXT NOT NULL,
    room_no    TEXT,

    FOREIGN KEY (topic_id) REFERENCES Topics(topic_id)
);
CREATE TABLE IF NOT EXISTS ExamQuestion
(
    exam_id     INTEGER NOT NULL,
    question_id INTEGER NOT NULL,

    PRIMARY KEY (exam_id, question_id),

    FOREIGN KEY (exam_id) REFERENCES ExamScheduler(exam_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES Questions(question_id) ON DELETE CASCADE
);