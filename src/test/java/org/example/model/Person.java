package org.example.model;

import org.example.generator.Generatable;

import java.util.Map;
import java.util.Set;

@Generatable
public class Person {
    private final String name;
    private final Set<Color> favouriteColors;
    private final Map<String, Point> places;
    private final boolean createdByAnnotatedConstructor;

    public Person(String name) {
        this.name = name;
        this.favouriteColors = Set.of();
        this.places = Map.of();
        this.createdByAnnotatedConstructor = false;
    }

    @Generatable
    private Person(String name, Set<Color> favouriteColors, Map<String, Point> places) {
        this.name = name;
        this.favouriteColors = favouriteColors;
        this.places = places;
        this.createdByAnnotatedConstructor = true;
    }

    public String getName() {
        return name;
    }

    public Set<Color> getFavouriteColors() {
        return favouriteColors;
    }

    public Map<String, Point> getPlaces() {
        return places;
    }

    public boolean isCreatedByAnnotatedConstructor() {
        return createdByAnnotatedConstructor;
    }
}
