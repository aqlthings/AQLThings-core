# AQLCalendar

## About ##

Manages the current time (time of day and date) and seasons.

season month and daylength required to create a calendar type

TODO : AQL Calendar API : open issue

CalendarType : the structure of time of a defined calendar
WorldCalendar : current state of time of a world

A WorldCalendar has :

| Name     | Type | Description |
|----------|------|-------------|
| name     | string | the name of the calendar |
| year     | int | the year number. For instance "1789" |
| monthId     | int | the current monthId|
| day     | int |  day of the month |
| seasonId | int |  current seasonId |
(what is fixed ?)

## Time tree description ##

``` mermaid
erDiagram
    WorldCalendar ||--|| CalendarType : "described by"
    WorldCalendar {
        int id
        string name 
        int year "current year"
        int monthId "current month"
        int day "current day of the year"
        int seasonId "current season"
    }
    CalendarType ||--|{ Month : has
    CalendarType ||--|{ Season : has
    CalendarType {
        int id
        int seasonDaysOffset
        int dayLength "in ticks"
    }
    Season ||--|{ Month : has
    Season {
        int id
        int index "order of the season"
        int name "name of the season"
        float dayLengthRatio "duration of the day for this season"
    }
    Month {
        int id
        int index "order of the month in the year"
        int days "number of days in month"
    }
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
