# AQlNobles

## About ##
Custom module to give some players specific abilities. "Noble" are trusted player that can guide new player into the community. To do so, they get the ability to teleport to visitor to accompany them and came back.

### exceptions ###
A player (admin) with a permissions  to read a language will read without obfuscation any messages.

No global message and no technical message are obfuscated.

### suggestion ###
According to the level,

## Commands ##
```
/noble
```
It displays a list of potential commands.
```
/noble info <player>
```
Displays known languages and level for this player (character?)
```
/noble select <lang>
```
Allows the player to select one of his known language to speak with
```
/noble set <player> <lang> <level> [<comment>]
```
Sets the level of a player in a specific language (quid level 0 ?)
```
/noble lang
```
Display a list of commands to edit languages
```
/noble lang list
```
Displays a list of all languages
```
/noble lang info <lang key>
```
Displays information about this language (name, key, alphabet, description, locutors and readers)
```
/noble lang new <lang key> <alphabet>
```
Allows to create a new language with the specified key and alphabets. Ex: elve Quenya
```
/noble lang name <lang key> <name>
```
Allows to name/rename an existing language. Ex: Elvish
```
/noble lang desc <lang key> <desc>
```
Allows add/update a description to an existing language
```
/noble lang alphabet <lang key> <alphabet>
```
Allows add/update the alphabet of an existing language
```
/noble lang delete <lang key>
```
Deletes existing language