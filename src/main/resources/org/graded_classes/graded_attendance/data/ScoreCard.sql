create table if not exists ScoreCard
(
    exam_id references ExamScheduler,
    ed_no references StudentData,
    subject references Topics,
    topic_name references Topics,
    marks_obtain integer,
    total_marks integer,
    remark text
)