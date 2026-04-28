CREATE TABLE IF NOT EXISTS "TimeTable%s"
(
    Day VARCHAR(512) PRIMARY KEY,
    "3:30 PM" VARCHAR(512),
    "4:15 PM" VARCHAR(512),
    "5:00 PM" VARCHAR(512),
    "5:45 PM" VARCHAR(512),
    "6:30 PM" VARCHAR(512),
    "7:15 PM" VARCHAR(512)
);

INSERT OR IGNORE INTO "TimeTable%s" (Day) VALUES
                                              ('Mon'),
                                              ('Tue'),
                                              ('Wed'),
                                              ('Thu'),
                                              ('Fri'),
                                              ('Sat'),
                                              ('Sun');

