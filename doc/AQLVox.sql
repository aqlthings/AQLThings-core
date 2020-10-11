-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------

DROP TABLE IF EXISTS aqlvox_apikeys;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------

-- API Keys
CREATE TABLE aqlvox_apikeys (
    key_id char(8) PRIMARY KEY,
    key_name varchar(64) NOT NULL,
    user_type varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    api_key char(64) UNIQUE NOT NULL,
    permissions text
);