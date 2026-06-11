INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (156, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the value of } a \text{ if } (x - a) \text{ is a factor of } \\ x^3 - ax^2 + x + 2.$', null),

       (157, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{If } (x - 2) \text{ is a factor of } \\ x^3 - kx - 12, \text{ find } k.$', null),

       (158, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Find } k \text{ if } (x + 2) \text{ is a factor of } \\ x^3 - kx^2 - 5x + 6.$', null),

       (159, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{What is } f(2) \text{ if } f(x) = 144 - 16x^2?$', null),

       (160, 101, 3, '2026-05-25', 'mcq', 'easy',
        '$\text{Find the remainder when } \\ 2x^3 + 3x^2 - 9x - 10 \\ \text{is divided by } x + 1.$', null),

       (161, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{The polynomial } x^3 - 2x^2 + ax + 12 \\ \text{leaves remainder 20 when divided by } (x + 1). \\ \text{Find } a.$',
        null),

       (162, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Use the factor theorem to factorise } \\ 2x^3 - x^2 - 13x - 6.$', null),

       (163, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Use the Remainder Theorem to factorise } \\ 6x^3 + 17x^2 + 4x - 12.$', null),

       (164, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Using the Remainder Theorem, factorise } \\ 3x^3 + 10x^2 + x - 6.$', null),

       (165, 101, 3, '2026-05-25', 'mcq', 'medium',
        '$\text{Find } k \text{ if } \\ ax^3 + 9x^2 + 4x - 10 \\ \text{divided by } x + 3 \text{ leaves remainder 5.}$',
        null),

       (166, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{Expression } 2x^3 + ax^2 + bx - 2 \\ \text{leaves remainders 7 and 0 when divided by } \\ 2x - 3 \text{ and } x + 2. \\ \text{Find } a \text{ and } b.$',
        null),

       (167, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{When divided by } x - 3, \\ x^3 - px^2 + x + 6 \text{ and } \\ 2x^3 - x^2 - (p + 3)x - 6 \\ \text{leave same remainder. Find } p.$',
        null),

       (168, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{Find } m \text{ and } n \\ \text{such that } x - 1 \text{ and } x + 2 \\ \text{are factors of } x^3 + (3m + 1)x^2 + nx - 18.$',
        null),

       (169, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{What number should be subtracted from } \\ 2x^3 + 5x^2 - 4x + 7 \\ \text{so it is divisible by } y + 3?$',
        null),

       (170, 101, 3, '2026-05-25', 'mcq', 'very difficult',
        '$\text{If } (x - 2) \text{ is a factor of } \\ 2x^3 + ax^2 + bx - 14 \\ \text{and leaves remainder 52 when divided by } (x - 3), \\ \text{find } a \text{ and } b.$',
        null);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (156, '$a = -2$', NULL, 1, 1),
       (156, '$a = 2$', NULL, 2, 0),
       (156, '$a = 0$', NULL, 3, 0),
       (156, '$a = 1$', NULL, 4, 0),

       (157, '$k = -2$', NULL, 1, 1),
       (157, '$k = 2$', NULL, 2, 0),
       (157, '$k = 3$', NULL, 3, 0),
       (157, '$k = -3$', NULL, 4, 0),

       (158, '$k = 3$', NULL, 1, 1),
       (158, '$k = 1$', NULL, 2, 0),
       (158, '$k = 2$', NULL, 3, 0),
       (158, '$k = -2$', NULL, 4, 0),

       (159, '$80$', NULL, 1, 1),
       (159, '$128$', NULL, 2, 0),
       (159, '$0$', NULL, 3, 0),
       (159, '$64$', NULL, 4, 0),

       (160, '$0$', NULL, 1, 1),
       (160, '$-4$', NULL, 2, 0),
       (160, '$2$', NULL, 3, 0),
       (160, '$5$', NULL, 4, 0),

       (161, '$a = -11$', NULL, 1, 1),
       (161, '$a = 11$', NULL, 2, 0),
       (161, '$a = -9$', NULL, 3, 0),
       (161, '$a = 5$', NULL, 4, 0),

       (162, '$(x - 3)(2x + 1)(x + 2)$', NULL, 1, 1),
       (162, '$(x + 3)(2x - 1)(x - 2)$', NULL, 2, 0),
       (162, '$(x - 3)(2x - 1)(x + 2)$', NULL, 3, 0),
       (162, '$(x + 3)(2x + 1)(x - 2)$', NULL, 4, 0),

       (163, '$(x + 2)(2x + 3)(3x - 2)$', NULL, 1, 1),
       (163, '$(x - 2)(2x - 3)(3x + 2)$', NULL, 2, 0),
       (163, '$(x + 2)(2x - 3)(3x - 2)$', NULL, 3, 0),
       (163, '$(x - 2)(2x + 3)(3x + 2)$', NULL, 4, 0),

       (164, '$(x + 1)(x + 3)(3x - 2)$', NULL, 1, 1),
       (164, '$(x - 1)(x - 3)(3x + 2)$', NULL, 2, 0),
       (164, '$(x + 1)(x - 3)(3x - 2)$', NULL, 3, 0),
       (164, '$(x - 1)(x + 3)(3x + 2)$', NULL, 4, 0),

       (165, '$a = 2$', NULL, 1, 1),
       (165, '$a = -2$', NULL, 2, 0),
       (165, '$a = 3$', NULL, 3, 0),
       (165, '$a = 1$', NULL, 4, 0),

       (166, '$a = 3, b = -2$', NULL, 1, 1),
       (166, '$a = -3, b = 2$', NULL, 2, 0),
       (166, '$a = 2, b = -3$', NULL, 3, 0),
       (166, '$a = 1, b = 4$', NULL, 4, 0),

       (167, '$p = 2$', NULL, 1, 1),
       (167, '$p = -2$', NULL, 2, 0),
       (167, '$p = 3$', NULL, 3, 0),
       (167, '$p = 1$', NULL, 4, 0),

       (168, '$m = 2, n = -3$', NULL, 1, 1),
       (168, '$m = -2, n = 3$', NULL, 2, 0),
       (168, '$m = 1, n = -5$', NULL, 3, 0),
       (168, '$m = 3, n = 2$', NULL, 4, 0),

       (169, '$-2$', NULL, 1, 1),
       (169, '$2$', NULL, 2, 0),
       (169, '$-5$', NULL, 3, 0),
       (169, '$7$', NULL, 4, 0),

       (170, '$a = 5, b = -3$', NULL, 1, 1),
       (170, '$a = -5, b = 3$', NULL, 2, 0),
       (170, '$a = 3, b = -5$', NULL, 3, 0),
       (170, '$a = 2, b = 4$', NULL, 4, 0);