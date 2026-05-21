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
INSERT INTO Questions (question_id, topic_id, user_id, date_of_making, type, level, question_txt, question_img_path)
VALUES (101, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'What is the SI unit of force?', NULL),
       (102, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'A body of mass 2 kg has weight (g = 9.8 m/s^2). What is its weight?',
        NULL),
       (103, 2, 2, '2026-05-10', 'MCQ', 'Easy',
        'A force of 10 N moves an object by 2 m in the direction of force. What is the work done?', NULL),
       (104, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'A machine does 600 J of work in 2 s. What is its power?', NULL),
       (105, 2, 2, '2026-05-10', 'MCQ', 'Medium', 'Find the kinetic energy of a 2 kg body moving at 3 m/s.', NULL),
       (106, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'Specific heat capacity of a substance is defined as:', NULL),
       (107, 2, 2, '2026-05-10', 'MCQ', 'Medium',
        'How much heat is required to raise the temperature of 0.5 kg of water by 10°C? (c = 4200 J/kg°C)', NULL),
       (108, 2, 2, '2026-05-10', 'MCQ', 'Easy',
        'For a concave mirror, when the object is placed at the centre of curvature (C), the image is formed:', NULL),
       (109, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'In which medium is the speed of light maximum?', NULL),
       (110, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'Electric current is defined as:', NULL),
       (111, 2, 2, '2026-05-10', 'MCQ', 'Easy',
        'Three resistors R1, R2 and R3 are connected in series. The equivalent resistance is:', NULL),
       (112, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'Ohm''s law states that:', NULL),
       (113, 2, 2, '2026-05-10', 'MCQ', 'Easy',
        'A 12 V battery is connected across a 3 ohm resistor. What current flows through it?', NULL),
       (114, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'The electric power of an appliance is given by:', NULL),
       (115, 2, 2, '2026-05-10', 'MCQ', 'Easy', 'The commercial unit of electrical energy is:', NULL),
       (116, 2, 2, '2026-05-10', 'MCQ', 'Easy',
        'The magnetic field around a straight current-carrying conductor consists of:', NULL),
       (117, 2, 2, '2026-05-10', 'MCQ', 'Medium', 'The right-hand thumb rule is used to determine:', NULL),
       (118, 2, 2, '2026-05-10', 'MCQ', 'Medium', 'Fleming''s left-hand rule is used to find the direction of:', NULL),
       (119, 2, 2, '2026-05-10', 'MCQ', 'Medium', 'In a step-down transformer:', NULL),
       (120, 2, 2, '2026-05-10', 'MCQ', 'Medium', 'A sound wave has frequency 256 Hz. What is its time period?', NULL);

INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct) VALUES
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

INSERT INTO QuestionOptions (question_id, option_text, option_img_path, option_order, is_correct) VALUES
-- Q101
(101, 'Newton', NULL, 1, 1),
(101, 'Joule', NULL, 2, 0),
(101, 'Watt', NULL, 3, 0),
(101, 'Pascal', NULL, 4, 0),

-- Q102
(102, '19.6 N', NULL, 1, 1),
(102, '9.8 N', NULL, 2, 0),
(102, '4.9 N', NULL, 3, 0),
(102, '2 N', NULL, 4, 0),

-- Q103
(103, '20 J', NULL, 1, 1),
(103, '5 J', NULL, 2, 0),
(103, '10 J', NULL, 3, 0),
(103, '40 J', NULL, 4, 0),

-- Q104
(104, '300 W', NULL, 1, 1),
(104, '1200 W', NULL, 2, 0),
(104, '200 W', NULL, 3, 0),
(104, '600 W', NULL, 4, 0),

-- Q105
(105, '9 J', NULL, 1, 1),
(105, '18 J', NULL, 2, 0),
(105, '6 J', NULL, 3, 0),
(105, '3 J', NULL, 4, 0),

-- Q106
(106, 'Heat required to raise temperature of 1 kg by 1°C', NULL, 1, 1),
(106, 'Heat required to raise temperature of 1 g by 1°C', NULL, 2, 0),
(106, 'Heat produced per second in a conductor', NULL, 3, 0),
(106, 'Heat required to change state without temperature change', NULL, 4, 0),

-- Q107
(107, '21000 J', NULL, 1, 1),
(107, '42000 J', NULL, 2, 0),
(107, '8400 J', NULL, 3, 0),
(107, '2100 J', NULL, 4, 0),

-- Q108
(108, 'At C, same size, inverted', NULL, 1, 1),
(108, 'At F, highly magnified, upright', NULL, 2, 0),
(108, 'Behind the mirror, diminished, upright', NULL, 3, 0),
(108, 'At infinity, highly diminished', NULL, 4, 0),

-- Q109
(109, 'Vacuum', NULL, 1, 1),
(109, 'Water', NULL, 2, 0),
(109, 'Glass', NULL, 3, 0),
(109, 'Air', NULL, 4, 0),

-- Q110
(110, 'Rate of flow of charge', NULL, 1, 1),
(110, 'Force per unit charge', NULL, 2, 0),
(110, 'Work done per unit charge', NULL, 3, 0),
(110, 'Charge per unit resistance', NULL, 4, 0),

-- Q111
(111, 'R1 + R2 + R3', NULL, 1, 1),
(111, 'R1R2R3', NULL, 2, 0),
(111, '1/(1/R1 + 1/R2 + 1/R3)', NULL, 3, 0),
(111, 'R1 - R2 - R3', NULL, 4, 0),

-- Q112
(112, 'Current is directly proportional to voltage at constant temperature', NULL, 1, 1),
(112, 'Voltage is inversely proportional to resistance always', NULL, 2, 0),
(112, 'Resistance is directly proportional to current always', NULL, 3, 0),
(112, 'Power is directly proportional to resistance always', NULL, 4, 0),

-- Q113
(113, '4 A', NULL, 1, 1),
(113, '36 A', NULL, 2, 0),
(113, '0.25 A', NULL, 3, 0),
(113, '9 A', NULL, 4, 0),

-- Q114
(114, 'P = VI', NULL, 1, 1),
(114, 'P = V/I', NULL, 2, 0),
(114, 'P = IR', NULL, 3, 0),
(114, 'P = R/I', NULL, 4, 0),

-- Q115
(115, 'Kilowatt-hour (kWh)', NULL, 1, 1),
(115, 'Joule (J)', NULL, 2, 0),
(115, 'Watt (W)', NULL, 3, 0),
(115, 'Newton (N)', NULL, 4, 0),

-- Q116
(116, 'Concentric circles around the wire', NULL, 1, 1),
(116, 'Straight lines parallel to the wire', NULL, 2, 0),
(116, 'Random curved lines', NULL, 3, 0),
(116, 'Radial lines outward from the wire', NULL, 4, 0),

-- Q117
(117, 'Direction of magnetic field around a current-carrying conductor', NULL, 1, 1),
(117, 'Direction of current in a conductor due to induced emf', NULL, 2, 0),
(117, 'Direction of force on a charge in electric field', NULL, 3, 0),
(117, 'Direction of image formation in mirrors', NULL, 4, 0),

-- Q118
(118, 'Force on a current-carrying conductor in a magnetic field', NULL, 1, 1),
(118, 'Induced current in a coil', NULL, 2, 0),
(118, 'Magnetic field direction in a solenoid', NULL, 3, 0),
(118, 'Potential difference across a resistor', NULL, 4, 0),

-- Q119
(119, 'Number of turns in secondary coil is less than primary coil', NULL, 1, 1),
(119, 'Number of turns in secondary coil is more than primary coil', NULL, 2, 0),
(119, 'Secondary voltage is always greater than primary voltage', NULL, 3, 0),
(119, 'It works only with DC supply', NULL, 4, 0),

-- Q120
(120, '1/256 s (about 0.0039 s)', NULL, 1, 1),
(120, '256 s', NULL, 2, 0),
(120, '1/128 s', NULL, 3, 0),
(120, '128 s', NULL, 4, 0);