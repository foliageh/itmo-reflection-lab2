package org.example.model;

import java.util.List;
import java.util.Map;

/** Источник generic-типов для тестов (через Field.getGenericType()). */
public class Holder {
    public List<Integer> integers;
    public Map<Color, List<Double>> nested;
    public List<? extends Animal> animals;
}
