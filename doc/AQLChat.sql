-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS aqlchat_bans;
DROP TABLE IF EXISTS aqlchat_players;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------
CREATE TABLE aqlchat_bans (
	uuid varchar(36) NOT NULL,
	channel varchar(150) NOT NULL,
	PRIMARY KEY (uuid, channel)
);

CREATE TABLE aqlchat_players (
	uuid varchar(36) PRIMARY KEY,
	channel varchar(150) NOT NULL
);