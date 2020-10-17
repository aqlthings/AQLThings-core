-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------

DROP TABLE IF EXISTS aqlbabel_player_lang;
DROP TABLE IF EXISTS aqlbabel_player;
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
CREATE TABLE aqlbabel_player (
    player char(32) PRIMARY KEY,
    player_name varchar(128),
    selected_lang varchar(32),
    FOREIGN KEY (selected_lang) REFERENCES aqlbabel_lang(lang)
);

-- Player languages
CREATE TABLE aqlbabel_player_lang (
    player char(32),
    lang varchar(32),
    lvl int(2) NOT NULL,
    comment varchar(256),
    FOREIGN KEY (lang) REFERENCES aqlbabel_lang(lang),
    FOREIGN KEY (player) REFERENCES aqlbabel_player(player),
    PRIMARY KEY (player, lang)
);