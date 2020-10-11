-- ----------------------------------------------------------------------------------
-- CLEAN
-- ----------------------------------------------------------------------------------
DROP TABLE IF EXISTS aqlcharacters_players;
DROP TABLE IF EXISTS aqlcharacters_char_skills;
DROP TABLE IF EXISTS aqlcharacters_char_skill_cat;
DROP TABLE IF EXISTS aqlcharacters_skills;
DROP TABLE IF EXISTS aqlcharacters_skill_cat;
DROP TABLE IF EXISTS aqlcharacters_char_skins;
DROP TABLE IF EXISTS aqlcharacters_char_staff_notes;
DROP TABLE IF EXISTS aqlcharacters_char_edits;
DROP TABLE IF EXISTS aqlcharacters_chars;
DROP TABLE IF EXISTS aqlcharacters_temp_chars;
DROP TABLE IF EXISTS aqlcharacters_common_skins;
DROP TABLE IF EXISTS aqlcharacters_races;
DROP TABLE IF EXISTS aqlcharacters_origins;

-- ----------------------------------------------------------------------------------
-- CREATE
-- ----------------------------------------------------------------------------------

-- Continents d'origines
CREATE TABLE aqlcharacters_origins (
	name varchar(64) PRIMARY KEY,
	wiki varchar(128)
);

-- Races disponibles
CREATE TABLE aqlcharacters_races (
	name varchar(32) PRIMARY KEY,
	category varchar(32) NOT NULL,
	info varchar(128),
	min_height float(11),
	max_height float(11),
	min_weight float(11),
	max_weight float(11),
	def_weight float(11)
);

-- Personnages
CREATE TABLE aqlcharacters_chars (
	charid int(11) PRIMARY KEY AUTO_INCREMENT,
	p_uuid char(32),
	status varchar(32) NOT NULL,
	status_comment varchar(256),
	category varchar(32),
	firstname varchar(64),
	lastname varchar(32),
	sex char(1) NOT NULL,
	race varchar(32) NOT NULL,
	birth varchar(32),
	age int(11),
    height float(11),
    weight float(11),
    luck_points int(11),
	origins varchar(64),
	religion varchar(64),
	occupation varchar(128),
	physical_desc text,
	story text,
	details text,
	FOREIGN KEY (race) REFERENCES aqlcharacters_races(name),
	FOREIGN KEY (origins) REFERENCES aqlcharacters_origins(name)
);

-- Historique des modification de persos
CREATE TABLE aqlcharacters_char_edits (
	charid int(11),
	field varchar(32),
	updated timestamp,
	author int(32) NOT NULL,
	status varchar(32) NOT NULL,
	comment varchar(256),
	diff text,
	FOREIGN KEY (charid) REFERENCES aqlcharacters_chars(charid),
	PRIMARY KEY (charid, field, updated)
);

-- Notes staff sur les persos
CREATE TABLE aqlcharacters_char_staff_notes (
	charid int(11),
	author int(32) NOT NULL,
	created timestamp NOT NULL,
	updated timestamp DEFAULT CURRENT_TIMESTAMP,
	note text,
	FOREIGN KEY (charid) REFERENCES aqlcharacters_chars(charid),
	PRIMARY KEY (charid, created)
);

-- Liste des skins d'un personnage
CREATE TABLE aqlcharacters_char_skins (
	c_charid int(11),
	name varchar(64) NOT NULL,
	file varchar(128) NOT NULL,
	FOREIGN KEY (c_charid) REFERENCES aqlcharacters_chars(charid),
	PRIMARY KEY (c_charid, name)
);

-- Personnages temporaires
CREATE TABLE aqlcharacters_temp_chars (
	p_uuid char(32) PRIMARY KEY,
	name varchar(64),
	sex char(1) NOT NULL,
	race varchar(32) NOT NULL,
    height float(11),
    weight float(11),
    skin varchar(128),
	FOREIGN KEY (race) REFERENCES aqlcharacters_races(name)
);

-- Liste des skins communs
CREATE TABLE aqlcharacters_common_skins (
    skin_id int(11) AUTO_INCREMENT,
	name varchar(64) NOT NULL,
	category varchar(64) NOT NULL,
	file varchar(128) NOT NULL,
	PRIMARY KEY (skin_id)
);

-- Categories de compétences
CREATE TABLE aqlcharacters_skill_cat (
	skill_cat varchar(64),
	label varchar(512),
	required tinyint(1),
    PRIMARY KEY (skill_cat)
);

-- Compétences par categories
CREATE TABLE aqlcharacters_skills (
    shorthand varchar(32) UNIQUE,
	skill_cat varchar(64),
	skill_name varchar(128),
	label varchar(512),
	FOREIGN KEY (skill_cat) REFERENCES aqlcharacters_skill_cat(skill_cat),
    PRIMARY KEY (skill_cat, skill_name)
);
-- Categories de compétence des personnages
CREATE TABLE aqlcharacters_char_skill_cat (
	c_charid int(11),
    skill_cat varchar(64),
    FOREIGN KEY (c_charid) REFERENCES aqlcharacters_chars(charid),
	FOREIGN KEY (skill_cat) REFERENCES aqlcharacters_skill_cat(skill_cat),
	PRIMARY KEY (c_charid, skill_cat)
);

-- Compétences des personnages
CREATE TABLE aqlcharacters_char_skills (
	c_charid int(11),
	skill_cat varchar(64) NOT NULL,
	skill_name varchar(128) NOT NULL,
	lvl int(11) NOT NULL,
	comment varchar(512),
	FOREIGN KEY (c_charid, skill_cat) REFERENCES aqlcharacters_char_skill_cat(c_charid, skill_cat),
	FOREIGN KEY (skill_cat, skill_name) REFERENCES aqlcharacters_skills(skill_cat, skill_name),
    PRIMARY KEY (c_charid, skill_cat, skill_name)
);

-- Personnages et skin selectionné
CREATE TABLE aqlcharacters_players (
	p_uuid char(32) PRIMARY KEY,
	c_charid int(11),
	username varchar(32) NOT NULL,
	name varchar(64),
	common_skin int(11),
	skin_name varchar(64),
	updated timestamp,
	FOREIGN KEY (c_charid) REFERENCES aqlcharacters_chars(charid),
    FOREIGN KEY (common_skin) REFERENCES aqlcharacters_common_skins(skin_id),
    FOREIGN KEY (c_charid, skin_name) REFERENCES aqlcharacters_char_skins(c_charid, name)
);

-- ----------------------------------------------------------------------------------
-- POPULATE
-- ----------------------------------------------------------------------------------

INSERT INTO aqlcharacters_origins VALUES
	('Archipel des brumes', NULL),
	('Heferdon', NULL),
	('Horinthü', NULL),
	('Ishanpur', NULL),
	('Panseoth', NULL),
	('Volestria', NULL),
	('Xinzhou', NULL),
	('Yarafah Sari', NULL);

INSERT INTO aqlcharacters_races VALUES
	('orc', 'base', 'https://www.aquilon-mc.fr/wiki/minecraft/lore/orques', 1.6, 2.2, 90, 150, 100),
	('elfe', 'base', 'https://www.aquilon-mc.fr/wiki/minecraft/lore/elfes', 1.8, 2.5, 40, 100, 60),
	('nain', 'base', 'https://www.aquilon-mc.fr/wiki/minecraft/lore/nains', 0.8, 1.6, 50, 100, 70),
	('humain', 'base', 'https://www.aquilon-mc.fr/wiki/minecraft/lore/humains', 1.3, 2, 50, 130, 80),
	('gobelin', 'base', 'https://www.aquilon-mc.fr/wiki/minecraft/lore/gobelins', 0.7, 1.5, 30, 80, 55),
	('robot_wooly_ernur','ernur',NULL,NULL,NULL,NULL,NULL,NULL),
	('robot_next_track_ernur','ernur',NULL,NULL,NULL,NULL,NULL,NULL),
	('robot','staff',NULL,NULL,NULL,NULL,NULL,NULL),
	('abomination_tyrios','staff',NULL,NULL,NULL,NULL,NULL,NULL),
	('creature_ocean','staff',NULL,NULL,NULL,NULL,NULL,NULL),
	('creature_nether','staff',NULL,NULL,NULL,NULL,NULL,NULL),
	('fantome','staff',NULL,NULL,NULL,NULL,NULL,NULL),
	('naga','staff',NULL,NULL,NULL,NULL,NULL,NULL);

INSERT INTO aqlcharacters_chars VALUES
	(
		1, '50bfaf810081403b8e2f853704bf598c', 'activated', NULL, NULL,
		'Billi', 'Nargilin', 'M', 'humain',
		'12/04/731', 87, 1.76, 83.4, 2, 'Ishanpur', 'Terra', 'Scientifique',
		'Viel homme aux cheveux gris se déplacant avec une canne en bois.', NULL, NULL
	),(
		2, 'ec14e8aa89963cbc8aef4be4d35702bb', 'pending', NULL, NULL,
		'Robert', 'Bloch', 'M', 'humain',
		'23/08/752', 57, 1.68, 67.4, 0, NULL, 'Cthulu', 'Écrivain',
		NULL, NULL, 'Descriptions à venir ...'
	),(
		3, NULL, 'activated', NULL, 'animation',
		'Captain Obvious', NULL, 'M', 'humain',
		'02/01/706', NULL, NULL, NULL, NULL, NULL, 'Terra', 'Marin',
		NULL, 'Le marin le plus connu de toutes les mers de l\'Alpha Cube, et même qu\'il est immourable.', NULL
	);

INSERT INTO aqlcharacters_char_staff_notes VALUES
    (1, 2, CURRENT_TIMESTAMP, NULL, 'Vérifier son état de santé, il est vieux !');

INSERT INTO aqlcharacters_char_skins VALUES
    (1,"main","50bfaf810081403b8e2f853704bf598c-1526038202417.png"),
    (3,"principal","Staff-1485611393806.png");

INSERT INTO aqlcharacters_players VALUES
	('50bfaf810081403b8e2f853704bf598c', 1, 'Billi', NULL, NULL, "main", CURRENT_TIMESTAMP);

INSERT INTO aqlcharacters_skill_cat VALUES
    ('Art','Pour les créatifs',0),
    ('Science','Sciences appliquées',0),
    ('Milieu Naturel','Compétences de survie et de navigation',0),
    ('Artisanat','Travaux manuels des matériaux',0),
    ('Érudition', 'Sciences abstraites et autres arts de l\'esprit',0),
    ('Larcins','Pour les compétences moins légales',0),
    ('Corps','Capacités physiques des personnages',1);

INSERT INTO aqlcharacters_skills VALUES
    ('pic', 'Art', 'Pictural', 'Maîtres du pinceau et des crayons'),
    ('mus', 'Art', 'Musical', 'Pour une maîtrise des sonorités et une notion du rythme'),
    ('let', 'Art', 'Lettres', 'Manieur expert de la plume et poseur de mots'),
    ('rep', 'Art', 'Représentation', 'Champion de la prose et conteur de l\'imaginaire'),
    ('cul', 'Art', 'Culinaire', 'Le savoir-faire des fourneaux'),
    ('med', 'Science', 'Médecine', 'Gestion des ecchymoses et des fractures ouvertes'),
    ('chi', 'Science', 'Chimie et Alchimie', 'Mélangeur de produits et concepteurs de substances'),
    ('ing', 'Science', 'Ingénierie', 'Connaissance techniques et de la conception'),
    ('fau', 'Milieu Naturel', 'Faune et flore', 'Connaisseur des plantes et des bêtes'),
    ('pis', 'Milieu Naturel', 'Pistage', 'Savoir reconnaître des traces et les suivre'),
    ('sur', 'Milieu Naturel', 'Survie', 'Savoir s\'adapter au milieu et en tirer parti'),
    ('boi', 'Artisanat', 'Bois', 'Maîtrise de la menuiserie et de la charpenterie'),
    ('met', 'Artisanat', 'Métaux', 'La fabrication d\'armes, d\'outil et d\'armures lourdes'),
    ('cui', 'Artisanat', 'Cuir et Tissus', 'Concepteur de tenue et d\'armures légères'),
    ('pie', 'Artisanat', 'Pierre', 'Tailler la roche pour la beauté du résultat'),
    ('bij', 'Artisanat', 'Bijoutier', 'Créateur de parures et autres bijoux'),
    ('lin', 'Érudition', 'Linguistique', 'Maîtrise de l’art du langage et de ses origines'),
    ('his', 'Érudition', 'Histoire et Géographie', 'Conteur du passé et adeptes des lieux'),
    ('the', 'Érudition', 'Théologie', 'Connaissance des religions et autres cultes'),
    ('fur', 'Larcins', 'Furtivité', 'Savoir passer inaperçu aux yeux des autres'),
    ('cro', 'Larcins', 'Crochetage', 'Ouvrir des serrures et autres mécanismes'),
    ('esc', 'Larcins', 'Escamotage', 'L\'art du larcin et de la fourberie'),
    ('phy', 'Corps', 'Condition physique', 'Ce qui fait trait à la force et l\'endurance'),
    ('per', 'Corps', 'Perception', 'Détecter, regarder et observer'),
    ('adr', 'Corps', 'Adresse', 'Maître de la dextérité et de l\'agilité'),
    ('vol', 'Corps', 'Volonté', 'La force mentale et résistance face aux assauts');

INSERT INTO aqlcharacters_char_skill_cat VALUES
    (1,'Érudition'),
    (1,'Science'),
    (1,'Art');

INSERT INTO aqlcharacters_char_skills VALUES
    (1,'Érudition', 'Théologie', 5, 'Beaucoup de recherches et de pratique dans diverses religions'),
    (1,'Science', 'Ingénierie', 3, 'Expérience de l\'Omnilogicarium et de l\'Initiative'),
    (1,'Art', 'Culinaire', 1, 'Sait se faire cuire un oeuf');