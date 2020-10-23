package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Holds info about a single month
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class CalendarMonths implements Iterable<CalendarMonths.Month> {
    private final String type;
    private List<Month> list;

    public CalendarMonths(String type) {
        this.type = Objects.requireNonNull(type);
        this.list = new ArrayList<>();
    }

    // Getters

    public String getType() {
        return type;
    }

    public List<Month> getList() {
        return Collections.unmodifiableList(list);
    }

    // List edition

    public void clear() {
        list.clear();
    }

    public boolean add(Month month) {
        return list.add(month);
    }

    public boolean addAll(Collection<Month> months) {
        return list.addAll(months);
    }

    public boolean addAll(Month... months) {
        return list.addAll(Arrays.asList(months));
    }

    // List accessors

    public Month get(int id) {
        return list.get(id);
    }

    public int size() {
        return list.size();
    }

    public Stream<Month> stream() {
        return list.stream();
    }

    @Override
    public Iterator<Month> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super Month> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<Month> spliterator() {
        return list.spliterator();
    }

    // A single month

    public static class Month {
        private final CalendarMonths monthSet;
        private final int index;
        private final String id;
        private String name;
        private int days;

        public Month(CalendarMonths monthSet, int index, String id, String name, int days) {
            this.monthSet = Objects.requireNonNull(monthSet);
            this.index = index;
            this.id = Objects.requireNonNull(id);
            this.name = name;
            this.days = days;
        }

        /*
        public Month(CalendarMonths monthSet, int id, String name) {
            this.monthSet = Objects.requireNonNull(monthSet);
            this.id = id;
            this.name = Objects.requireNonNull(name);
        }*/

        public CalendarMonths getMonthSet() {
            return monthSet;
        }

        public String getType() {
            return monthSet.getType();
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

        public int getDays() {
            return days;
        }

        public void setDays(int days) {
            this.days = days;
        }
    }
}
