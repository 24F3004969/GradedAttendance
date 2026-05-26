INSERT INTO Questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (121, 101, 3, '2026-05-25', 'MCQ', 'Medium',
        '$\text{Find the remainder when the polynomial } 2x^3 - 3x^2 + 4x - 5 \\ \text{is divided by } x - 2.$', NULL),
       (122, 101, 3, '2026-05-25', 'MCQ', 'Medium',
        '$\text{If } (x - 3) \text{ is a factor of the polynomial } x^3 - kx^2 + 2x - 6\text{,} \\ \text{find the value of } k.$',
        NULL),
       (123, 101, 3, '2026-05-25', 'MCQ', 'Medium',
        '$\text{The polynomial } 3x^3 + 2x^2 - 19x + 6 \text{ has } (x - 2) \text{ as one of its factors.} \\ \text{What is the remaining quadratic factor after dividing by } (x - 2)?$',
        NULL),
       (124, 101, 3, '2026-05-25', 'MCQ', 'Hard', '$\text{Completely factorize the expression:} \\ x^3 - 7x - 6.$',
        NULL),
       (125, 101, 3, '2026-05-25', 'MCQ', 'Medium',
        '$\text{What number must be subtracted from } 2x^3 - 5x^2 + 5x \\ \text{so that the resulting polynomial is exactly divisible by } 2x - 1?$',
        NULL),
       (126, 101, 3, '2026-05-25', 'MCQ', 'Hard',
        '$\text{If } (x - 1) \text{ and } (x + 2) \text{ are both factors of the polynomial } x^3 + ax^2 + bx - 6\text{,} \\ \text{find the value of } a.$',
        NULL),
       (127, 101, 3, '2026-05-25', 'MCQ', 'Medium',
        '$\text{When the polynomial } 2x^3 - ax^2 + 7x - 2 \text{ is divided by } (x - 1)\text{,} \\ \text{the remainder is } 3\text{. Find the value of } a.$',
        NULL),
       (128, 101, 3, '2026-05-25', 'MCQ', 'Hard',
        '$\text{Find the factors of the polynomial} \\ 2x^3 + 3x^2 - 11x - 6 \text{ completely.}$', NULL),
       (129, 101, 3, '2026-05-25', 'MCQ', 'Hard',
        '$\text{The polynomials } ax^3 + 3x^2 - 13 \text{ and } 2x^3 - 5x + a\text{, when divided} \\ \text{by } (x - 2)\text{, leave the same remainder. Find the value of } a.$',
        NULL),
       (130, 101, 3, '2026-05-25', 'MCQ', 'Medium',
        '$\text{If } (2x + 1) \text{ is a factor of the expression } 2x^3 - x^2 - 5x - 2\text{,} \\ \text{which of the following is also a factor?}$',
        NULL);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct) VALUES
-- Question 121 Options
(121, '$7$', NULL, 1, 1),
(121, '$-5$', NULL, 2, 0),
(121, '$3$', NULL, 3, 0),
(121, '$11$', NULL, 4, 0),

-- Question 122 Options
(122, '$1$', NULL, 1, 0),
(122, '$3$', NULL, 2, 1),
(122, '$2$', NULL, 3, 0),
(122, '$-3$', NULL, 4, 0),

-- Question 123 Options
(123, '$3x^2 + 8x - 3$', NULL, 1, 1),
(123, '$3x^2 - 4x - 3$', NULL, 2, 0),
(123, '$3x^2 + 8x + 3$', NULL, 3, 0),
(123, '$3x^2 + 2x - 3$', NULL, 4, 0),

-- Question 124 Options
(124, '$(x - 1)(x + 2)(x + 3)$', NULL, 1, 0),
(124, '$(x + 1)(x - 3)(x + 2)$', NULL, 2, 1),
(124, '$(x + 1)(x + 3)(x - 2)$', NULL, 3, 0),
(124, '$(x - 1)(x - 2)(x - 3)$', NULL, 4, 0),

-- Question 125 Options
(125, '$1$', NULL, 1, 0),
(125, '$1.5$', NULL, 2, 1),
(125, '$2$', NULL, 3, 0),
(125, '$0.5$', NULL, 4, 0),

-- Question 126 Options
(126, '$2$', NULL, 1, 0),
(126, '$4$', NULL, 2, 1),
(126, '$1$', NULL, 3, 0),
(126, '$-2$', NULL, 4, 0),

-- Question 127 Options
(127, '$2$', NULL, 1, 0),
(127, '$4$', NULL, 2, 1),
(127, '$5$', NULL, 3, 0),
(127, '$-4$', NULL, 4, 0),

-- Question 128 Options
(128, '$(x - 2)(2x + 1)(x + 3)$', NULL, 1, 1),
(128, '$(x + 2)(2x - 1)(x - 3)$', NULL, 2, 0),
(128, '$(x - 2)(2x - 1)(x + 3)$', NULL, 3, 0),
(128, '$(x - 3)(2x + 1)(x + 2)$', NULL, 4, 0),

-- Question 129 Options
(129, '$1$', NULL, 1, 1),
(129, '$2$', NULL, 2, 0),
(129, '$-1$', NULL, 3, 0),
(129, '$0$', NULL, 4, 0),

-- Question 130 Options
(130, '$(x - 1)$', NULL, 1, 0),
(130, '$(x - 2)$', NULL, 2, 1),
(130, '$(x + 1)$', NULL, 3, 0),
(130, '$(x + 2)$', NULL, 4, 0);