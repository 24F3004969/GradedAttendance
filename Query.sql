SELECT
    tsc.topic_score_id,
    tsc.exam_id,
    tsc.ed_no,
    sd.name,
    sd.class,
    tsc.subject,
    tsc.topic_name,
    tsc.total_questions,
    tsc.attempted_questions,
    tsc.correct_answers,
    tsc.wrong_answers,
    tsc.marks_obtain,
    tsc.total_marks,
    tsc.remark,
    tsc.generated_at
FROM TopicWiseScoreCard tsc
         JOIN StudentData sd
              ON tsc.ed_no = sd.ed_no;