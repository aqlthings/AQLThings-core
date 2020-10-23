package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.stream.Collectors;

/**
 * Holds info about a calendar type
 * Created on 19/10/2020.
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class CalendarType {
    private final String name;
    private final CalendarSet<Season> seasons;
    private final CalendarSet<Month> months;
    private long dayLength;
    private int seasonDaysOffset;

    public CalendarType(String name,
                        CalendarSet<Season> seasons, CalendarSet<Month> months,
                        long dayLength, int seasonDaysOffset) {
        this.name = name;
        this.seasons = seasons;
        this.months = months;
        this.dayLength = dayLength;
        this.seasonDaysOffset = seasonDaysOffset;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public CalendarSet<Season> getSeasons() {
        return seasons;
    }

    public CalendarSet<Month> getMonths() {
        return months;
    }

    public long getTotalDayLength() {
        return dayLength;
    }

    public void setTotalDayLength(long dayLength) {
        this.dayLength = dayLength;
    }

    public int getSeasonDaysOffset() {
        return seasonDaysOffset;
    }

    public void setSeasonDaysOffset(int seasonDaysOffset) {
        this.seasonDaysOffset = seasonDaysOffset;
    }

    public String getUpdatePacketData() {
        // Format:
        // $ <calendar_type>:{seasons}:{months}:<dayLength>:<seasonDaysOffset>
        //      {seasons} = <count>:{season}|*
        //       {season} = <id>/<name>/<dayTimeRatio>
        //       {months} = <count>:{month}|*
        //        {month} = <id>/<name>/<days>
        return name + ':' +
                seasons.size() + ':' +
                seasons.stream().map(s -> s.getId()+'/'+s.getName()+'/'+s.getDayLengthRatio())
                        .collect(Collectors.joining("|")) + ':' +
                months.size() + ':' +
                months.stream().map(m -> m.getId()+'/'+m.getName()+'/'+m.getDays())
                        .collect(Collectors.joining("|")) + ':' +
                getTotalDayLength() + ':' +
                seasonDaysOffset;
    }
}
