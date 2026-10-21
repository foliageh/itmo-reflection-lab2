package org.example.model;

import org.example.generator.Generatable;

@Generatable
public class Cat extends Animal {
    private final Color color;

    public Cat(String name, Color color) {
        super(name);
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
