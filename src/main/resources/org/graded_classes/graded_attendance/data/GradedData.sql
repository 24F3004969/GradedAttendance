CREATE TABLE if not exists StudentData
(
    ed_no             TEXT PRIMARY KEY
                           NOT NULL,
    name              TEXT NOT NULL,
    class             TEXT,
    date_of_add       TEXT,
    email             TEXT NOT NULL,
    bloodGroup        TEXT,
    guardian_phone    TEXT,
    aadhaar_no        TEXT,
    father_name       TEXT,
    mother_name       TEXT,
    gender            TEXT,
    dob               TEXT,
    address           TEXT,
    father_occ        TEXT,
    mother_occ        TEXT,
    previous_ins_name TEXT,
    reason_leaving    TEXT,
    school_n          TEXT,
    suggestions       TEXT,
    subjects          TEXT,
    telegram_id       TEXT,
    last_payment_date TEXT,
    fee               REAL NOT NULL default (0),
    points            REAL          DEFAULT 0
);


