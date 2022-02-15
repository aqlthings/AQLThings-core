-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------

DROP TABLE IF EXISTS aqlvox_apikeys;
DROP TABLE IF EXISTS aqlvox_dbusers;

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

-- Database Users
CREATE TABLE aqlvox_dbusers (
    user_id varchar(64) PRIMARY KEY,
    p_uuid char(32),
    passwd VARCHAR(1024),
    pw_salt VARCHAR(1024),
    permissions text
);
