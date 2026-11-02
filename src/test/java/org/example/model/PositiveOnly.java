package org.example.model;

import org.example.generator.Generatable;

/** Конструктор отвергает отрицательные значения — генератор должен перегенерировать аргументы. */
@Generatable
public class PositiveOnly {
    private final int value;

    public PositiveOnly(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
