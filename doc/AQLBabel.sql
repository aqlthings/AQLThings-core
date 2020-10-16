-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------

DROP TABLE IF EXISTS aqlbabel_player_lang;
DROP TABLE IF EXISTS aqlbabel_lang;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------

-- Languages
CREATE TABLE aqlbabel_lang (
    lang varchar(32) PRIMARY KEY,
    name varchar(64) UNIQUE,
    alphabet varchar(128) NOT NULL,
    description varchar(256)
);

-- Player languages
CREATE TABLE aqlbabel_player_lang (
    player char(32),
    lang varchar(32),
    player_name varchar(128),
    lvl int(2) NOT NULL,
    comment varchar(256),
    FOREIGN KEY (lang) REFERENCES aqlbabel_lang(lang),
    PRIMARY KEY (player, lang)
);