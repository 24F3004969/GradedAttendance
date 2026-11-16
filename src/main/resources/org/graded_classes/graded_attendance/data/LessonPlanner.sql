create table if not exists LessonPlanner
(
    class    TEXT NOT NULL PRIMARY KEY ,
    subjects TEXT
);
INSERT OR IGNORE INTO LessonPlanner (class, subjects)
VALUES ('I', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Basic Science'),
       ('II', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Basic Science'),
       ('III', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Basic Science'),
       ('IV', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Social Studies,Basic Science'),
       ('V', 'English,Math,Hindi,Computer,EVS,Gk/IQ,Social Studies,Basic Science'),
       ('VI', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Basic Science'),
       ('VII', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Basic Science'),
       ('VIII', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Basic Science'),
       ('IX', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Basic Science'),
       ('X', 'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Account,Basic Science'),
       ('XI',
        'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Account,Business Studies,Basic Science'),
       ('XII',
        'English,Math,Hindi,Computer,Physics,Chemistry,Biology,Gk/IQ,Social Studies,Economics,Commerce,Account,Business Studies,Basic Science');