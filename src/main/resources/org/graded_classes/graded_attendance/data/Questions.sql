create table if not exists Questions
(
    question_id    integer not null,
    topic_id       integer not null,
    user_id        integer not null,
    date_of_making text    not null,
    type           text    not null,
    level          text    not null,
    question_txt   text,
    question_img_path   text
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
