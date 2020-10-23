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
 * Holds a set of seasons
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class CalendarSeasons implements Iterable<CalendarSeasons.Season> {
    private final String type;
    private List<Season> list;

    public CalendarSeasons(String type) {
        this.type = Objects.requireNonNull(type);
        this.list = new ArrayList<>();
    }

    // Getters

    public String getType() {
        return type;
    }

    public List<Season> getList() {
        return Collections.unmodifiableList(list);
    }

    // List edition

    public void clear() {
        list.clear();
    }

    public boolean add(Season season) {
        return list.add(season);
    }

    public boolean addAll(Collection<Season> seasons) {
        return list.addAll(seasons);
    }

    public boolean addAll(Season... seasons) {
        return list.addAll(Arrays.asList(seasons));
    }

    // List accessors

    public Season get(int id) {
        return list.get(id);
    }

    public int size() {
        return list.size();
    }

    public Stream<Season> stream() {
        return list.stream();
    }

    @Override
    public Iterator<Season> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super Season> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<Season> spliterator() {
        return list.spliterator();
    }

    // A single season

    public static class Season {
        private final CalendarSeasons seasonSet;
        private final int index;
        private final String id;
        private String name;
        private float dayLengthRatio;

        public Season(CalendarSeasons seasonSet, int index, String id, String name, float dayLengthRatio) {
            this.seasonSet = Objects.requireNonNull(seasonSet);
            this.index = index;
            this.name = name;
            this.id = Objects.requireNonNull(id);
            setDayLengthRatio(dayLengthRatio);
        }

        public CalendarSeasons getSeasonSet() {
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
            return null;
        }

        public Season previous() {
            return null;
        }

        /**
         * @return The duration of the day in ticks for the current season
         */
        public long getDayTimeTicks(long dayLength) {
            return (long) (dayLengthRatio * dayLength);
        }

        /**
         * @return The duration of the night in ticks for the current season
         */
        public long getNightTimeTicks(long dayLength) {
            return (long) ((1f- dayLengthRatio) * dayLength);
        }
    }
}
