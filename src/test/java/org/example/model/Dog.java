package org.example.model;

import org.example.generator.Generatable;

@Generatable
public class Dog extends Animal {
    private final int age;

    public Dog(String name, int age) {
        super(name);
        this.age = age;
    }

    public int getAge() {
        return age;
    }
}
