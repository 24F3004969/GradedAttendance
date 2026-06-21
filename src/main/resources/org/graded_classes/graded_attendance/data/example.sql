INSERT INTO Questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (1, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'What is the value of -7 + 12?', NULL),
       (2, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'What is the value of 15 - (-8)?', NULL),
       (3, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'What is the value of (-6) × 4?', NULL),
       (4, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'What is the value of (-48) ÷ 6?', NULL),
       (5, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'What is the absolute value of -35?', NULL),
       (6, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'Which of the following integers is greatest?', NULL),
       (7, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'Simplify: -10 + (-15)', NULL),
       (8, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'Simplify: 27 + (-35)', NULL),
       (9, 1, 1, '2026-05-10', 'MCQ', 'Medium', 'Evaluate: (-5)^2', NULL),
       (10, 1, 1, '2026-05-10', 'MCQ', 'Medium', 'Evaluate: (-2)^4', NULL),
       (11, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'If a = -4 and b = 7, find a + b.', NULL),
       (12, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'Solve for x: x - 9 = -2', NULL),
       (13, 1, 1, '2026-05-10', 'MCQ', 'Medium', 'Find the sign of the product: (-3) × (-2) × 5', NULL),
       (14, 1, 1, '2026-05-10', 'MCQ', 'Medium', 'Evaluate: 3[ -4 + 6 ]', NULL),
       (15, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'Which integer lies between -8 and -3?', NULL),
       (16, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'Simplify: -18 - 22', NULL),
       (17, 1, 1, '2026-05-10', 'MCQ', 'Easy', 'What is (-12) ÷ (-3)?', NULL),
       (18, 1, 1, '2026-05-10', 'MCQ', 'Medium', 'Which expression equals -9?', NULL),
       (19, 1, 1, '2026-05-10', 'MCQ', 'Medium', 'Temperature changes from -5°C to 4°C. What is the increase?', NULL),
       (20, 1, 1, '2026-05-10', 'MCQ', 'Hard', 'Evaluate: (-7) + (-3) - (-5)', NULL);

INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES
-- Q1
(1, '5', NULL, 1, 1),
(1, '-5', NULL, 2, 0),
(1, '19', NULL, 3, 0),
(1, '-19', NULL, 4, 0),

-- Q2
(2, '7', NULL, 1, 0),
(2, '23', NULL, 2, 1),
(2, '-23', NULL, 3, 0),
(2, '8', NULL, 4, 0),

-- Q3
(3, '-24', NULL, 1, 1),
(3, '24', NULL, 2, 0),
(3, '-10', NULL, 3, 0),
(3, '10', NULL, 4, 0),

-- Q4
(4, '-8', NULL, 1, 1),
(4, '8', NULL, 2, 0),
(4, '-6', NULL, 3, 0),
(4, '6', NULL, 4, 0),

-- Q5
(5, '35', NULL, 1, 1),
(5, '-35', NULL, 2, 0),
(5, '0', NULL, 3, 0),
(5, '1', NULL, 4, 0),
-- Q6
(6, '-3', NULL, 1, 0),
(6, '-7', NULL, 2, 0),
(6, '0', NULL, 3, 1),
(6, '-1', NULL, 4, 0),

-- Q7
(7, '-25', NULL, 1, 1),
(7, '25', NULL, 2, 0),
(7, '-5', NULL, 3, 0),
(7, '5', NULL, 4, 0),

-- Q8
(8, '-8', NULL, 1, 1),
(8, '8', NULL, 2, 0),
(8, '-62', NULL, 3, 0),
(8, '62', NULL, 4, 0),

-- Q9
(9, '25', NULL, 1, 1),
(9, '-25', NULL, 2, 0),
(9, '10', NULL, 3, 0),
(9, '-10', NULL, 4, 0),

-- Q10
(10, '16', NULL, 1, 1),
(10, '-16', NULL, 2, 0),
(10, '8', NULL, 3, 0),
(10, '-8', NULL, 4, 0),

-- Q11
(11, '3', NULL, 1, 1),
(11, '-3', NULL, 2, 0),
(11, '11', NULL, 3, 0),
(11, '-11', NULL, 4, 0),

-- Q12
(12, '7', NULL, 1, 1),
(12, '-7', NULL, 2, 0),
(12, '11', NULL, 3, 0),
(12, '-11', NULL, 4, 0),

-- Q13
(13, 'Positive', NULL, 1, 1),
(13, 'Negative', NULL, 2, 0),
(13, 'Zero', NULL, 3, 0),
(13, 'Cannot be determined', NULL, 4, 0),

-- Q14
(14, '6', NULL, 1, 1),
(14, '-6', NULL, 2, 0),
(14, '2', NULL, 3, 0),
(14, '-2', NULL, 4, 0),

-- Q15
(15, '-5', NULL, 1, 1),
(15, '-9', NULL, 2, 0),
(15, '-2', NULL, 3, 0),
(15, '0', NULL, 4, 0),

-- Q16
(16, '-40', NULL, 1, 1),
(16, '40', NULL, 2, 0),
(16, '-4', NULL, 3, 0),
(16, '4', NULL, 4, 0),

-- Q17
(17, '4', NULL, 1, 1),
(17, '-4', NULL, 2, 0),
(17, '9', NULL, 3, 0),
(17, '-9', NULL, 4, 0),

-- Q18
(18, '3 - 12', NULL, 1, 1),
(18, '12 - 3', NULL, 2, 0),
(18, '(-3) × (-3)', NULL, 3, 0),
(18, '(-9) ÷ (-1)', NULL, 4, 0),

-- Q19
(19, '9°C', NULL, 1, 1),
(19, '-9°C', NULL, 2, 0),
(19, '1°C', NULL, 3, 0),
(19, '-1°C', NULL, 4, 0),

-- Q20
(20, '-5', NULL, 1, 1),
(20, '5', NULL, 2, 0),
(20, '-15', NULL, 3, 0),
(20, '15', NULL, 4, 0);
