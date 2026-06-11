INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (141, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when } x^4 - 3x^2 + 2x + 1 \\ \text{is divided by } x - 1.$', null),

       (142, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when the polynomial } x^3 + 3x^2 - 12x + 4 \\ \text{is divided by } x - 2.$', null),

       (143, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Show that } (x - 2) \text{ is a factor of } \\ 5x^2 + 15x - 50.$', null),

       (144, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Verify if } 2x - 1 \text{ is a factor of } \\ 6x^3 - x^2 - 5x + 2.$', null),

       (145, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when } x^3 - 3x^2 + 4x - 4 \\ \text{is divided by } x - 2.$', null),

       (146, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{If } (x - 3) \text{ is a factor of the polynomial } \\ x^3 - kx^2 + 2x - 6, \\ \text{find the value of } k.$',
        null),

       (147, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Find the value of } k, \\ \text{if } 3x - 4 \text{ is a factor of } \\ 3x^2 + 2x - k.$', null),

       (148, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Using the factor theorem, show that } (x - 2) \text{ is a factor of } \\ x^3 + x^2 - 4x - 4. \\ \text{Hence factorise it completely.}$',
        null),

       (149, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{What number should be added to } \\ 27x^3 - 54x^2 + 36x - 11 \\ \text{so that it is divisible by } 3x - 2?$',
        null),

       (150, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Factorise completely: } \\ x^3 - 3x^2 - x + 3.$', null),

       (151, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{If } x^3 + ax^2 + bx + 6 \\ \text{has } (x - 2) \text{ as a factor and leaves remainder 3} \\ \text{when divided by } (x - 3), \\ \text{find } a \text{ and } b.$',
        null),

       (152, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{If } (x - 1) \text{ and } (x + 3) \text{ are factors of } \\ x^3 - ax^2 - 13x + b, \\ \text{find } a \text{ and } b.$',
        null),

       (153, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{Without actual division, prove that } \\ x^4 + 2x^3 - 2x^2 + 2x + 3 \\ \text{is divisible by } x^2 + 2x - 3.$',
        null),

       (154, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{Find } p \text{ and } q \\ \text{in } x^3 - px^2 + 14x - q \\ \text{if divisible by } (x - 1) \text{ and } (x - 2).$',
        null),

       (155, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{When } f(x) \text{ is divided by } (x - 1) \text{ and } (x - 2), \\ \text{remainders are } 3 \text{ and } 5. \\ \text{Find remainder when divided by } (x - 1)(x - 2).$',
        null);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct) VALUES

-- Options for Question 141 (Easy)
(141, '$1$', NULL, 1, 1),
(141, '$0$', NULL, 2, 0),
(141, '$2$', NULL, 3, 0),
(141, '$-1$', NULL, 4, 0),

-- Options for Question 142 (Easy)
(142, '$0$', NULL, 1, 1),
(142, '$4$', NULL, 2, 0),
(142, '$-2$', NULL, 3, 0),
(142, '$2$', NULL, 4, 0),

-- Options for Question 143 (Easy)
(143, '$\text{Remainder } = 0 \\ \text{Hence it is a factor.}$', NULL, 1, 1),
(143, '$\text{Remainder } = 5 \\ \text{Not a factor.}$', NULL, 2, 0),
(143, '$\text{Remainder } = -10 \\ \text{Not a factor.}$', NULL, 3, 0),
(143, '$\text{Remainder } = 2 \\ \text{Not a factor.}$', NULL, 4, 0),

-- Options for Question 144 (Easy)
(144, '$\text{Yes} \\ \text{Remainder } = 0$', NULL, 1, 1),
(144, '$\text{No} \\ \text{Remainder } = 1$', NULL, 2, 0),
(144, '$\text{No} \\ \text{Remainder } = -1$', NULL, 3, 0),
(144, '$\text{Yes} \\ \text{Remainder } = 2$', NULL, 4, 0),

-- Options for Question 145 (Easy)
(145, '$0$', NULL, 1, 1),
(145, '$2$', NULL, 2, 0),
(145, '$-4$', NULL, 3, 0),
(145, '$4$', NULL, 4, 0),

-- Options for Question 146 (Medium)
(146, '$k = 3$', NULL, 1, 1),
(146, '$k = -3$', NULL, 2, 0),
(146, '$k = 2$', NULL, 3, 0),
(146, '$k = 0$', NULL, 4, 0),

-- Options for Question 147 (Medium)
(147, '$k = 8$', NULL, 1, 1),
(147, '$k = -8$', NULL, 2, 0),
(147, '$k = 4$', NULL, 3, 0),
(147, '$k = 6$', NULL, 4, 0),

-- Options for Question 148 (Medium)
(148, '$(x - 2)(x + 2)(x + 1)$', NULL, 1, 1),
(148, '$(x + 2)(x - 2)(x - 1)$', NULL, 2, 0),
(148, '$(x - 2)(x - 1)(x + 1)$', NULL, 3, 0),
(148, '$(x + 2)(x + 2)(x - 1)$', NULL, 4, 0),

-- Options for Question 149 (Medium)
(149, '$3$', NULL, 1, 1),
(149, '$-3$', NULL, 2, 0),
(149, '$5$', NULL, 3, 0),
(149, '$-5$', NULL, 4, 0),

-- Options for Question 150 (Medium)
(150, '$(x - 3)(x - 1)(x + 1)$', NULL, 1, 1),
(150, '$(x + 3)(x - 1)(x + 1)$', NULL, 2, 0),
(150, '$(x - 3)(x - 1)^2$', NULL, 3, 0),
(150, '$(x - 3)(x + 1)^2$', NULL, 4, 0),

-- Options for Question 151 (Very Difficult)
(151, '$a = -3, \\ b = -1$', NULL, 1, 1),
(151, '$a = 3, \\ b = 1$', NULL, 2, 0),
(151, '$a = -1, \\ b = -3$', NULL, 3, 0),
(151, '$a = 2, \\ b = -5$', NULL, 4, 0),

-- Options for Question 152 (Very Difficult)
(152, '$a = 3, \\ b = 15$', NULL, 1, 1),
(152, '$a = -3, \\ b = -15$', NULL, 2, 0),
(152, '$a = 15, \\ b = 3$', NULL, 3, 0),
(152, '$a = 2, \\ b = 12$', NULL, 4, 0),

-- Options for Question 153 (Very Difficult)
(153, '$\text{Factors: } (x-1), (x+3) \\ \text{Remainder = 0}$', NULL, 1, 1),
(153, '$\text{Factors: } (x+1), (x-3) \\ \text{Remainder = 0}$', NULL, 2, 0),
(153, '$\text{Remainders: } 1 \text{ and } -1$', NULL, 3, 0),
(153, '$\text{Not factorisable}$', NULL, 4, 0),

-- Options for Question 154 (Very Difficult)
(154, '$p = 7, \\ q = 8$', NULL, 1, 1),
(154, '$p = -7, \\ q = -8$', NULL, 2, 0),
(154, '$p = 8, \\ q = 7$', NULL, 3, 0),
(154, '$p = 6, \\ q = 12$', NULL, 4, 0),

-- Options for Question 155 (Very Difficult)
(155, '$2x + 1$', NULL, 1, 1),
(155, '$2x - 1$', NULL, 2, 0),
(155, '$x + 2$', NULL, 3, 0);