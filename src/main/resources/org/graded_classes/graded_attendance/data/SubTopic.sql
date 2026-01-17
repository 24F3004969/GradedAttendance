create table if not exists Subtopics
(
    subtopic_id        INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    subject            NOT NULL,
    subtopic_name TEXT NOT NULL,
    topic_id           NOT NULL,
    FOREIGN KEY (topic_id) REFERENCES Topics (topic_id) ON DELETE CASCADE
);
