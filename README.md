# AquilonThings #
_A Minecraft Bukkit plugin framework for modular functionnalities oriented towards roleplay servers_

## About ##
This project provides a framework to quickly develop new plugin features for minecraft servers.

It was originally developed for the specific purpose of *aquilon-mc.fr* minecraft server.
Since it has closed down the choice was made to render the code publicly available so that it can be used by others.

This project remains actively developed and new features are often added.
If you would like to contribute please let us know by opening an issue, and we will gladly help you get started.

As of now the project still severely lacks documentation. In an effort to help user get started with the project
we recently started redacting a proper documentation. Feel free to help us out.

Some parts of the code might be in French since it was initially developed in this language.

## Main concepts ##
This plugin relies on a modular system.
The core plugin provides the framework and each module add its own functionality.

Modules can be dependent on each other and can use other plugins.

Currently all modules are contained within a single JAR file
however a dynamic module loading system is being developed.

## Modules ##
### AQLVox ###
The HTTP API for AquilonThings. It runs a web server to expose an HTTP REST API.

AQLVox itself is subdivided in API modules, each AquilonThings module can register API modules
to expose public routes.

### AQLBlessures ###
An injury manager. Keeps track of injured players and rolls new injuries automatically.

### AQLCharacters ###
_Still in Development_

A roleplay character sheet directly interfaced with the game. It uses AQLVox to expose an API backend
and a VueJS application as a frontend.

### AQLChat ###
A full-featured chat channel manager.

### AQLEmotes ###
_Requires client side modifications_

Handles player emotes.

### AQLFire ###
Keep track of and fight fires.

### AQLGroups ###
_Requires external components_

Automatically synchronise player groups and permissions with a PHPBB forum.

### AQLLooting ###
Add custom loots from your plugins.

### AQLMarkers ###
A marker system for maps and in-game teleportation.

### AQLMisc ###
Adds miscellaneous commands and world policies.

### AQLNames ###
Manages players roleplay names and descriptions.

### AQLNobles ###
Custom module to give some players specific abilities.

### AQLPlaces ###
Add custom events and actions to places in your world.

### AQLRandoms ###
A simple dice roller.

### AQLRegionBorder ###
_Requires client side modifications_

Send clients information about the borders of the current WorldGard area they are in.

### AQLSeason ###
_Requires client side modifications_

Manages the current time (time of day and date) and seasons.

### AQLStaff ###
Miscellaneous utilities for minecraft server staffs.

### AQLVanish ###
Allows some player to connect to the server without anyone knowing.

### AQLBabel ###
Allows players to "speak" with differents languages, using chat color or prefixes.

### And more ... ###
Some other modules are still in development and haven't yet been merged in the master branch.

 * AQLDonjon
 * RandomEncounters

## Authors ##
Main developers:
 * [BilliAlpha](https://github.com/BilliAlpha)
 * Termm

## License ##
This project is licensed under the [GNU General Public License v3.0](https://opensource.org/licenses/GPL-3.0)
making it Open-Source (approved by the Open Source Initiative)
