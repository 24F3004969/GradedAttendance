WITH RankedByLevel AS (
    SELECT
        q.question_id,
        q.topic_id,
        q.user_id,
        q.date_of_making,
        q.type,
        q.level,
        q.question_txt,
        q.question_img_path,
        ROW_NUMBER() OVER (
            PARTITION BY LOWER(q.level)
            ORDER BY RANDOM()
            ) AS level_rank
    FROM Questions AS q
             INNER JOIN Topics AS t
                        ON t.topic_id = q.topic_id
    WHERE t.class = 'X'
      AND t.subject = 'Math'
      AND q.topic_id IN (1, 2, 3)
      AND LOWER(q.level) IN ('easy', 'medium', 'hard')
),

     PreferredQuestions AS (
         SELECT *
         FROM RankedByLevel
         WHERE level_rank <= CASE LOWER(level)
                                 WHEN 'easy'   THEN 5
                                 WHEN 'medium' THEN 8
                                 WHEN 'hard'   THEN 7
                                 ELSE 0
             END
     ),

     FallbackQuestions AS (
         SELECT *
         FROM RankedByLevel
         WHERE question_id NOT IN (
             SELECT question_id
             FROM PreferredQuestions
         )
         ORDER BY RANDOM()
         LIMIT (
             SELECT MAX(0, 20 - COUNT(*))
             FROM PreferredQuestions
         )
     ),

     FinalQuestions AS (
         SELECT
             question_id,
             topic_id,
             user_id,
             date_of_making,
             type,
             level,
             question_txt,
             question_img_path
         FROM PreferredQuestions

         UNION ALL

         SELECT
             question_id,
             topic_id,
             user_id,
             date_of_making,
             type,
             level,
             question_txt,
             question_img_path
         FROM FallbackQuestions
     )

SELECT *
FROM FinalQuestions
ORDER BY RANDOM();
