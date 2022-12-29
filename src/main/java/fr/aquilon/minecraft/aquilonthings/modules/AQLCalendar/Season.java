package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.Objects;

/**
 * Holds info about a season
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class Season implements ICalendarElement {
    private final CalendarSet<Season> seasonSet;
    private final int index;
    private final String id;
    private String name;
    private float dayLengthRatio;

    public Season(CalendarSet<Season> seasonSet, int index, String id, String name, float dayLengthRatio) {
        this.seasonSet = Objects.requireNonNull(seasonSet);
        this.index = index;
        this.name = name;
        this.id = Objects.requireNonNull(id);
        setDayLengthRatio(dayLengthRatio);
    }

    public CalendarSet<Season> getSeasonSet() {
        return seasonSet;
    }

    public String getType() {
        return seasonSet.getType();
    }

    public int getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDayLengthRatio() {
        return dayLengthRatio;
    }

    public void setDayLengthRatio(float dayLengthRatio) {
        if (dayLengthRatio > 1 || dayLengthRatio < 0)
            throw new IllegalArgumentException("Ratio must be between 0 and 1");
        this.dayLengthRatio = dayLengthRatio;
    }

    public Season next() {
        return seasonSet.get((index + 1) % seasonSet.size());
    }

    public Season previous() {
        return seasonSet.get((index - 1) % seasonSet.size());
    }

    /**
     * @param dayLength The length of a full day in ticks (from midnight to midnight)
     * @return The duration of the day in ticks for the current season
     */
    public long getDayTimeTicks(long dayLength) {
        return (long) (dayLengthRatio * dayLength);
    }

    /**
     * @param dayLength The length of a full day in ticks (from midnight to midnight)
     * @return The duration of the night in ticks for the current season
     */
    public long getNightTimeTicks(long dayLength) {
        return (long) ((1f- dayLengthRatio) * dayLength);
    }
}
