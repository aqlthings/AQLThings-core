package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.Iterator;
import java.util.Objects;

/**
 * Holds info about a month
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class Month implements CalendarElement {
    private final CalendarSet<Month> monthSet;
    private final int index;
    private final String id;
    private String name;
    private int days;

    public Month(CalendarSet<Month> monthSet, int index, String id, String name, int days) {
        this.monthSet = Objects.requireNonNull(monthSet);
        this.index = index;
        this.id = Objects.requireNonNull(id);
        this.name = name;
        setDays(days);
    }

    public CalendarSet<Month> getMonthSet() {
        return monthSet;
    }

    public String getType() {
        return monthSet.getType();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        if (days <= 0) throw new IllegalArgumentException("Invalid number of days");
        this.days = days;
    }

    // Helper method

    public static DayInMonth getDayInMonth(CalendarSet<Month> months, int dayInYear) {
        Iterator<Month> it = months.iterator();
        Month m = null;
        int days = 0;
        while (it.hasNext()) {
            m = it.next();
            if (days+m.days >= dayInYear) break;
            days += m.days;
        }
        return new DayInMonth(dayInYear-days, m);
    }

    public static class DayInMonth {
        public final int day;
        public final Month month;

        public DayInMonth(int day, Month month) {
            this.day = day;
            this.month = month;
        }
    }
}
