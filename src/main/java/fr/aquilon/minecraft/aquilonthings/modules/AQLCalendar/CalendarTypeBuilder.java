package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.Objects;

/**
 * Helper to build a calendar type
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class CalendarTypeBuilder {
    private final String type;
    private final CalendarSet<Season> seasons;
    private final CalendarSet<Month> months;
    private long dayLength;
    private int seasonDaysOffset;

    public CalendarTypeBuilder(String type) {
        this.type = Objects.requireNonNull(type);
        this.seasons = new CalendarSet<>(type);
        this.months = new CalendarSet<>(type);
    }

    public String getType() {
        return type;
    }

    public CalendarTypeBuilder dayLength(long dayLength) {
        this.dayLength = dayLength;
        return this;
    }

    public CalendarTypeBuilder seasonDaysOffset(int seasonDaysOffset) {
        if (seasonDaysOffset < 0)
            throw new IllegalStateException("Invalid day offset for seasons, positive values only");
        this.seasonDaysOffset = seasonDaysOffset;
        return this;
    }

    public CalendarTypeBuilder addSeason(String id, String name, float dayLengthRatio) {
        seasons.add(new Season(seasons, seasons.size(), id, name, dayLengthRatio));
        return this;
    }

    public CalendarTypeBuilder addMonth(String id, String name, int days) {
        months.add(new Month(months, months.size(), id, name, days));
        return this;
    }

    public CalendarType build() {
        if (seasons.size() < 1) throw new IllegalStateException("Missing seasons (at least one required)");
        if (months.size() < 1) throw new IllegalStateException("Missing months (at least one required)");
        if (dayLength <= 0) throw new IllegalStateException("Missing day length");
        return new CalendarType(type, seasons, months, dayLength, seasonDaysOffset);
    }
}
