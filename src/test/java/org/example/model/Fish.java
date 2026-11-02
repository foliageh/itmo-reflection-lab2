package org.example.model;

/** Реализация без @Generatable — не должна выбираться при генерации Animal. */
public class Fish extends Animal {
    public Fish(String name) {
        super(name);
    }
}
