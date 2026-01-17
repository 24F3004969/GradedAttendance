-- =========================
-- DUMMY DATA FOR TOPICS
-- =========================

-- Primary: Classes I–V
INSERT INTO Topics (class, subject, topic_name)
VALUES ('I', 'English', 'Alphabet and Phonics'),
       ('I', 'Math', 'Counting 1–100'),
       ('I', 'Hindi', 'Varnmala (स्वर और व्यंजन)'),
       ('I', 'EVS', 'My Family and Home'),
       ('I', 'Computer', 'Parts of a Computer'),

       ('II', 'English', 'Basic Grammar: Nouns & Verbs'),
       ('II', 'Math', 'Addition and Subtraction'),
       ('II', 'Hindi', 'Barakhadi (बरहखड़ी)'),
       ('II', 'EVS', 'Neighbourhood and Community'),
       ('II', 'Computer', 'Input and Output Devices'),

       ('III', 'English', 'Comprehension and Vocabulary'),
       ('III', 'Math', 'Multiplication and Division'),
       ('III', 'Hindi', 'Sandhi and Compound Words'),
       ('III', 'EVS', 'Plants and Animals'),
       ('III', 'Computer', 'Introduction to Word Processing'),

       ('IV', 'English', 'Parts of Speech'),
       ('IV', 'Math', 'Fractions and Decimals'),
       ('IV', 'Hindi', 'Muhavare and Lokokti'),
       ('IV', 'Social Studies', 'Maps and Globes'),
       ('IV', 'Computer', 'Internet Basics'),

       ('V', 'English', 'Tenses and Sentence Structure'),
       ('V', 'Math', 'Geometry: Angles and Shapes'),
       ('V', 'Hindi', 'Vyakaran: Kriya and Kaal'),
       ('V', 'Social Studies', 'Indian States and Capitals'),
       ('V', 'Computer', 'Spreadsheets Basics'),

-- Middle: Classes VI–VIII
       ('VI', 'English', 'Active and Passive Voice'),
       ('VI', 'Math', 'Integers and Rational Numbers'),
       ('VI', 'Physics', 'Motion and Measurement'),
       ('VI', 'Chemistry', 'Separation of Substances'),
       ('VI', 'Biology', 'Plant Tissues'),
       ('VI', 'Social Studies', 'Ancient Civilizations'),
       ('VI', 'Computer', 'Algorithms and Flowcharts'),

       ('VII', 'English', 'Direct and Indirect Speech'),
       ('VII', 'Math', 'Proportion and Percentage'),
       ('VII', 'Physics', 'Heat and Temperature'),
       ('VII', 'Chemistry', 'Acids, Bases and Salts'),
       ('VII', 'Biology', 'Nutrition in Animals and Plants'),
       ('VII', 'Social Studies', 'Medieval India'),
       ('VII', 'Computer', 'Basics of HTML'),

       ('VIII', 'English', 'Figure of Speech'),
       ('VIII', 'Math', 'Linear Equations in One Variable'),
       ('VIII', 'Physics', 'Light and Optics'),
       ('VIII', 'Chemistry', 'Synthetic Fibres and Plastics'),
       ('VIII', 'Biology', 'Microorganisms: Friend and Foe'),
       ('VIII', 'Social Studies', 'The Modern World'),
       ('VIII', 'Computer', 'Introduction to Python'),

-- Secondary: Classes IX–X
       ('IX', 'English', 'Literary Devices'),
       ('IX', 'Math', 'Coordinate Geometry'),
       ('IX', 'Physics', 'Laws of Motion'),
       ('IX', 'Chemistry', 'Structure of Atom'),
       ('IX', 'Biology', 'Cell: Structure and Function'),
       ('IX', 'Social Studies', 'Democracy and Politics'),
       ('IX', 'Economics', 'People as Resource'),
       ('IX', 'Commerce', 'Introduction to Commerce'),

       ('X', 'English', 'Advanced Grammar & Composition'),
       ('X', 'Math', 'Quadratic Equations'),
       ('X', 'Physics', 'Electricity'),
       ('X', 'Chemistry', 'Chemical Reactions and Equations'),
       ('X', 'Biology', 'Heredity and Evolution'),
       ('X', 'Social Studies', 'Nationalism in India'),
       ('X', 'Economics', 'Sectors of the Economy'),
       ('X', 'Commerce', 'Marketing Basics'),
       ('X', 'Account', 'Fundamentals of Accounting'),

-- Senior Secondary: Classes XI–XII
       ('XI', 'English', 'Poetry and Prose Analysis'),
       ('XI', 'Math', 'Sets, Relations and Functions'),
       ('XI', 'Physics', 'Kinematics'),
       ('XI', 'Chemistry', 'Atomic Structure'),
       ('XI', 'Biology', 'Biomolecules'),
       ('XI', 'Economics', 'Microeconomics Basics'),
       ('XI', 'Commerce', 'Business Environment'),
       ('XI', 'Account', 'Journal and Ledger'),
       ('XI', 'Business Studies', 'Principles of Management'),
       ('XI', 'Computer', 'OOPs in Python'),

       ('XII', 'English', 'Advanced Writing Skills'),
       ('XII', 'Math', 'Matrices and Determinants'),
       ('XII', 'Physics', 'Electromagnetic Induction'),
       ('XII', 'Chemistry', 'Coordination Compounds'),
       ('XII', 'Biology', 'Human Reproduction'),
       ('XII', 'Economics', 'Macroeconomics: National Income'),
       ('XII', 'Commerce', 'Financial Markets'),
       ('XII', 'Account', 'Final Accounts and Adjustments'),
       ('XII', 'Business Studies', 'Marketing Management'),
       ('XII', 'Computer', 'Database Management: SQL');


-- =========================
-- DUMMY DATA FOR SUBTOPICS
-- =========================
-- Using subqueries to fetch topic_id by (class, subject, topic_name)
-- This avoids dependency on unknown AUTOINCREMENT values.

-- Class I
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Vowels and Consonants', (SELECT topic_id
                                             FROM Topics
                                             WHERE class = 'I'
                                               AND subject = 'English'
                                               AND topic_name = 'Alphabet and Phonics')),
       ('Math', 'Counting by 2s/5s/10s',
        (SELECT topic_id FROM Topics WHERE class = 'I' AND subject = 'Math' AND topic_name = 'Counting 1–100')),
       ('Hindi', 'स्वर की पहचान', (SELECT topic_id
                                   FROM Topics
                                   WHERE class = 'I'
                                     AND subject = 'Hindi'
                                     AND topic_name = 'Varnmala (स्वर और व्यंजन)')),
       ('EVS', 'Members of a Family',
        (SELECT topic_id FROM Topics WHERE class = 'I' AND subject = 'EVS' AND topic_name = 'My Family and Home')),
       ('Computer', 'Monitor, Keyboard, Mouse', (SELECT topic_id
                                                 FROM Topics
                                                 WHERE class = 'I'
                                                   AND subject = 'Computer'
                                                   AND topic_name = 'Parts of a Computer'));

-- Class II
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Common vs Proper Nouns', (SELECT topic_id
                                              FROM Topics
                                              WHERE class = 'II'
                                                AND subject = 'English'
                                                AND topic_name = 'Basic Grammar: Nouns & Verbs')),
       ('Math', 'Two-digit Addition', (SELECT topic_id
                                       FROM Topics
                                       WHERE class = 'II'
                                         AND subject = 'Math'
                                         AND topic_name = 'Addition and Subtraction')),
       ('Hindi', 'बरहखड़ी अभ्यास',
        (SELECT topic_id FROM Topics WHERE class = 'II' AND subject = 'Hindi' AND topic_name = 'Barakhadi (बरहखड़ी)')),
       ('EVS', 'Community Helpers', (SELECT topic_id
                                     FROM Topics
                                     WHERE class = 'II'
                                       AND subject = 'EVS'
                                       AND topic_name = 'Neighbourhood and Community')),
       ('Computer', 'Input vs Output', (SELECT topic_id
                                        FROM Topics
                                        WHERE class = 'II'
                                          AND subject = 'Computer'
                                          AND topic_name = 'Input and Output Devices'));

-- Class III
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Synonyms and Antonyms', (SELECT topic_id
                                             FROM Topics
                                             WHERE class = 'III'
                                               AND subject = 'English'
                                               AND topic_name = 'Comprehension and Vocabulary')),
       ('Math', 'Multiplication Tables 2–12', (SELECT topic_id
                                               FROM Topics
                                               WHERE class = 'III'
                                                 AND subject = 'Math'
                                                 AND topic_name = 'Multiplication and Division')),
       ('Hindi', 'समास परिचय', (SELECT topic_id
                                FROM Topics
                                WHERE class = 'III'
                                  AND subject = 'Hindi'
                                  AND topic_name = 'Sandhi and Compound Words')),
       ('EVS', 'Parts of a Plant',
        (SELECT topic_id FROM Topics WHERE class = 'III' AND subject = 'EVS' AND topic_name = 'Plants and Animals')),
       ('Computer', 'Saving a Document', (SELECT topic_id
                                          FROM Topics
                                          WHERE class = 'III'
                                            AND subject = 'Computer'
                                            AND topic_name = 'Introduction to Word Processing'));

-- Class IV
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Noun, Pronoun, Adjective',
        (SELECT topic_id FROM Topics WHERE class = 'IV' AND subject = 'English' AND topic_name = 'Parts of Speech')),
       ('Math', 'Equivalent Fractions', (SELECT topic_id
                                         FROM Topics
                                         WHERE class = 'IV'
                                           AND subject = 'Math'
                                           AND topic_name = 'Fractions and Decimals')),
       ('Hindi', 'लोकोक्ति के उदाहरण',
        (SELECT topic_id FROM Topics WHERE class = 'IV' AND subject = 'Hindi' AND topic_name = 'Muhavare and Lokokti')),
       ('Social Studies', 'Reading a Map', (SELECT topic_id
                                            FROM Topics
                                            WHERE class = 'IV'
                                              AND subject = 'Social Studies'
                                              AND topic_name = 'Maps and Globes')),
       ('Computer', 'Web Browser Basics',
        (SELECT topic_id FROM Topics WHERE class = 'IV' AND subject = 'Computer' AND topic_name = 'Internet Basics'));

-- Class V
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Simple vs Continuous Tense', (SELECT topic_id
                                                  FROM Topics
                                                  WHERE class = 'V'
                                                    AND subject = 'English'
                                                    AND topic_name = 'Tenses and Sentence Structure')),
       ('Math', 'Types of Angles', (SELECT topic_id
                                    FROM Topics
                                    WHERE class = 'V'
                                      AND subject = 'Math'
                                      AND topic_name = 'Geometry: Angles and Shapes')),
       ('Hindi', 'काल के प्रकार', (SELECT topic_id
                                   FROM Topics
                                   WHERE class = 'V'
                                     AND subject = 'Hindi'
                                     AND topic_name = 'Vyakaran: Kriya and Kaal')),
       ('Social Studies', 'State Symbols', (SELECT topic_id
                                            FROM Topics
                                            WHERE class = 'V'
                                              AND subject = 'Social Studies'
                                              AND topic_name = 'Indian States and Capitals')),
       ('Computer', 'Cells and Formulas', (SELECT topic_id
                                           FROM Topics
                                           WHERE class = 'V'
                                             AND subject = 'Computer'
                                             AND topic_name = 'Spreadsheets Basics'));

-- Class VI
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Voice Conversion Rules', (SELECT topic_id
                                              FROM Topics
                                              WHERE class = 'VI'
                                                AND subject = 'English'
                                                AND topic_name = 'Active and Passive Voice')),
       ('Math', 'Number Line for Integers', (SELECT topic_id
                                             FROM Topics
                                             WHERE class = 'VI'
                                               AND subject = 'Math'
                                               AND topic_name = 'Integers and Rational Numbers')),
       ('Physics', 'Types of Motion', (SELECT topic_id
                                       FROM Topics
                                       WHERE class = 'VI'
                                         AND subject = 'Physics'
                                         AND topic_name = 'Motion and Measurement')),
       ('Chemistry', 'Filtration and Evaporation', (SELECT topic_id
                                                    FROM Topics
                                                    WHERE class = 'VI'
                                                      AND subject = 'Chemistry'
                                                      AND topic_name = 'Separation of Substances')),
       ('Biology', 'Meristematic vs Permanent Tissues',
        (SELECT topic_id FROM Topics WHERE class = 'VI' AND subject = 'Biology' AND topic_name = 'Plant Tissues')),
       ('Social Studies', 'Mesopotamia and Egypt', (SELECT topic_id
                                                    FROM Topics
                                                    WHERE class = 'VI'
                                                      AND subject = 'Social Studies'
                                                      AND topic_name = 'Ancient Civilizations')),
       ('Computer', 'Flowchart Symbols', (SELECT topic_id
                                          FROM Topics
                                          WHERE class = 'VI'
                                            AND subject = 'Computer'
                                            AND topic_name = 'Algorithms and Flowcharts'));

-- Class VII
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Reported Speech Rules', (SELECT topic_id
                                             FROM Topics
                                             WHERE class = 'VII'
                                               AND subject = 'English'
                                               AND topic_name = 'Direct and Indirect Speech')),
       ('Math', 'Profit and Loss', (SELECT topic_id
                                    FROM Topics
                                    WHERE class = 'VII'
                                      AND subject = 'Math'
                                      AND topic_name = 'Proportion and Percentage')),
       ('Physics', 'Heat Transfer Methods', (SELECT topic_id
                                             FROM Topics
                                             WHERE class = 'VII'
                                               AND subject = 'Physics'
                                               AND topic_name = 'Heat and Temperature')),
       ('Chemistry', 'Indicators and pH', (SELECT topic_id
                                           FROM Topics
                                           WHERE class = 'VII'
                                             AND subject = 'Chemistry'
                                             AND topic_name = 'Acids, Bases and Salts')),
       ('Biology', 'Photosynthesis and Digestion', (SELECT topic_id
                                                    FROM Topics
                                                    WHERE class = 'VII'
                                                      AND subject = 'Biology'
                                                      AND topic_name = 'Nutrition in Animals and Plants')),
       ('Social Studies', 'Delhi Sultanate', (SELECT topic_id
                                              FROM Topics
                                              WHERE class = 'VII'
                                                AND subject = 'Social Studies'
                                                AND topic_name = 'Medieval India')),
       ('Computer', 'Basic HTML Tags',
        (SELECT topic_id FROM Topics WHERE class = 'VII' AND subject = 'Computer' AND topic_name = 'Basics of HTML'));

-- Class VIII
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Metaphor, Simile, Personification',
        (SELECT topic_id FROM Topics WHERE class = 'VIII' AND subject = 'English' AND topic_name = 'Figure of Speech')),
       ('Math', 'Balancing Equations', (SELECT topic_id
                                        FROM Topics
                                        WHERE class = 'VIII'
                                          AND subject = 'Math'
                                          AND topic_name = 'Linear Equations in One Variable')),
       ('Physics', 'Reflection and Refraction',
        (SELECT topic_id FROM Topics WHERE class = 'VIII' AND subject = 'Physics' AND topic_name = 'Light and Optics')),
       ('Chemistry', 'Types of Polymers', (SELECT topic_id
                                           FROM Topics
                                           WHERE class = 'VIII'
                                             AND subject = 'Chemistry'
                                             AND topic_name = 'Synthetic Fibres and Plastics')),
       ('Biology', 'Useful and Harmful Microbes', (SELECT topic_id
                                                   FROM Topics
                                                   WHERE class = 'VIII'
                                                     AND subject = 'Biology'
                                                     AND topic_name = 'Microorganisms: Friend and Foe')),
       ('Social Studies', 'Industrial Revolution', (SELECT topic_id
                                                    FROM Topics
                                                    WHERE class = 'VIII'
                                                      AND subject = 'Social Studies'
                                                      AND topic_name = 'The Modern World')),
       ('Computer', 'Print, Variables, Loops', (SELECT topic_id
                                                FROM Topics
                                                WHERE class = 'VIII'
                                                  AND subject = 'Computer'
                                                  AND topic_name = 'Introduction to Python'));

-- Class IX
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Imagery and Symbolism',
        (SELECT topic_id FROM Topics WHERE class = 'IX' AND subject = 'English' AND topic_name = 'Literary Devices')),
       ('Math', 'Distance Formula',
        (SELECT topic_id FROM Topics WHERE class = 'IX' AND subject = 'Math' AND topic_name = 'Coordinate Geometry')),
       ('Physics', 'Newton’s Laws',
        (SELECT topic_id FROM Topics WHERE class = 'IX' AND subject = 'Physics' AND topic_name = 'Laws of Motion')),
       ('Chemistry', 'Electrons, Protons, Neutrons', (SELECT topic_id
                                                      FROM Topics
                                                      WHERE class = 'IX'
                                                        AND subject = 'Chemistry'
                                                        AND topic_name = 'Structure of Atom')),
       ('Biology', 'Prokaryotic vs Eukaryotic Cells', (SELECT topic_id
                                                       FROM Topics
                                                       WHERE class = 'IX'
                                                         AND subject = 'Biology'
                                                         AND topic_name = 'Cell: Structure and Function')),
       ('Social Studies', 'Constitution and Rights', (SELECT topic_id
                                                      FROM Topics
                                                      WHERE class = 'IX'
                                                        AND subject = 'Social Studies'
                                                        AND topic_name = 'Democracy and Politics')),
       ('Economics', 'Human Capital Formation', (SELECT topic_id
                                                 FROM Topics
                                                 WHERE class = 'IX'
                                                   AND subject = 'Economics'
                                                   AND topic_name = 'People as Resource')),
       ('Commerce', 'Scope of Commerce', (SELECT topic_id
                                          FROM Topics
                                          WHERE class = 'IX'
                                            AND subject = 'Commerce'
                                            AND topic_name = 'Introduction to Commerce'));

-- Class X
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Essay and Letter Writing', (SELECT topic_id
                                                FROM Topics
                                                WHERE class = 'X'
                                                  AND subject = 'English'
                                                  AND topic_name = 'Advanced Grammar & Composition')),
       ('Math', 'Roots of Quadratics',
        (SELECT topic_id FROM Topics WHERE class = 'X' AND subject = 'Math' AND topic_name = 'Quadratic Equations')),
       ('Physics', 'Ohm’s Law',
        (SELECT topic_id FROM Topics WHERE class = 'X' AND subject = 'Physics' AND topic_name = 'Electricity')),
       ('Chemistry', 'Types of Reactions', (SELECT topic_id
                                            FROM Topics
                                            WHERE class = 'X'
                                              AND subject = 'Chemistry'
                                              AND topic_name = 'Chemical Reactions and Equations')),
       ('Biology', 'Mendel’s Laws', (SELECT topic_id
                                     FROM Topics
                                     WHERE class = 'X'
                                       AND subject = 'Biology'
                                       AND topic_name = 'Heredity and Evolution')),
       ('Social Studies', 'Non-Cooperation Movement', (SELECT topic_id
                                                       FROM Topics
                                                       WHERE class = 'X'
                                                         AND subject = 'Social Studies'
                                                         AND topic_name = 'Nationalism in India')),
       ('Economics', 'Primary, Secondary, Tertiary Sectors', (SELECT topic_id
                                                              FROM Topics
                                                              WHERE class = 'X'
                                                                AND subject = 'Economics'
                                                                AND topic_name = 'Sectors of the Economy')),
       ('Commerce', '4Ps of Marketing',
        (SELECT topic_id FROM Topics WHERE class = 'X' AND subject = 'Commerce' AND topic_name = 'Marketing Basics')),
       ('Account', 'Debit and Credit Rules', (SELECT topic_id
                                              FROM Topics
                                              WHERE class = 'X'
                                                AND subject = 'Account'
                                                AND topic_name = 'Fundamentals of Accounting'));

-- Class XI
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Stylistic Devices in Poetry', (SELECT topic_id
                                                   FROM Topics
                                                   WHERE class = 'XI'
                                                     AND subject = 'English'
                                                     AND topic_name = 'Poetry and Prose Analysis')),
       ('Math', 'Domain and Range', (SELECT topic_id
                                     FROM Topics
                                     WHERE class = 'XI'
                                       AND subject = 'Math'
                                       AND topic_name = 'Sets, Relations and Functions')),
       ('Physics', 'Displacement vs Distance',
        (SELECT topic_id FROM Topics WHERE class = 'XI' AND subject = 'Physics' AND topic_name = 'Kinematics')),
       ('Chemistry', 'Quantum Numbers',
        (SELECT topic_id FROM Topics WHERE class = 'XI' AND subject = 'Chemistry' AND topic_name = 'Atomic Structure')),
       ('Biology', 'Carbohydrates and Proteins',
        (SELECT topic_id FROM Topics WHERE class = 'XI' AND subject = 'Biology' AND topic_name = 'Biomolecules')),
       ('Economics', 'Demand and Supply', (SELECT topic_id
                                           FROM Topics
                                           WHERE class = 'XI'
                                             AND subject = 'Economics'
                                             AND topic_name = 'Microeconomics Basics')),
       ('Commerce', 'Types of Business Organisations', (SELECT topic_id
                                                        FROM Topics
                                                        WHERE class = 'XI'
                                                          AND subject = 'Commerce'
                                                          AND topic_name = 'Business Environment')),
       ('Account', 'Posting to Ledger',
        (SELECT topic_id FROM Topics WHERE class = 'XI' AND subject = 'Account' AND topic_name = 'Journal and Ledger')),
       ('Business Studies', 'Management Functions', (SELECT topic_id
                                                     FROM Topics
                                                     WHERE class = 'XI'
                                                       AND subject = 'Business Studies'
                                                       AND topic_name = 'Principles of Management')),
       ('Computer', 'Classes and Objects in Python',
        (SELECT topic_id FROM Topics WHERE class = 'XI' AND subject = 'Computer' AND topic_name = 'OOPs in Python'));

-- Class XII
INSERT INTO Subtopics (subject, subtopic_name, topic_id)
VALUES ('English', 'Report and Article Writing', (SELECT topic_id
                                                  FROM Topics
                                                  WHERE class = 'XII'
                                                    AND subject = 'English'
                                                    AND topic_name = 'Advanced Writing Skills')),
       ('Math', 'Inverse of a Matrix', (SELECT topic_id
                                        FROM Topics
                                        WHERE class = 'XII'
                                          AND subject = 'Math'
                                          AND topic_name = 'Matrices and Determinants')),
       ('Physics', 'Faraday’s Law', (SELECT topic_id
                                     FROM Topics
                                     WHERE class = 'XII'
                                       AND subject = 'Physics'
                                       AND topic_name = 'Electromagnetic Induction')),
       ('Chemistry', 'Werner’s Theory', (SELECT topic_id
                                         FROM Topics
                                         WHERE class = 'XII'
                                           AND subject = 'Chemistry'
                                           AND topic_name = 'Coordination Compounds')),
       ('Biology', 'Gametogenesis', (SELECT topic_id
                                     FROM Topics
                                     WHERE class = 'XII'
                                       AND subject = 'Biology'
                                       AND topic_name = 'Human Reproduction')),
       ('Economics', 'GDP and GNP', (SELECT topic_id
                                     FROM Topics
                                     WHERE class = 'XII'
                                       AND subject = 'Economics'
                                       AND topic_name = 'Macroeconomics: National Income')),
       ('Commerce', 'SEBI and Stock Exchange', (SELECT topic_id
                                                FROM Topics
                                                WHERE class = 'XII'
                                                  AND subject = 'Commerce'
                                                  AND topic_name = 'Financial Markets')),
       ('Account', 'Adjustments in Final Accounts', (SELECT topic_id
                                                     FROM Topics
                                                     WHERE class = 'XII'
                                                       AND subject = 'Account'
                                                       AND topic_name = 'Final Accounts and Adjustments')),
       ('Business Studies', 'Marketing Mix', (SELECT topic_id
                                              FROM Topics
                                              WHERE class = 'XII'
                                                AND subject = 'Business Studies'
                                                AND topic_name = 'Marketing Management')),
       ('Computer', 'SQL Joins and Keys', (SELECT topic_id
                                           FROM Topics
                                           WHERE class = 'XII'
                                             AND subject = 'Computer'
                                             AND topic_name = 'Database Management: SQL'));
