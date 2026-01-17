create TABLE IF not exists "fee_2025"
(
    ed_no TEXT PRIMARY KEY NOT NULL,
    name  TEXT             NOT NULL,
    class TEXT,
    Jan   TEXT,
    Feb   TEXT,
    Mar   TEXT,
    Apr   TEXT,
    May   TEXT,
    Jun   TEXT,
    Jul   TEXT,
    Aug   TEXT,
    Sep  TEXT,
    Oct   TEXT,
    Nov   TEXT,
    Dec   TEXT,
    FOREIGN KEY (ed_no
        ) REFERENCES StudentData (ed_no) ON DELETE CASCADE
);