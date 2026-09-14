
CREATE TABLE IF NOT EXISTS Topics
(
    topic_id INTEGER PRIMARY KEY AUTOINCREMENT,
    class TEXT NOT NULL,
    subject TEXT NOT NULL,
    topic_name TEXT NOT NULL
);
INSERT OR IGNORE INTO Topics (class, subject, topic_name)
VALUES
    ('IV',  'English',   'Diagnosis'),
    ('IV',  'Math',      'Diagnosis'),
    ('IV',  'Physics',   'Diagnosis'),
    ('IV',  'Chemistry', 'Diagnosis'),
    ('IV',  'Biology',   'Diagnosis'),

    ('V',   'English',   'Diagnosis'),
    ('V',   'Math',      'Diagnosis'),
    ('V',   'Physics',   'Diagnosis'),
    ('V',   'Chemistry', 'Diagnosis'),
    ('V',   'Biology',   'Diagnosis'),

    ('VI',  'English',   'Diagnosis'),
    ('VI',  'Math',      'Diagnosis'),
    ('VI',  'Physics',   'Diagnosis'),
    ('VI',  'Chemistry', 'Diagnosis'),
    ('VI',  'Biology',   'Diagnosis'),

    ('VII', 'English',   'Diagnosis'),
    ('VII', 'Math',      'Diagnosis'),
    ('VII', 'Physics',   'Diagnosis'),
    ('VII', 'Chemistry', 'Diagnosis'),
    ('VII', 'Biology',   'Diagnosis'),

    ('VIII','English',   'Diagnosis'),
    ('VIII','Math',      'Diagnosis'),
    ('VIII','Physics',   'Diagnosis'),
    ('VIII','Chemistry', 'Diagnosis'),
    ('VIII','Biology',   'Diagnosis'),

    ('IX',  'English',   'Diagnosis'),
    ('IX',  'Math',      'Diagnosis'),
    ('IX',  'Physics',   'Diagnosis'),
    ('IX',  'Chemistry', 'Diagnosis'),
    ('IX',  'Biology',   'Diagnosis'),

    ('X',   'English',   'Diagnosis'),
    ('X',   'Math',      'Diagnosis'),
    ('X',   'Physics',   'Diagnosis'),
    ('X',   'Chemistry', 'Diagnosis'),
    ('X',   'Biology',   'Diagnosis');