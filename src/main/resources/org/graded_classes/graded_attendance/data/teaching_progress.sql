create table if not exists StudentProgress
(
    ed_no foreign key REFERENCES StudentData,
    topic_id foreign key references Topics,
    start_date TEXT,
    subject    TEXT,
    class      TEXT,
    subtopic_id foreign key references Subtopics,
    level1     INTEGER,
    level2     INTEGER,
    level3     INTEGER,
    end_date   TEXT,
    status     TEXT
);