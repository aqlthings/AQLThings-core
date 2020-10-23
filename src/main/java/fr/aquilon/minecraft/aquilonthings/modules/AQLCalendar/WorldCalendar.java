package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Holds info about a world date and time
 * Created on 19/10/2020.
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class WorldCalendar implements JSONExportable {
    private final String worldName;
    private final CalendarType type;
    private int startYear;
    private CalendarSeasons.Season startSeason;
    private CalendarMonths.Month startMonth;
    private int startDay;
    private Integer fixedYear;
    private CalendarSeasons.Season fixedSeason;
    private CalendarMonths.Month fixedMonth;
    private int fixedDay;

    public WorldCalendar(String worldName, CalendarType type) {
        this.worldName = Objects.requireNonNull(worldName);
        this.type = type;
        fixedDay = -1;
    }

    // ---- Getters ----

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public boolean hasCalendar() {
        return type != null;
    }

    public CalendarType getType() {
        return type;
    }

    public long getTotalDayLength() {
        return type.getTotalDayLength();
    }

    public int getStartYear() {
        return startYear;
    }

    public CalendarSeasons.Season getStartSeason() {
        return startSeason;
    }

    public int getStartSeasonId() {
        return startSeason != null ? startSeason.getIndex() : 0;
    }

    public CalendarMonths.Month getStartMonth() {
        return startMonth;
    }

    public int getStartMonthId() {
        return startMonth != null ? startMonth.getIndex() : 0;
    }

    // ---- Accessor utils ----
    // Year

    public boolean isFixedYear() {
        return fixedYear != null;
    }

    public int getYear() {
        if (isFixedYear()) return fixedYear;
        return startYear + (int) (getWorldTimeInDays() / getYearLength());
    }

    public void setFixedYear(Integer year) {
        this.fixedYear = year;
    }

    // Season

    public boolean isFixedSeason() {
        return fixedSeason != null;
    }

    public CalendarSeasons.Season getSeason() {
        if (isFixedSeason()) return fixedSeason;
        int nSeason = type.getSeasons().size();
        int offsetDay = (getCurrentYearDay() + type.getSeasonDaysOffset()) % getYearLength();
        int iSeason = (offsetDay / getSeasonLength()) + getStartSeasonId();
        return type.getSeasons().get(iSeason % nSeason);
    }

    public void setFixedSeason(CalendarSeasons.Season season) {
        if (!type.getName().equals(season.getType())) throw new IllegalArgumentException("Mismatched calendar types");
        this.fixedSeason = Objects.requireNonNull(season);
    }

    public void nextSeason() {
        if (fixedSeason == null) throw new IllegalStateException("Cannot increment season when not fixed");
        this.fixedSeason = fixedSeason.next();
    }

    public void previousSeason() {
        if (fixedSeason == null) throw new IllegalStateException("Cannot decrement season when not fixed");
        this.fixedSeason = fixedSeason.previous();
    }

    // Month

    public boolean isFixedMonth() {
        return fixedMonth != null;
    }

    public CalendarMonths.Month getMonth() {
        if (isFixedMonth()) return fixedMonth;
        int nMonth = type.getMonths().size();
        int iMonth = (getCurrentYearDay() / type.getDaysPerMonth()) + getStartMonthId();
        return type.getMonths().get(iMonth % nMonth);
    }

    public void setFixedMonth(CalendarMonths.Month month) {
        if (!type.getName().equals(month.getType())) throw new IllegalArgumentException("Mismatched calendar types");
        this.fixedMonth = Objects.requireNonNull(month);
    }

    // Day

    public boolean isFixedDay() {
        return fixedDay != -1;
    }

    /**
     * @return The day in this month. <b>Starts at 0</b>
     */
    public int getDay() {
        if (isFixedDay()) return fixedDay;
        return (startDay + getCurrentYearDay()) % type.getDaysPerMonth();
    }

    /**
     * @param day The day in this month. <b>Starts at 0</b>
     */
    public void setFixedDay(int day) {
        if (day < 0) throw new IllegalArgumentException("Days are positive only");
        if (day >= type.getDaysPerMonth())
            throw new IllegalArgumentException("Maximum "+type.getDaysPerMonth()+" days per month");
        this.fixedDay = day;
    }

    // ---- Utils ----

    /**
     * @return The number of days in a year
     */
    public int getYearLength() {
        return type.getDaysPerMonth() * type.getMonths().size();
    }

    /**
     * @return The number of days in a season
     */
    public int getSeasonLength() {
        return getYearLength() / type.getSeasons().size();
    }

    /**
     * @return The duration of the day in ticks for the current season
     */
    public long getDayTimeTicks() {
        return getSeason().getDayTimeTicks(type.getTotalDayLength());
    }

    /**
     * @return The duration of the night in ticks for the current season
     */
    public long getNightTimeTicks() {
        return getSeason().getNightTimeTicks(type.getTotalDayLength());
    }

    /**
     * @return The adjusted age of the world in ticks
     */
    public long getWorldTime() {
        World w = getWorld();
        if (w == null) throw new IllegalStateException("Unknown world: "+worldName);
        return w.getFullTime() + (type.getTotalDayLength() / 3); // Shift by a third of a day to account for day time
    }

    /**
     * @return The number of ticks since the start of this day (midnight)
     */
    public long getCurrentDayTicks() {
        return getWorldTime() % type.getTotalDayLength();
    }

    public boolean isDayTime() {
        return getCurrentDayTicks() <= getDayTimeTicks();
    }

    public boolean isNightTime() {
        return !isDayTime();
    }

    /**
     * @return A ratio (between 0 and 1) representing the current time of day (percentage)
     */
    public float getCurrentDayRatio() {
        return getCurrentDayTicks() / (float) type.getTotalDayLength();
    }

    /**
     * @return The number of minutes since the start of this day (midnight)
     */
    public float getCurrentDayMinutes() {
        return getCurrentDayRatio() * 24f * 60f;
    }

    public int getHour() {
        return (int) (getCurrentDayMinutes() / 60f);
    }

    public int getMinute() {
        return (int) (getCurrentDayMinutes() % 60f);
    }

    public String getTime() {
        return String.format("%02d:%02d", getHour(), getMinute());
    }

    public String getDate() {
        return String.format("%04d-%02d-%02d", getYear(), getMonth().getIndex()+1, getDay()+1);
    }

    // ---- Internal utils ----

    /**
     * @return The age of the world in days
     */
    public long getWorldTimeInDays() {
        return getWorldTime() / type.getTotalDayLength();
    }

    public int getCurrentYearDay() {
        return (int) (getWorldTimeInDays() % getYearLength());
    }

    public String getUpdatePacketData() {
        // Format:
        // $ <world_name>:<calendar_type>:[y/Y]<year>:[s/S]<season>:[m/M]<month>:[d/D]<day>
        return worldName + ':' +
                type.getName() + ':' +
                (isFixedYear() ? 'Y'+fixedYear : 'y'+startYear) + ':' +
                (isFixedSeason() ? 'S'+fixedSeason.getIndex() : 's'+(startSeason != null ? startSeason.getIndex() : 0)) +':'+
                (isFixedMonth() ? 'M'+fixedMonth.getIndex() : 'm'+(startMonth != null ? startMonth.getIndex() : 0)) + ':' +
                (isFixedDay() ? 'D'+fixedDay : 'd'+startDay);
    }

    // ---- Export methods ----

    @Override
    public JSONObject toJSON() {
        return toJSON(false);
    }

    public JSONObject toJSON(boolean detailed) {
        JSONObject res = new JSONObject();
        res.put("type", type != null ? type.getName() : JSONObject.NULL);
        res.put("age", getWorldTime());
        res.put("date", getDate());
        res.put("time", getTime());
        if (detailed) {
            res.put("year", getYear());
            res.put("month", getMonth().getName());
            res.put("season", getSeason().getId());
            res.put("day", getDay() + 1);
        }
        // TODO: complete
        return res;
    }
}
