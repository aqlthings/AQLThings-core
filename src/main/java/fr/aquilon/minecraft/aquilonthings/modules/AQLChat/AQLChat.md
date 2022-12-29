# AQLChat

## About ##

A full-featured chat channel manager. It allows the creation of custom channels with Minecraft color and format (see documentation).
Message visibility is based on permission, channel subscription, and distance between players.

For instance, one can create a "whispering" channel **w** with a distance of 3 blocks. Only players very close to one another will receive the broadcasted message. This can be used for description, event, or ambient channels.

## Channel description ##

A channel is quite simple:

| Name     | Type | Description |
|----------|------|-------------|
| name     | string | the name of the channel |
| nick     | string | the alias of the channel, used in commands |
| color    | string | the minecraft color/format chars of the channel |
| distance | int | the maximum distance in which player will receive messages |
| format   | string | the template format of the channel |

Default format : {color}[{nick}] {sender}{color}: {message}

```
[L] Legolas: dawn is red... 
```

### Exceptions ###

### Permissions ###
[root]
.chat.channel.
.join
.read
.speak
.leave
.format
.ban
### suggestion ###

## Commands ##

```
/ch list
```
Displays a list of the channels

```
/ch (ban/unban) <joueur> <channel>
```
Ban/unban a player from channel.

```
/ch join <channel>
```
Allows to join (subscribe) to a channel

```
/ch <channel>
```
Alias to join a channel and speak in this channel (for instance switching to whispering...)

```
/ch leave <channel>
```
Allow to leave (unsubscribe) a channel. (Useful to leave a general channel during an event, but useless when leaving the local channel during an event...)

```
/<channel> <message>
```
When speaking in a previously selected channel, broadcast this message in another. (Useful to scream in a crowd, while whispering to your neighbor).

```
/qmsg <player> <msg>
```
