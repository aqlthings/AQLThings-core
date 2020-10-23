package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Holds a set of {@link CalendarElement}
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class CalendarSet<T extends CalendarElement> implements Iterable<T> {
    private final String type;
    private final List<T> list;
    private final Map<String, T> map;

    public CalendarSet(String type) {
        this.type = Objects.requireNonNull(type);
        this.list = new ArrayList<>();
        this.map = new HashMap<>();
    }

    // Getters

    public String getType() {
        return type;
    }

    public List<T> getList() {
        return Collections.unmodifiableList(list);
    }

    // List edition

    public void clear() {
        list.clear();
    }

    public void add(T element) {
        list.add(element);
        map.put(element.getId(), element);
    }

    public void addAll(Collection<T> elements) {
        elements.forEach(this::add);
    }

    public void addAll(T... elements) {
        Arrays.stream(elements).forEach(this::add);
    }

    // List accessors

    public T get(int id) {
        return list.get(id);
    }

    public T get(String name) {
        return map.get(name);
    }

    public int size() {
        return list.size();
    }

    public Stream<T> stream() {
        return list.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return list.spliterator();
    }
}
