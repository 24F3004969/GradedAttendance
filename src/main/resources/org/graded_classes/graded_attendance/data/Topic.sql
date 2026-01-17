create table if not exists Topics
(
    topic_id        INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    class           NOT NULL,
    subject         NOT NULL,
    topic_name TEXT NOT NULL
);
