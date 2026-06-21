INSERT INTO Questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES
    (21, 2, 1, '2026-05-25', 'MCQ', 'Medium',
     '$\text{Find the remainder when the polynomial } 2x^3 - 3x^2 + 4x - 5 \\ \text{is divided by } x - 2.$', NULL),

    (22, 2, 1, '2026-05-25', 'MCQ', 'Medium',
     '$\text{If } (x - 3) \text{ is a factor of the polynomial } x^3 - kx^2 + 2x - 6\text{,} \\ \text{find the value of } k.$',
     NULL),

    (23, 2, 1, '2026-05-25', 'MCQ', 'Medium',
     '$\text{The polynomial } 3x^3 + 2x^2 - 19x + 6 \text{ has } (x - 2) \text{ as one of its factors.} \\ \text{What is the remaining quadratic factor after dividing by } (x - 2)?$',
     NULL),

    (24, 2, 1, '2026-05-25', 'MCQ', 'Hard',
     '$\text{Completely factorize the expression:} \\ x^3 - 7x - 6.$',
     NULL),

    (25, 2, 1, '2026-05-25', 'MCQ', 'Medium',
     '$\text{What number must be subtracted from } 2x^3 - 5x^2 + 5x \\ \text{so that the resulting polynomial is exactly divisible by } 2x - 1?$',
     NULL),

    (26, 2, 1, '2026-05-25', 'MCQ', 'Hard',
     '$\text{If } (x - 1) \text{ and } (x + 2) \text{ are both factors of the polynomial } x^3 + ax^2 + bx - 6\text{,} \\ \text{find the value of } a.$',
     NULL),

    (27, 2, 1, '2026-05-25', 'MCQ', 'Medium',
     '$\text{When the polynomial } 2x^3 - ax^2 + 7x - 2 \text{ is divided by } (x - 1)\text{,} \\ \text{the remainder is } 3\text{. Find the value of } a.$',
     NULL),

    (28, 2, 1, '2026-05-25', 'MCQ', 'Hard',
     '$\text{Find the factors of the polynomial} \\ 2x^3 + 3x^2 - 11x - 6 \text{ completely.}$',
     NULL),

    (29, 2, 1, '2026-05-25', 'MCQ', 'Hard',
     '$\text{The polynomials } ax^3 + 3x^2 - 13 \text{ and } 2x^3 - 5x + a\text{, when divided} \\ \text{by } (x - 2)\text{, leave the same remainder. Find the value of } a.$',
     NULL),

    (30, 2, 1, '2026-05-25', 'MCQ', 'Medium',
     '$\text{If } (2x + 1) \text{ is a factor of the expression } 2x^3 - x^2 - 5x - 2\text{,} \\ \text{which of the following is also a factor?}$',
     NULL);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct) VALUES

-- Question 21
(21, '$7$', NULL, 1, 1),
(21, '$-5$', NULL, 2, 0),
(21, '$3$', NULL, 3, 0),
(21, '$11$', NULL, 4, 0),

-- Question 22
(22, '$1$', NULL, 1, 0),
(22, '$3$', NULL, 2, 1),
(22, '$2$', NULL, 3, 0),
(22, '$-3$', NULL, 4, 0),

-- Question 23
(23, '$3x^2 + 8x - 3$', NULL, 1, 1),
(23, '$3x^2 - 4x - 3$', NULL, 2, 0),
(23, '$3x^2 + 8x + 3$', NULL, 3, 0),
(23, '$3x^2 + 2x - 3$', NULL, 4, 0),

-- Question 24
(24, '$(x - 1)(x + 2)(x + 3)$', NULL, 1, 0),
(24, '$(x + 1)(x - 3)(x + 2)$', NULL, 2, 1),
(24, '$(x + 1)(x + 3)(x - 2)$', NULL, 3, 0),
(24, '$(x - 1)(x - 2)(x - 3)$', NULL, 4, 0),

-- Question 25
(25, '$1$', NULL, 1, 0),
(25, '$1.5$', NULL, 2, 1),
(25, '$2$', NULL, 3, 0),
(25, '$0.5$', NULL, 4, 0),

-- Question 26
(26, '$2$', NULL, 1, 0),
(26, '$4$', NULL, 2, 1),
(26, '$1$', NULL, 3, 0),
(26, '$-2$', NULL, 4, 0),

-- Question 27
(27, '$2$', NULL, 1, 0),
(27, '$4$', NULL, 2, 1),
(27, '$5$', NULL, 3, 0),
(27, '$-4$', NULL, 4, 0),

-- Question 28
(28, '$(x - 2)(2x + 1)(x + 3)$', NULL, 1, 1),
(28, '$(x + 2)(2x - 1)(x - 3)$', NULL, 2, 0),
(28, '$(x - 2)(2x - 1)(x + 3)$', NULL, 3, 0),
(28, '$(x - 3)(2x + 1)(x + 2)$', NULL, 4, 0),

-- Question 29
(29, '$1$', NULL, 1, 1),
(29, '$2$', NULL, 2, 0),
(29, '$-1$', NULL, 3, 0),
(29, '$0$', NULL, 4, 0),

-- Question 30
(30, '$(x - 1)$', NULL, 1, 0),
(30, '$(x - 2)$', NULL, 2, 1),
(30, '$(x + 1)$', NULL, 3, 0),
(30, '$(x + 2)$', NULL, 4, 0);