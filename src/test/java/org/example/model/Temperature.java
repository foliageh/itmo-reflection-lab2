package org.example.model;

import org.example.generator.Generatable;
import org.example.generator.Range;

@Generatable
public class Temperature {
    private final int celsius;
    private final long kelvinTimesTen;

    public Temperature(@Range(min = -50, max = 50) int celsius, @Range(min = 2732, max = 2732) long kelvinTimesTen) {
        this.celsius = celsius;
        this.kelvinTimesTen = kelvinTimesTen;
    }

    public int getCelsius() {
        return celsius;
    }

    public long getKelvinTimesTen() {
        return kelvinTimesTen;
    }
}
