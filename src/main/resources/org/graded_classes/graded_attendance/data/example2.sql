INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (31, 2, 1, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when } x^4 - 3x^2 + 2x + 1 \\ \text{is divided by } x - 1.$', NULL),

       (32, 2, 1, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when the polynomial } x^3 + 3x^2 - 12x + 4 \\ \text{is divided by } x - 2.$', NULL),

       (33, 2, 1, '2026-05-25', 'mcq', 'easy',
        '$\text{Show that } (x - 2) \text{ is a factor of } \\ 5x^2 + 15x - 50.$', NULL),

       (34, 2, 1, '2026-05-25', 'mcq', 'easy',
        '$\text{Verify if } 2x - 1 \text{ is a factor of } \\ 6x^3 - x^2 - 5x + 2.$', NULL),

       (35, 2, 1, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when } x^3 - 3x^2 + 4x - 4 \\ \text{is divided by } x - 2.$', NULL),

       (36, 2, 1, '2026-05-25', 'mcq', 'medium',
        '$\text{If } (x - 3) \text{ is a factor of the polynomial } \\ x^3 - kx^2 + 2x - 6, \\ \text{find the value of } k.$',
        NULL),

       (37, 2, 1, '2026-05-25', 'mcq', 'medium',
        '$\text{Find the value of } k, \\ \text{if } 3x - 4 \text{ is a factor of } \\ 3x^2 + 2x - k.$', NULL),

       (38, 2, 1, '2026-05-25', 'mcq', 'medium',
        '$\text{Using the factor theorem, show that } (x - 2) \text{ is a factor of } \\ x^3 + x^2 - 4x - 4. \\ \text{Hence factorise it completely.}$',
        NULL),

       (39, 2, 1, '2026-05-25', 'mcq', 'medium',
        '$\text{What number should be added to } \\ 27x^3 - 54x^2 + 36x - 11 \\ \text{so that it is divisible by } 3x - 2?$',
        NULL),

       (40, 2, 1, '2026-05-25', 'mcq', 'medium',
        '$\text{Factorise completely: } \\ x^3 - 3x^2 - x + 3.$', NULL),

       (41, 2, 1, '2026-05-25', 'mcq', 'very difficult',
        '$\text{If } x^3 + ax^2 + bx + 6 \\ \text{has } (x - 2) \text{ as a factor and leaves remainder 3} \\ \text{when divided by } (x - 3), \\ \text{find } a \text{ and } b.$',
        NULL),

       (42, 2, 1, '2026-05-25', 'mcq', 'very difficult',
        '$\text{If } (x - 1) \text{ and } (x + 3) \text{ are factors of } \\ x^3 - ax^2 - 13x + b, \\ \text{find } a \text{ and } b.$',
        NULL),

       (43, 2, 1, '2026-05-25', 'mcq', 'very difficult',
        '$\text{Without actual division, prove that } \\ x^4 + 2x^3 - 2x^2 + 2x + 3 \\ \text{is divisible by } x^2 + 2x - 3.$',
        NULL),

       (44, 2, 1, '2026-05-25', 'mcq', 'very difficult',
        '$\text{Find } p \text{ and } q \\ \text{in } x^3 - px^2 + 14x - q \\ \text{if divisible by } (x - 1) \text{ and } (x - 2).$',
        NULL),

       (45, 2, 1, '2026-05-25', 'mcq', 'very difficult',
        '$\text{When } f(x) \text{ is divided by } (x - 1) \text{ and } (x - 2), \\ \text{remainders are } 3 \text{ and } 5. \\ \text{Find remainder when divided by } (x - 1)(x - 2).$',
        NULL);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (31, '$1$', NULL, 1, 1),
       (31, '$0$', NULL, 2, 0),
       (31, '$2$', NULL, 3, 0),
       (31, '$-1$', NULL, 4, 0),

       (32, '$0$', NULL, 1, 1),
       (32, '$4$', NULL, 2, 0),
       (32, '$-2$', NULL, 3, 0),
       (32, '$2$', NULL, 4, 0),

       (33, '$\text{Remainder } = 0 \\ \text{Hence it is a factor.}$', NULL, 1, 1),
       (33, '$\text{Remainder } = 5 \\ \text{Not a factor.}$', NULL, 2, 0),
       (33, '$\text{Remainder } = -10 \\ \text{Not a factor.}$', NULL, 3, 0),
       (33, '$\text{Remainder } = 2 \\ \text{Not a factor.}$', NULL, 4, 0),

       (34, '$\text{Yes} \\ \text{Remainder } = 0$', NULL, 1, 1),
       (34, '$\text{No} \\ \text{Remainder } = 1$', NULL, 2, 0),
       (34, '$\text{No} \\ \text{Remainder } = -1$', NULL, 3, 0),
       (34, '$\text{Yes} \\ \text{Remainder } = 2$', NULL, 4, 0),

       (35, '$0$', NULL, 1, 1),
       (35, '$2$', NULL, 2, 0),
       (35, '$-4$', NULL, 3, 0),
       (35, '$4$', NULL, 4, 0),

       (36, '$k = 3$', NULL, 1, 1),
       (36, '$k = -3$', NULL, 2, 0),
       (36, '$k = 2$', NULL, 3, 0),
       (36, '$k = 0$', NULL, 4, 0),

       (37, '$k = 8$', NULL, 1, 1),
       (37, '$k = -8$', NULL, 2, 0),
       (37, '$k = 4$', NULL, 3, 0),
       (37, '$k = 6$', NULL, 4, 0),

       (38, '$(x - 2)(x + 2)(x + 1)$', NULL, 1, 1),
       (38, '$(x + 2)(x - 2)(x - 1)$', NULL, 2, 0),
       (38, '$(x - 2)(x - 1)(x + 1)$', NULL, 3, 0),
       (38, '$(x + 2)(x + 2)(x - 1)$', NULL, 4, 0),

       (39, '$3$', NULL, 1, 1),
       (39, '$-3$', NULL, 2, 0),
       (39, '$5$', NULL, 3, 0),
       (39, '$-5$', NULL, 4, 0),

       (40, '$(x - 3)(x - 1)(x + 1)$', NULL, 1, 1),
       (40, '$(x + 3)(x - 1)(x + 1)$', NULL, 2, 0),
       (40, '$(x - 3)(x - 1)^2$', NULL, 3, 0),
       (40, '$(x - 3)(x + 1)^2$', NULL, 4, 0),

       (41, '$a = -3, \\ b = -1$', NULL, 1, 1),
       (41, '$a = 3, \\ b = 1$', NULL, 2, 0),
       (41, '$a = -1, \\ b = -3$', NULL, 3, 0),
       (41, '$a = 2, \\ b = -5$', NULL, 4, 0),

       (42, '$a = 3, \\ b = 15$', NULL, 1, 1),
       (42, '$a = -3, \\ b = -15$', NULL, 2, 0),
       (42, '$a = 15, \\ b = 3$', NULL, 3, 0),
       (42, '$a = 2, \\ b = 12$', NULL, 4, 0),

       (43, '$\text{Factors: } (x-1), (x+3) \\ \text{Remainder = 0}$', NULL, 1, 1),
       (43, '$\text{Factors: } (x+1), (x-3) \\ \text{Remainder = 0}$', NULL, 2, 0),
       (43, '$\text{Remainders: } 1 \text{ and } -1$', NULL, 3, 0),
       (43, '$\text{Not factorisable}$', NULL, 4, 0),

       (44, '$p = 7, \\ q = 8$', NULL, 1, 1),
       (44, '$p = -7, \\ q = -8$', NULL, 2, 0),
       (44, '$p = 8, \\ q = 7$', NULL, 3, 0),
       (44, '$p = 6, \\ q = 12$', NULL, 4, 0),

       (45, '$2x + 1$', NULL, 1, 1),
       (45, '$2x - 1$', NULL, 2, 0),
       (45, '$x + 2$', NULL, 3, 0);