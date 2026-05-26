INSERT INTO Questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (131, 102, 3, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Find the value of } k \text{ for which the quadratic equation } \\ 2x^2 + kx + 3 = 0 \text{ has two equal roots.}$',
        NULL),
       (132, 102, 3, '2026-05-26', 'MCQ', 'Medium',
        '$\text{If } x = 3 \text{ is one root of the quadratic equation } \\ x^2 - 2kx - 6 = 0\text{, find the value of } k.$',
        NULL),
       (133, 102, 3, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Find the discriminant of the quadratic equation } \\ 3\sqrt{3}x^2 + 10x + \sqrt{3} = 0.$', NULL),
       (134, 102, 3, '2026-05-26', 'MCQ', 'Hard',
        '$\text{Solve the following quadratic equation for } x: \\ x^2 - 3\sqrt{5}x + 10 = 0.$', NULL),
       (135, 102, 3, '2026-05-26', 'MCQ', 'Medium',
        '$\text{If the quadratic equation } px^2 - 2\sqrt{5}px + 15 = 0 \\ \text{has two equal real roots, find the value of } p.$',
        NULL),
       (136, 102, 3, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Find the nature of the roots of the quadratic equation } \\ 2x^2 - 4x + 3 = 0.$', NULL),
       (137, 102, 3, '2026-05-26', 'MCQ', 'Hard',
        '$\text{If } -5 \text{ is a root of the quadratic equation } 2x^2 + px - 15 = 0 \\ \text{and the quadratic equation } p(x^2 + x) + k = 0 \text{ has equal roots, find the value of } k.$',
        NULL),
       (138, 102, 3, '2026-05-26', 'MCQ', 'Hard',
        '$\text{Solve the quadratic equation } x^2 - 4ax + 4a^2 - b^2 = 0 \\ \text{for } x.$', NULL),
       (139, 102, 3, '2026-05-26', 'MCQ', 'Hard',
        '$\text{Find the roots of the quadratic equation } \\ 4x^2 - 4a^2x + (a^4 - b^4) = 0.$', NULL),
       (140, 102, 3, '2026-05-26', 'MCQ', 'Medium',
        '$\text{Which of the following values of } k \text{ makes the roots of } \\ x^2 - kx + 9 = 0 \text{ real and distinct?}$',
        NULL);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct) VALUES
-- Question 131 Options
(131, '$\pm 2\sqrt{6}$', NULL, 1, 1),
(131, '$\pm 6$', NULL, 2, 0),
(131, '$\pm 4\sqrt{3}$', NULL, 3, 0),
(131, '$\pm 24$', NULL, 4, 0),

-- Question 132 Options
(132, '$k = 1$', NULL, 1, 0),
(132, '$k = 0.5$', NULL, 2, 1),
(132, '$k = 2$', NULL, 3, 0),
(132, '$k = -0.5$', NULL, 4, 0),

-- Question 133 Options
(133, '$100$', NULL, 1, 0),
(133, '$64$', NULL, 2, 1),
(133, '$36$', NULL, 3, 0),
(133, '$46$', NULL, 4, 0),

-- Question 134 Options
(134, '$x = 2\sqrt{5} \text{ or } x = \sqrt{5}$', NULL, 1, 1),
(134, '$x = -2\sqrt{5} \text{ or } x = -\sqrt{5}$', NULL, 2, 0),
(134, '$x = 3\sqrt{5} \text{ or } x = \sqrt{5}$', NULL, 3, 0),
(134, '$x = 5 \text{ or } x = 2$', NULL, 4, 0),

-- Question 135 Options
(135, '$p = 0$', NULL, 1, 0),
(135, '$p = 3$', NULL, 2, 1),
(135, '$p = 5$', NULL, 3, 0),
(135, '$p = 4$', NULL, 4, 0),

-- Question 136 Options
(136, '$\text{Two distinct real roots}$', NULL, 1, 0),
(136, '$\text{Two equal real roots}$', NULL, 2, 0),
(136, '$\text{No real roots (imaginary roots)}$', NULL, 3, 1),
(136, '$\text{More than two roots}$', NULL, 4, 0),

-- Question 137 Options
(137, '$k = \frac{7}{4}$', NULL, 1, 1),
(137, '$k = \frac{4}{7}$', NULL, 2, 0),
(137, '$k = 7$', NULL, 3, 0),
(137, '$k = \frac{49}{4}$', NULL, 4, 0),

-- Question 138 Options
(138, '$x = 2a + b \text{ or } x = 2a - b$', NULL, 1, 1),
(138, '$x = a + 2b \text{ or } x = a - 2b$', NULL, 2, 0),
(138, '$x = -2a + b \text{ or } x = -2a - b$', NULL, 3, 0),
(138, '$x = 2a \text{ or } x = b$', NULL, 4, 0),

-- Question 139 Options
(139, '$x = \frac{a^2 + b^2}{2} \text{ or } x = \frac{a^2 - b^2}{2}$', NULL, 1, 1),
(139, '$x = a^2 + b^2 \text{ or } x = a^2 - b^2$', NULL, 2, 0),
(139, '$x = \frac{a^2 + b^2}{4} \text{ or } x = \frac{a^2 - b^2}{4}$', NULL, 3, 0),
(139, '$x = \frac{a + b}{2} \text{ or } x = \frac{a - b}{2}$', NULL, 4, 0),

-- Question 140 Options
(140, '$-6 < k < 6$', NULL, 1, 0),
(140, '$k > 6 \text{ or } k < -6$', NULL, 2, 1),
(140, '$k = \pm 6$', NULL, 3, 0),
(140, '$k \geq 6$', NULL, 4, 0);