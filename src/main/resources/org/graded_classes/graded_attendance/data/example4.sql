INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES
    (56, 2, 1, '2026-05-25', 'mcq', 'easy',
     '$\text{Find the value of } a \text{ if } (x - a) \text{ is a factor of } \\ x^3 - ax^2 + x + 2.$', NULL),

    (57, 2, 1, '2026-05-25', 'mcq', 'easy',
     '$\text{If } (x - 2) \text{ is a factor of } \\ x^3 - kx - 12, \text{ find } k.$', NULL),

    (58, 2, 1, '2026-05-25', 'mcq', 'easy',
     '$\text{Find } k \text{ if } (x + 2) \text{ is a factor of } \\ x^3 - kx^2 - 5x + 6.$', NULL),

    (59, 2, 1, '2026-05-25', 'mcq', 'easy',
     '$\text{What is } f(2) \text{ if } f(x) = 144 - 16x^2?$', NULL),

    (60, 2, 1, '2026-05-25', 'mcq', 'easy',
     '$\text{Find the remainder when } \\ 2x^3 + 3x^2 - 9x - 10 \\ \text{is divided by } x + 1.$', NULL),

    (61, 2, 1, '2026-05-25', 'mcq', 'medium',
     '$\text{The polynomial } x^3 - 2x^2 + ax + 12 \\ \text{leaves remainder 20 when divided by } (x + 1). \\ \text{Find } a.$', NULL),

    (62, 2, 1, '2026-05-25', 'mcq', 'medium',
     '$\text{Use the factor theorem to factorise } \\ 2x^3 - x^2 - 13x - 6.$', NULL),

    (63, 2, 1, '2026-05-25', 'mcq', 'medium',
     '$\text{Use the Remainder Theorem to factorise } \\ 6x^3 + 17x^2 + 4x - 12.$', NULL),

    (64, 2, 1, '2026-05-25', 'mcq', 'medium',
     '$\text{Using the Remainder Theorem, factorise } \\ 3x^3 + 10x^2 + x - 6.$', NULL),

    (65, 2, 1, '2026-05-25', 'mcq', 'medium',
     '$\text{Find } k \text{ if } \\ ax^3 + 9x^2 + 4x - 10 \\ \text{divided by } x + 3 \text{ leaves remainder 5.}$', NULL),

    (66, 2, 1, '2026-05-25', 'mcq', 'very difficult',
     '$\text{Expression } 2x^3 + ax^2 + bx - 2 \\ \text{leaves remainders 7 and 0 when divided by } \\ 2x - 3 \text{ and } x + 2. \\ \text{Find } a \text{ and } b.$', NULL),

    (67, 2, 1, '2026-05-25', 'mcq', 'very difficult',
     '$\text{When divided by } x - 3, \\ x^3 - px^2 + x + 6 \text{ and } \\ 2x^3 - x^2 - (p + 3)x - 6 \\ \text{leave same remainder. Find } p.$', NULL),

    (68, 2, 1, '2026-05-25', 'mcq', 'very difficult',
     '$\text{Find } m \text{ and } n \\ \text{such that } x - 1 \text{ and } x + 2 \\ \text{are factors of } x^3 + (3m + 1)x^2 + nx - 18.$', NULL),

    (69, 2, 1, '2026-05-25', 'mcq', 'very difficult',
     '$\text{What number should be subtracted from } \\ 2x^3 + 5x^2 - 4x + 7 \\ \text{so it is divisible by } y + 3?$', NULL),

    (70, 2, 1, '2026-05-25', 'mcq', 'very difficult',
     '$\text{If } (x - 2) \text{ is a factor of } \\ 2x^3 + ax^2 + bx - 14 \\ \text{and leaves remainder 52 when divided by } (x - 3), \\ \text{find } a \text{ and } b.$', NULL);


INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES
    (56, '$a = -2$', NULL, 1, 1),
    (56, '$a = 2$', NULL, 2, 0),
    (56, '$a = 0$', NULL, 3, 0),
    (56, '$a = 1$', NULL, 4, 0),

    (57, '$k = -2$', NULL, 1, 1),
    (57, '$k = 2$', NULL, 2, 0),
    (57, '$k = 3$', NULL, 3, 0),
    (57, '$k = -3$', NULL, 4, 0),

    (58, '$k = 3$', NULL, 1, 1),
    (58, '$k = 1$', NULL, 2, 0),
    (58, '$k = 2$', NULL, 3, 0),
    (58, '$k = -2$', NULL, 4, 0),

    (59, '$80$', NULL, 1, 1),
    (59, '$128$', NULL, 2, 0),
    (59, '$0$', NULL, 3, 0),
    (59, '$64$', NULL, 4, 0),

    (60, '$0$', NULL, 1, 1),
    (60, '$-4$', NULL, 2, 0),
    (60, '$2$', NULL, 3, 0),
    (60, '$5$', NULL, 4, 0),

    (61, '$a = -11$', NULL, 1, 1),
    (61, '$a = 11$', NULL, 2, 0),
    (61, '$a = -9$', NULL, 3, 0),
    (61, '$a = 5$', NULL, 4, 0),

    (62, '$(x - 3)(2x + 1)(x + 2)$', NULL, 1, 1),
    (62, '$(x + 3)(2x - 1)(x - 2)$', NULL, 2, 0),
    (62, '$(x - 3)(2x - 1)(x + 2)$', NULL, 3, 0),
    (62, '$(x + 3)(2x + 1)(x - 2)$', NULL, 4, 0),

    (63, '$(x + 2)(2x + 3)(3x - 2)$', NULL, 1, 1),
    (63, '$(x - 2)(2x - 3)(3x + 2)$', NULL, 2, 0),
    (63, '$(x + 2)(2x - 3)(3x - 2)$', NULL, 3, 0),
    (63, '$(x - 2)(2x + 3)(3x + 2)$', NULL, 4, 0),

    (64, '$(x + 1)(x + 3)(3x - 2)$', NULL, 1, 1),
    (64, '$(x - 1)(x - 3)(3x + 2)$', NULL, 2, 0),
    (64, '$(x + 1)(x - 3)(3x - 2)$', NULL, 3, 0),
    (64, '$(x - 1)(x + 3)(3x + 2)$', NULL, 4, 0),

    (65, '$a = 2$', NULL, 1, 1),
    (65, '$a = -2$', NULL, 2, 0),
    (65, '$a = 3$', NULL, 3, 0),
    (65, '$a = 1$', NULL, 4, 0),

    (66, '$a = 3, b = -2$', NULL, 1, 1),
    (66, '$a = -3, b = 2$', NULL, 2, 0),
    (66, '$a = 2, b = -3$', NULL, 3, 0),
    (66, '$a = 1, b = 4$', NULL, 4, 0),

    (67, '$p = 2$', NULL, 1, 1),
    (67, '$p = -2$', NULL, 2, 0),
    (67, '$p = 3$', NULL, 3, 0),
    (67, '$p = 1$', NULL, 4, 0),

    (68, '$m = 2, n = -3$', NULL, 1, 1),
    (68, '$m = -2, n = 3$', NULL, 2, 0),
    (68, '$m = 1, n = -5$', NULL, 3, 0),
    (68, '$m = 3, n = 2$', NULL, 4, 0),

    (69, '$-2$', NULL, 1, 1),
    (69, '$2$', NULL, 2, 0),
    (69, '$-5$', NULL, 3, 0),
    (69, '$7$', NULL, 4, 0),

    (70, '$a = 5, b = -3$', NULL, 1, 1),
    (70, '$a = -5, b = 3$', NULL, 2, 0),
    (70, '$a = 3, b = -5$', NULL, 3, 0),
    (70, '$a = 2, b = 4$', NULL, 4, 0);