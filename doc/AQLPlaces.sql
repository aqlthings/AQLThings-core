-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS aqlplaces_trigger_values;
DROP TABLE IF EXISTS aqlplaces_triggers;
DROP TABLE IF EXISTS aqlplaces_place_values;
DROP TABLE IF EXISTS aqlplaces_places;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------
CREATE TABLE aqlplaces_places (
	id int(11) PRIMARY KEY AUTO_INCREMENT,
	type varchar(32) NOT NULL,
	name varchar(150) NOT NULL,
	world varchar(32) NOT NULL
);

CREATE TABLE aqlplaces_place_values (
    place_id int(11) NOT NULL,
    fieldname varchar(64) NOT NULL,
    fieldvalue varchar(512) NOT NULL,
    FOREIGN KEY (place_id) REFERENCES aqlplaces_places(id),
    PRIMARY KEY (place_id, fieldname)
);

CREATE TABLE aqlplaces_triggers (
	id int(11) PRIMARY KEY AUTO_INCREMENT,
	place int(11) NOT NULL,
	type varchar(32) NOT NULL,
	name varchar(150) NOT NULL,
	state tinyint(1) NOT NULL,
	FOREIGN KEY (place) REFERENCES aqlplaces_places(id)
);

CREATE TABLE aqlplaces_trigger_values (
    trigger_id int(11) NOT NULL,
    fieldname varchar(64) NOT NULL,
    fieldvalue varchar(512) NOT NULL,
    FOREIGN KEY (trigger_id) REFERENCES aqlplaces_triggers(id),
    PRIMARY KEY (trigger_id, fieldname)
);