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
    days_per_month int(11) NOT NULL,
    seasons_offset int(11) NOT NULL
);

CREATE TABLE aqlcalendar_seasons (
    type varchar(64),
    id int(11),
    name varchar(64) NOT NULL,
    day_ratio float(12) NOT NULL
    FOREIGN KEY (type) REFERENCES aqlcalendar_types (type),
    PRIMARY KEY (type, id)
);

CREATE TABLE aqlcalendar_months (
    type varchar(64),
    id int(11),
    name varchar(64) NOT NULL,
    FOREIGN KEY (type) REFERENCES aqlcalendar_types (type),
    PRIMARY KEY (type, id)
);

CREATE TABLE aqlcalendar_worlds (
    world varchar(64) PRIMARY KEY,
    type varchar(64),
    fixed_year int(11),
    fixed_season int(11),
    fixed_month int(11),
    fixed_day int(11),
    start_year int(11),
    start_season int(11),
    start_month int(11),
    start_day int(11),
    FOREIGN KEY (type) REFERENCES aqlcalendar_types (type),
    FOREIGN KEY (type, fixed_season) REFERENCES aqlcalendar_seasons (type, id),
    FOREIGN KEY (type, start_season) REFERENCES aqlcalendar_seasons (type, id),
    FOREIGN KEY (type, fixed_month) REFERENCES aqlcalendar_months (type, id),
    FOREIGN KEY (type, start_month) REFERENCES aqlcalendar_months (type, id)
);
