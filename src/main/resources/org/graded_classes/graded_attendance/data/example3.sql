INSERT INTO Questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (46, 2, 1, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Find the value of } k \text{ for which the quadratic equation } \\ 2x^2 + kx + 3 = 0 \text{ has two equal roots.}$',
        NULL),

       (47, 2, 1, '2026-05-26', 'MCQ', 'Medium',
        '$\text{If } x = 3 \text{ is one root of the quadratic equation } \\ x^2 - 2kx - 6 = 0\text{, find the value of } k.$',
        NULL),

       (48, 2, 1, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Find the discriminant of the quadratic equation } \\ 3\sqrt{3}x^2 + 10x + \sqrt{3} = 0.$', NULL),

       (49, 2, 1, '2026-05-26', 'MCQ', 'Hard',
        '$\text{Solve the following quadratic equation for } x: \\ x^2 - 3\sqrt{5}x + 10 = 0.$', NULL),

       (50, 2, 1, '2026-05-26', 'MCQ', 'Medium',
        '$\text{If the quadratic equation } px^2 - 2\sqrt{5}px + 15 = 0 \\ \text{has two equal real roots, find the value of } p.$',
        NULL),

       (51, 2, 1, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Find the nature of the roots of the quadratic equation } \\ 2x^2 - 4x + 3 = 0.$', NULL),

       (52, 2, 1, '2026-05-26', 'MCQ', 'Hard',
        '$\text{If } -5 \text{ is a root of the quadratic equation } 2x^2 + px - 15 = 0 \\ \text{and the quadratic equation } p(x^2 + x) + k = 0 \text{ has equal roots, find the value of } k.$',
        NULL),

       (53, 2, 1, '2026-05-26', 'MCQ', 'Hard',
        '$\text{Solve the quadratic equation } x^2 - 4ax + 4a^2 - b^2 = 0 \\ \text{for } x.$', NULL),

       (54, 2, 1, '2026-05-26', 'MCQ', 'Hard',
        '$\text{Find the roots of the quadratic equation } \\ 4x^2 - 4a^2x + (a^4 - b^4) = 0.$', NULL),

       (55, 2, 1, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Which of the following values of } k \text{ makes the roots of } \\ x^2 - kx + 9 = 0 \text{ real and distinct?}$',
        NULL);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (46, '$\pm 2\sqrt{6}$', NULL, 1, 1),
       (46, '$\pm 6$', NULL, 2, 0),
       (46, '$\pm 4\sqrt{3}$', NULL, 3, 0),
       (46, '$\pm 24$', NULL, 4, 0),

       (47, '$k = 1$', NULL, 1, 0),
       (47, '$k = 0.5$', NULL, 2, 1),
       (47, '$k = 2$', NULL, 3, 0),
       (47, '$k = -0.5$', NULL, 4, 0),

       (48, '$100$', NULL, 1, 0),
       (48, '$64$', NULL, 2, 1),
       (48, '$36$', NULL, 3, 0),
       (48, '$46$', NULL, 4, 0),

       (49, '$x = 2\sqrt{5} \text{ or } x = \sqrt{5}$', NULL, 1, 1),
       (49, '$x = -2\sqrt{5} \text{ or } x = -\sqrt{5}$', NULL, 2, 0),
       (49, '$x = 3\sqrt{5} \text{ or } x = \sqrt{5}$', NULL, 3, 0),
       (49, '$x = 5 \text{ or } x = 2$', NULL, 4, 0),

       (50, '$p = 0$', NULL, 1, 0),
       (50, '$p = 3$', NULL, 2, 1),
       (50, '$p = 5$', NULL, 3, 0),
       (50, '$p = 4$', NULL, 4, 0),

       (51, '$\text{Two distinct real roots}$', NULL, 1, 0),
       (51, '$\text{Two equal real roots}$', NULL, 2, 0),
       (51, '$\text{No real roots (imaginary roots)}$', NULL, 3, 1),
       (51, '$\text{More than two roots}$', NULL, 4, 0),

       (52, '$k = \\frac{7}{4}$', NULL, 1, 1),
       (52, '$k = \\frac{4}{7}$', NULL, 2, 0),
       (52, '$k = 7$', NULL, 3, 0),
       (52, '$k = \\frac{49}{4}$', NULL, 4, 0),

       (53, '$x = 2a + b \\text{ or } x = 2a - b$', NULL, 1, 1),
       (53, '$x = a + 2b \\text{ or } x = a - 2b$', NULL, 2, 0),
       (53, '$x = -2a + b \\text{ or } x = -2a - b$', NULL, 3, 0),
       (53, '$x = 2a \\text{ or } x = b$', NULL, 4, 0),

       (54, '$x = \\frac{a^2 + b^2}{2} \\text{ or } x = \\frac{a^2 - b^2}{2}$', NULL, 1, 1),
       (54, '$x = a^2 + b^2 \\text{ or } x = a^2 - b^2$', NULL, 2, 0),
       (54, '$x = \\frac{a^2 + b^2}{4} \\text{ or } x = \\frac{a^2 - b^2}{4}$', NULL, 3, 0),
       (54, '$x = \\frac{a + b}{2} \\text{ or } x = \\frac{a - b}{2}$', NULL, 4, 0),

       (55, '$-6 < k < 6$', NULL, 1, 0),
       (55, '$k > 6 \\text{ or } k < -6$', NULL, 2, 1),
       (55, '$k = \\pm 6$', NULL, 3, 0),
       (55, '$k \\geq 6$', NULL, 4, 0);