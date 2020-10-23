package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.Objects;

/**
 * Helper to build a calendar type
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class CalendarBuilder {
    private final String type;
    private final CalendarSeasons seasons;
    private final CalendarMonths months;
    private long dayLength;
    private int daysPerMonth;
    private int seasonDaysOffset;

    public CalendarBuilder(String type) {
        this.type = Objects.requireNonNull(type);
        this.seasons = new CalendarSeasons(type);
        this.months = new CalendarMonths(type);
    }

    public CalendarBuilder dayLength(long dayLength) {
        this.dayLength = dayLength;
        return this;
    }

    public CalendarBuilder daysPerMonth(int daysPerMonth) {
        this.daysPerMonth = daysPerMonth;
        return this;
    }

    public CalendarBuilder seasonDaysOffset(int seasonDaysOffset) {
        if (seasonDaysOffset < 0)
            throw new IllegalStateException("Invalid day offset for seasons, positive values only");
        this.seasonDaysOffset = seasonDaysOffset;
        return this;
    }

    public CalendarBuilder addSeason(String id, String name, float dayLengthRatio) {
        seasons.add(new CalendarSeasons.Season(seasons, seasons.size(), id, name, dayLengthRatio));
        return this;
    }

    public CalendarBuilder addMonth(String id, String name, int days) {
        months.add(new CalendarMonths.Month(months, months.size(), id, name, days));
        return this;
    }

    public CalendarType build() {
        if (seasons.size() < 1) throw new IllegalStateException("Missing seasons (at least one required)");
        if (months.size() < 1) throw new IllegalStateException("Missing months (at least one required)");
        if (dayLength <= 0) throw new IllegalStateException("Missing day length");
        if (daysPerMonth <= 0) throw new IllegalStateException("Missing number of days per month");
        return new CalendarType(type, seasons, months, dayLength, daysPerMonth, seasonDaysOffset);
    }
}
