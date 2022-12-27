# AQLBabel

## About ##
This module allows players to "speak" differents languages, according to their character. Player are marked as locutor of a language, will see full tchat messages. Non-locutor will get gibberish text instead !

### exceptions ###
A player (admin) with a permissions  to read a language will read without obfuscation any messages.

No global message and no technical message are obfuscated.

### suggestion ###
According to the level,

## Commands ##
```
/babel
```
It displays a list of potential commands.
```
/babel info <player>
```
Displays known languages and level for this player (character?)
```
/babel select <lang>
```
Allows the player to select one of his known language to speak with
```
/babel set <player> <lang> <level> [<comment>]
```
Sets the level of a player in a specific language (quid level 0 ?)
```
/babel lang
```
Display a list of commands to edit languages
```
/babel lang list
```
Displays a list of all languages
```
/babel lang info <lang key>
```
Displays information about this language (name, key, alphabet, description, locutors and readers)
```
/babel lang new <lang key> <alphabet>
```
Allows to create a new language with the specified key and alphabets. Ex: elve Quenya
```
/babel lang name <lang key> <name>
```
Allows to name/rename an existing language. Ex: Elvish
```
/babel lang desc <lang key> <desc>
```
Allows add/update a description to an existing language
```
/babel lang alphabet <lang key> <alphabet>
```
Allows add/update the alphabet of an existing language
```
/babel lang delete <lang key>
```
Deletes existing language