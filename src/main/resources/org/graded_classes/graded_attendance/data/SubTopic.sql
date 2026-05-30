
CREATE TABLE IF NOT EXISTS Subtopics
(
    subtopic_id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject TEXT NOT NULL,
    subtopic_name TEXT NOT NULL,
    topic_id INTEGER NOT NULL,
    FOREIGN KEY (topic_id)
        REFERENCES Topics (topic_id)
        ON DELETE CASCADE
);
