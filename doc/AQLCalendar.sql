-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS aqlcalendar_worlds;
DROP TABLE IF EXISTS aqlcalendar_months;
DROP TABLE IF EXISTS aqlcalendar_seasons;
DROP TABLE IF EXISTS aqlcalendar_types;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------
CREATE TABLE aqlcalendar_types (
    type varchar(64) PRIMARY KEY,
    day_length int(11) NOT NULL,
    seasons_offset int(11) NOT NULL
);

CREATE TABLE aqlcalendar_seasons (
    type varchar(64),
    id varchar(64),
    i_index int(11) NOT NULL,
    name varchar(64) NOT NULL,
    day_ratio float(12) NOT NULL,
    FOREIGN KEY (type) REFERENCES aqlcalendar_types (type),
    PRIMARY KEY (type, id)
);

CREATE TABLE aqlcalendar_months (
    type varchar(64),
    id varchar(64),
    i_index int(11) NOT NULL,
    name varchar(64) NOT NULL,
    days int(11) NOT NULL,
    FOREIGN KEY (type) REFERENCES aqlcalendar_types (type),
    PRIMARY KEY (type, id)
);

CREATE TABLE aqlcalendar_worlds (
    world varchar(64) PRIMARY KEY,
    type varchar(64) NOT NULL,
    y_year int(11),
    fixed_year int(1) NOT NULL,
    s_season varchar(64),
    fixed_season int(1) NOT NULL,
    m_month varchar(64),
    fixed_month int(1) NOT NULL,
    d_day int(11),
    fixed_day int(1) NOT NULL,
    FOREIGN KEY (type) REFERENCES aqlcalendar_types (type),
    FOREIGN KEY (type, s_season) REFERENCES aqlcalendar_seasons (type, id),
    FOREIGN KEY (type, m_month) REFERENCES aqlcalendar_months (type, id)
);

-- ----------------------------------------------------------------------------------
-- INSERT
-- ----------------------------------------------------------------------------------
INSERT INTO aqlcalendar_types VALUES
    ('default', 144000, 80);

INSERT INTO aqlcalendar_seasons VALUES
    ('default', 'spring', 1, 'Spring', 0.58),
    ('default', 'summer', 2, 'Summer', 0.66),
    ('default', 'autumn', 3, 'Autumn', 0.58),
    ('default', 'winter', 0, 'Winter', 0.50);

INSERT INTO aqlcalendar_months VALUES
    ('default', 'january', 0, 'January', 30),
    ('default', 'february', 1, 'February', 30),
    ('default', 'march', 2, 'March', 30),
    ('default', 'april', 3, 'April', 30),
    ('default', 'may', 4, 'May', 30),
    ('default', 'june', 5, 'June', 30),
    ('default', 'july', 6, 'July', 30),
    ('default', 'august', 7, 'August', 30),
    ('default', 'september', 8, 'September', 30),
    ('default', 'october', 9, 'October', 30),
    ('default', 'november', 10, 'November', 30),
    ('default', 'december', 11, 'December', 30);
