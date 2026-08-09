create table if not exists Questions
(
    question_id    integer not null primary key autoincrement ,
    topic_id       integer not null,
    user_id        integer not null,
    date_of_making text    not null,
    type           text    not null,
    level          text    not null,
    question_txt   text,
    question_img_path   text,
    FOREIGN KEY (topic_id) REFERENCES Topics(topic_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS QuestionOptions
(
    option_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    question_id    INTEGER NOT NULL,
    option_text    TEXT,
    option_img_path TEXT,
    option_order   INTEGER NOT NULL DEFAULT 1,  -- 1,2,3,4...
    is_correct     INTEGER NOT NULL DEFAULT 0,  -- 0/1 for false/true

    FOREIGN KEY (question_id) REFERENCES Questions(question_id) ON DELETE CASCADE
);
create table if not exists ExamScheduler
(
    exam_id    INTEGER
        primary key autoincrement,
    topic_id   INTEGER
        references Topics,
    subject    TEXT not null,
    class      TEXT not null,
    exam_date  TEXT not null,
    start_time TEXT not null,
    end_time   TEXT not null,
    room_no    TEXT
);

-- auto-generated definition
create table if not exists ExamQuestion
(
    exam_id     INTEGER not null
        references ExamScheduler
            on delete cascade,
    question_id INTEGER not null
        references Questions
            on delete cascade,
    primary key (exam_id, question_id)
);
CREATE TABLE IF NOT EXISTS ScoreCard
(
    exam_id      INTEGER REFERENCES ExamScheduler,
    ed_no        TEXT REFERENCES StudentData,
    subject      TEXT,
    topic_name   TEXT,
    marks_obtain INTEGER,
    total_marks  INTEGER,
    remark       TEXT,

    PRIMARY KEY(exam_id, ed_no)
);
