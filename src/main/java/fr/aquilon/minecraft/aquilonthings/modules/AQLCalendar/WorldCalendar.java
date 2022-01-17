package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Holds info about a world date and time
 * Created on 19/10/2020.
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class WorldCalendar implements JSONExportable {
    private final String worldName;
    private final CalendarType type;
    private int yYear;
    private boolean fixedYear;
    private Season sSeason;
    private boolean fixedSeason;
    private Month mMonth;
    private boolean fixedMonth;
    private int dDay;
    private boolean fixedDay;

    public WorldCalendar(String worldName, CalendarType type) {
        this.worldName = Objects.requireNonNull(worldName);
        this.type = type;
        fixedYear = fixedSeason = fixedMonth = fixedDay = false;
    }

    // ---- Getters & Setters ----

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

    // ---- Accessor utils ----
    // Year

    public boolean isFixedYear() {
        return fixedYear;
    }

    public int getYear() {
        if (isFixedYear()) return yYear;
        return yYear + (int) (getWorldTimeInDays() / getYearLength());
    }

    public void setFixedYear(int fixedYear) {
        this.fixedYear = true;
        this.yYear = fixedYear;
    }

    public void setStartYear(int startYear) {
        this.fixedYear = false;
        this.yYear = startYear;
    }

    public void setAutoYear() {
        this.fixedYear = false;
        this.yYear = 0;
    }

    public WorldCalendar setYear(int year, boolean isFixedYear) {
        if (isFixedYear) setFixedYear(year);
        else setStartYear(year);
        return this;
    }

    // Season

    public boolean isFixedSeason() {
        return fixedSeason;
    }

    public Season getSeason() {
        if (isFixedSeason()) return sSeason;
        int nSeason = type.getSeasons().size();
        int offsetDay = (getCurrentYearDay() + type.getSeasonDaysOffset()) % getYearLength();
        int iSeason = (offsetDay / getSeasonLength()) + (sSeason != null ? sSeason.getIndex() : 0);
        return type.getSeasons().get(iSeason % nSeason);
    }

    public void setFixedSeason(Season fixedSeason) {
        if (!type.getName().equals(Objects.requireNonNull(fixedSeason).getType()))
            throw new IllegalArgumentException("Mismatched calendar types");
        this.fixedSeason = true;
        this.sSeason = fixedSeason;
    }

    public void setStartSeason(Season startSeason) {
        if (startSeason != null && !type.getName().equals(startSeason.getType()))
            throw new IllegalArgumentException("Mismatched calendar types");
        this.fixedSeason = false;
        this.sSeason = startSeason;
    }

    public void setAutoSeason() {
        this.fixedSeason = false;
        this.sSeason = null;
    }

    public void nextSeason() {
        if (!fixedSeason) throw new IllegalStateException("Cannot increment season when not fixed");
        this.sSeason = sSeason.next();
    }

    public void previousSeason() {
        if (!fixedSeason) throw new IllegalStateException("Cannot decrement season when not fixed");
        this.sSeason = sSeason.previous();
    }

    public WorldCalendar setSeason(Season season, boolean isFixedSeason) {
        if (isFixedSeason) setFixedSeason(season);
        else setStartSeason(season);
        return this;
    }

    // Month

    public boolean isFixedMonth() {
        return fixedMonth;
    }

    public Month getMonth() {
        if (isFixedMonth()) return mMonth;
        Month m = Month.getDayInMonth(type.getMonths(), getCurrentYearDay()).month;
        int nMonth = type.getMonths().size();
        int iMonth = m.getIndex() + (mMonth != null ? mMonth.getIndex() : 0);
        return type.getMonths().get(iMonth % nMonth);
    }

    public void setFixedMonth(Month fixedMonth) {
        if (!type.getName().equals(Objects.requireNonNull(fixedMonth).getType()))
            throw new IllegalArgumentException("Mismatched calendar types");
        this.fixedMonth = true;
        this.mMonth = fixedMonth;
    }

    public void setStartMonth(Month startMonth) {
        if (startMonth != null && !type.getName().equals(startMonth.getType()))
            throw new IllegalArgumentException("Mismatched calendar types");
        this.fixedMonth = false;
        this.mMonth = startMonth;
    }

    public void setAutoMonth() {
        this.fixedMonth = false;
        this.mMonth = null;
    }

    public WorldCalendar setMonth(Month month, boolean isFixedMonth) {
        if (isFixedMonth) setFixedMonth(month);
        else setStartMonth(month);
        return this;
    }

    // Day

    public boolean isFixedDay() {
        return fixedDay;
    }

    /**
     * @return The day in this month. <b>Starts at 0</b>
     */
    public int getDay() {
        if (isFixedDay()) return dDay;
        Month.DayInMonth dim = Month.getDayInMonth(type.getMonths(), getCurrentYearDay());
        return (dDay + dim.day) % dim.month.getDays();
    }

    /**
     * @param day The day in this month. <b>Starts at 0</b>
     */
    public void setFixedDay(int day) {
        if (day < 0) throw new IllegalArgumentException("Days are positive only");
        if (day >= getMonth().getDays())
            throw new IllegalArgumentException("Maximum "+getMonth().getDays()+" days per month");
        this.fixedDay = true;
        this.dDay = day;
    }

    public void setStartDay(int startDay) {
        this.fixedDay = false;
        this.dDay = startDay;
    }

    public void setAutoDay() {
        this.fixedDay = false;
        this.dDay = 0;
    }

    public WorldCalendar setDay(int day, boolean isFixedDay) {
        if (isFixedDay) setFixedDay(day);
        else setStartDay(day);
        return this;
    }

    // ---- Utils ----

    /**
     * @return The number of days in a year
     */
    public int getYearLength() {
        return type.getMonths().stream().mapToInt(Month::getDays).sum();
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
        return worldName + ":" +
                type.getName() + ":" +
                (isFixedYear() ? "Y" : "y") + yYear + ":" +
                (isFixedSeason() ? "S" : "s") + (sSeason != null ? sSeason.getIndex() : 0) + ":" +
                (isFixedMonth() ? "M" : "m") + (mMonth != null ? mMonth.getIndex() : 0) + ":" +
                (isFixedDay() ? "D" : "d") + dDay;
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
