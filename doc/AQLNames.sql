-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------

DROP TABLE IF EXISTS aqlnames_names;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------

-- Names
CREATE TABLE aqlnames_names (
    player char(32) PRIMARY KEY,
    player_name varchar(64) NOT NULL,
    display_name varchar(64),
    description varchar(256),
    updated timestamp NOT NULL
);