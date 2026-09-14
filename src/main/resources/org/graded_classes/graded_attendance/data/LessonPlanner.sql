create table if not exists LessonPlanner
(
    class    TEXT NOT NULL PRIMARY KEY ,
    subjects TEXT
);
INSERT OR IGNORE INTO LessonPlanner (class, subjects)
VALUES ('I', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Diagnostic Test'),
       ('II', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Diagnostic Test'),
       ('III', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Diagnostic Test'),
       ('IV', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Social Studies,Diagnostic Test'),
       ('V', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Social Studies,Diagnostic Test'),
       ('VI', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Diagnostic Test'),
       ('VII', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Diagnostic Test'),
       ('VIII', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Diagnostic Test'),
       ('IX', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Diagnostic Test'),
       ('X', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Account,Diagnostic Test'),
       ('XI',
        'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Account,Business Studies,Diagnostic Test'),
       ('XII',
        'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Account,Business Studies,Diagnostic Test');