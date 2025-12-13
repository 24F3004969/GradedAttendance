create table if not exists DailyTopics
(
    date     text,
    class    text,
    subject1 text,
    topic1   text,
    subject2 text,
    topic2   text,
    subject3 text,
    topic3   text,
    subject4 text,
    topic4   text,
    PRIMARY KEY (date, class)
);
