CREATE TABLE if not exists StudentData
(
    ed_no             TEXT PRIMARY KEY
                           NOT NULL,
    name              TEXT NOT NULL,
    class             TEXT,
    date_of_add       TEXT,
    email             TEXT NOT NULL,
    bloodGroup        TEXT,
    guardian_phone    TEXT,
    aadhaar_no        TEXT,
    father_name       TEXT,
    mother_name       TEXT,
    gender            TEXT,
    dob               TEXT,
    address           TEXT,
    father_occ        TEXT,
    mother_occ        TEXT,
    previous_ins_name TEXT,
    reason_leaving    TEXT,
    school_n          TEXT,
    suggestions       TEXT,
    subjects          TEXT,
    telegram_id       TEXT,
    last_payment_date TEXT,
    fee               REAL NOT NULL default (0),
    points            REAL          DEFAULT 0,
    board             TEXT CHECK (board IN ('ICSE', 'CBSE') ) DEFAULT 'ICSE'
);

CREATE TABLE IF NOT EXISTS DiagnosticScoreCard
(
    diagnostic_result_id INTEGER PRIMARY KEY AUTOINCREMENT,
    exam_id               INTEGER NOT NULL,
    student_name          TEXT NOT NULL COLLATE NOCASE,
    subject               TEXT NOT NULL,
    topic_name            TEXT,
    marks_obtain          INTEGER NOT NULL DEFAULT 0,
    total_marks           INTEGER NOT NULL DEFAULT 0,
    total_attempted       INTEGER NOT NULL DEFAULT 0,
    correct_answers       INTEGER NOT NULL DEFAULT 0,
    wrong_answers         INTEGER NOT NULL DEFAULT 0,
    remark                TEXT,
    generated_at          TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (exam_id, student_name, subject)
);
CREATE TABLE IF NOT EXISTS TopicWiseScoreCard (
                                                  topic_score_id INTEGER PRIMARY KEY AUTOINCREMENT,

                                                  exam_id INTEGER NOT NULL,
                                                  ed_no TEXT NOT NULL,
                                                  topic_id INTEGER NOT NULL,

                                                  subject TEXT NOT NULL,
                                                  topic_name TEXT NOT NULL,

                                                  total_questions INTEGER NOT NULL DEFAULT 0,
                                                  attempted_questions INTEGER NOT NULL DEFAULT 0,
                                                  correct_answers INTEGER NOT NULL DEFAULT 0,
                                                  wrong_answers INTEGER NOT NULL DEFAULT 0,

                                                  marks_obtain INTEGER NOT NULL DEFAULT 0,
                                                  total_marks INTEGER NOT NULL DEFAULT 0,

                                                  remark TEXT,
                                                  generated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                  UNIQUE (exam_id, ed_no, topic_id),

                                                  FOREIGN KEY (exam_id)
                                                      REFERENCES ExamScheduler(exam_id)
                                                      ON DELETE CASCADE,

                                                  FOREIGN KEY (ed_no)
                                                      REFERENCES StudentData(ed_no)
                                                      ON DELETE CASCADE,

                                                  FOREIGN KEY (topic_id)
                                                      REFERENCES Topics(topic_id)
                                                      ON DELETE CASCADE
);