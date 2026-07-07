-- Question 1 (ID: 151)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (151, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{If } x = \sqrt[3]{7 + \sqrt{50}} + \sqrt[3]{7 - \sqrt{50}}\text{, determine the exact value of } x^3 + 3x - 14.$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (151, '$0$', NULL, 1, 1),
       (151, '$1$', NULL, 2, 0),
       (151, '$7$', NULL, 3, 0),
       (151, '$\sqrt{50}$', NULL, 4, 0);

-- Question 2 (ID: 152)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (152, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Simplify the infinite nested radical expression: } \sqrt{6 + \sqrt{6 + \sqrt{6 + \dots}}} \times \sqrt[3]{6 \sqrt[3]{6 \sqrt[3]{6 \dots}}}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (152, '$3\sqrt{6}$', NULL, 1, 1),
       (152, '$6$', NULL, 2, 0),
       (152, '$9$', NULL, 3, 0),
       (152, '$\sqrt{6}$', NULL, 4, 0);

-- Question 3 (ID: 153)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (153, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Find the value of } x \text{ that satisfies the equation: } 2^{2x+1} - 5(2^x) + 2 = 0 \text{ given } x > 0.$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (153, '$1$', NULL, 1, 1),
       (153, '$-1$', NULL, 2, 0),
       (153, '$0$', NULL, 3, 0),
       (153, '$2$', NULL, 4, 0);

-- Question 4 (ID: 154)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (154, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Evaluate the expression: } \frac{1}{1 + a^{m-n} + a^{m-p}} + \frac{1}{1 + a^{n-m} + a^{n-p}} + \frac{1}{1 + a^{p-m} + a^{p-n}}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (154, '$1$', NULL, 1, 1),
       (154, '$a$', NULL, 2, 0),
       (154, '$a^{m+n+p}$', NULL, 3, 0),
       (154, '$0$', NULL, 4, 0);

-- Question 5 (ID: 155)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (155, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{If } 2^a = 3^b = 6^{-c}\text{, find the value of the reciprocal algebraic sum: } \frac{1}{a} + \frac{1}{b} + \frac{1}{c}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (155, '$0$', NULL, 1, 1),
       (155, '$1$', NULL, 2, 0),
       (155, '$\log_2 3$', NULL, 3, 0),
       (155, '$-1$', NULL, 4, 0);

-- Question 6 (ID: 156)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (156, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Find the real value of } x \text{ for the nested radical index chain equation: } \sqrt{x \sqrt{x \sqrt{x}}} = 8^{\frac{7}{8}}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (156, '$8$', NULL, 1, 1),
       (156, '$2$', NULL, 2, 0),
       (156, '$64$', NULL, 3, 0),
       (156, '$4$', NULL, 4, 0);

-- Question 7 (ID: 157)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (157, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Solve for real values of } x\text{: } (x^2 - 5x + 5)^{x^2 - 9x + 20} = 1$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (157, '$x \in \{1, 2, 4, 5\}$', NULL, 1, 1),
       (157, '$x \in \{4, 5\}$', NULL, 2, 0),
       (157, '$x \in \{1, 4, 5\}$', NULL, 3, 0),
       (157, '$x \in \{2, 3, 4, 5\}$', NULL, 4, 0);

-- Question 8 (ID: 158)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (158, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Simplify the structural fraction: } \left[ \frac{x^{\frac{1}{2}} + y^{\frac{1}{2}}}{x^{\frac{1}{2}} - y^{\frac{1}{2}}} - \frac{x^{\frac{1}{2}} - y^{\frac{1}{2}}}{x^{\frac{1}{2}} + y^{\frac{1}{2}}} \right] \div \frac{x^{\frac{1}{2}}y^{\frac{1}{2}}}{x - y}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (158, '$4$', NULL, 1, 1),
       (158, '$2$', NULL, 2, 0),
       (158, '$1$', NULL, 3, 0),
       (158, '$\frac{4}{x-y}$', NULL, 4, 0);

-- Question 9 (ID: 159)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (159, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Determine the product of all real roots of the system: } 3^x \cdot 2^y = 18 \text{ and } 3^y \cdot 2^x = 12$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (159, '$2$', NULL, 1, 1),
       (159, '$1$', NULL, 2, 0),
       (159, '$3$', NULL, 3, 0),
       (159, '$6$', NULL, 4, 0);

-- Question 10 (ID: 160)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (160, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{If } \sqrt[x]{2026} = \sqrt[y]{2026 \sqrt[z]{2026}}\text{, establish the precise relationship between variables } x, y, \text{ and } z.$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (160, '$xz = yz + x$', NULL, 1, 1),
       (160, '$xyz = 1$', NULL, 2, 0),
       (160, '$yz = xz + y$', NULL, 3, 0),
       (160, '$z = xy + 1$', NULL, 4, 0);

-- Question 11 (ID: 161)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (161, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Find the simplified numeric index valuation of } \frac{(a+b)^{-1} \cdot (a^{-1} + b^{-1})}{(a^{-1}b^{-1})^{-1}}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (161, '$(ab)^{-2}$', NULL, 1, 1),
       (161, '$1$', NULL, 2, 0),
       (161, '$ab$', NULL, 3, 0),
       (161, '$(ab)^{-1}$', NULL, 4, 0);

-- Question 12 (ID: 162)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (162, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{If } 5^{\left(5^x\right)} = \left(5^5\right)^x\text{, find the exact computational value of the variable } x.$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (162, '$\frac{5}{4}$', NULL, 1, 1),
       (162, '$1$', NULL, 2, 0),
       (162, '$\frac{4}{5}$', NULL, 3, 0),
       (162, '$5$', NULL, 4, 0);

-- Question 13 (ID: 163)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (163, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Simplify the multi-level radical structure: } \sqrt{\frac{x^a}{x^b}} \cdot \sqrt{\frac{x^b}{x^c}} \cdot \sqrt{\frac{x^c}{x^a}}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (163, '$1$', NULL, 1, 1),
       (163, '$0$', NULL, 2, 0),
       (163, '$x^{a+b+c}$', NULL, 3, 0),
       (163, '$x$', NULL, 4, 0);

-- Question 14 (ID: 164)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (164, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Find the valuation parameter } K \text{ if } \left(\frac{x^b}{x^c}\right)^{b+c-a} \cdot \left(\frac{x^c}{x^a}\right)^{c+a-b} \cdot \left(\frac{x^a}{x^b}\right)^{a+b-c} = x^K$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (164, '$0$', NULL, 1, 1),
       (164, '$1$', NULL, 2, 0),
       (164, '$a+b+c$', NULL, 3, 0),
       (164, '$a^2+b^2+c^2$', NULL, 4, 0);

-- Question 15 (ID: 165)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (165, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Solve for all real values of } x \text{ in the nested tower equation: } x^{\left(x^x\right)} = \left(x^x\right)^x$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (165, '$x \in \{1, 2\}$', NULL, 1, 1),
       (165, '$x \in \{1\}$', NULL, 2, 0),
       (165, '$x \in \{2\}$', NULL, 3, 0),
       (165, '$x \in \{1, 4\}$', NULL, 4, 0);

-- Question 16 (ID: 166)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (166, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{If } p = x^{\frac{1}{a}}, q = x^{\frac{1}{b}}, r = x^{\frac{1}{c}} \text{ and } pqr = 1\text{, compute the expression value of: } a + b + c$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (166, '$0$', NULL, 1, 1),
       (166, '$1$', NULL, 2, 0),
       (166, '$x$', NULL, 3, 0),
       (166, '$-1$', NULL, 4, 0);

-- Question 17 (ID: 167)
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (167, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Evaluate the nested exponential root constraint value of } x \text{ if: } \sqrt{x}^{\sqrt{x}^{\sqrt{x}^{\dots}}} = \frac{1}{2}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (167, '$\frac{1}{16}$', NULL, 1, 1),
       (167, '$\frac{1}{4}$', NULL, 2, 0),
       (167, '$\frac{1}{2}$', NULL, 3, 0),
       (167, '$\frac{1}{256}$', NULL, 4, 0);
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (168, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{If } \frac{9^n \times 3^2 \times (3^{-\frac{n}{2}})^{-2} - (27)^n}{3^{3m} \times 2^3} = \frac{1}{27}\text{, solve for the structural factor value } m - n.$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (168, '$1$', NULL, 1, 1),
       (168, '$0$', NULL, 2, 0),
       (168, '$-1$', NULL, 3, 0),
       (168, '$3$', NULL, 4, 0);
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (169, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Find the numeric equivalent valuation of: } \frac{1}{1 + \sqrt{2} + \sqrt{3}} + \frac{1}{1 - \sqrt{2} + \sqrt{3}} + \frac{1}{1 + \sqrt{2} - \sqrt{3}} + \frac{1}{1 - \sqrt{2} - \sqrt{3}}$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (169, '$1$', NULL, 1, 1),
       (169, '$0$', NULL, 2, 0),
       (169, '$\sqrt{2}$', NULL, 3, 0),
       (169, '$2$', NULL, 4, 0);
INSERT INTO questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (170, 7, 1, '2026-07-02', 'mcq', 'hard',
        '$\text{Determine the unique positive real solution } x \text{ to the functional index power balance equation: } x^{\left(x^4\right)} = 4$',
        NULL);
INSERT INTO questionoptions (question_id, option_text, option_img_path, option_order, is_correct)
VALUES (170, '$\sqrt{2}$', NULL, 1, 1),
       (170, '$\sqrt[4]{2}$', NULL, 2, 0),
       (170, '$2$', NULL, 3, 0),
       (170, '$\sqrt[4]{4}$', NULL, 4, 0);