-- ----------------------------------------------------------------------------------
-- DROP
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS aqlmarkers_markers;
DROP TABLE IF EXISTS aqlmarkers_groups;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------
CREATE TABLE aqlmarkers_groups (
    id int(11) AUTO_INCREMENT,
    name varchar(64) NOT NULL,
    label varchar(128),
    PRIMARY KEY (id),
    UNIQUE KEY (name)
);

CREATE TABLE aqlmarkers_markers (
    id int(11) AUTO_INCREMENT,
    name varchar(64) NOT NULL,
    group_id int(11),
    world varchar(256) NOT NULL,
    x int(11) NOT NULL,
    y int(11) NOT NULL,
    z int(11) NOT NULL,
    yaw float(12),
    pitch float(12),
    icon varchar(64) NOT NULL,
    label varchar(128),
    description varchar(1024),
    perm varchar(256),
    PRIMARY KEY (id),
    UNIQUE KEY (name),
    FOREIGN KEY (group_id) REFERENCES aqlmarkers_groups(id)
);