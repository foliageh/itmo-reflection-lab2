package org.example.model;

import org.example.generator.Generatable;

/** Помеченный класс, зависящий от непомеченного. */
@Generatable
public class Wrapper {
    public Wrapper(NotGeneratable inner) {
    }
}
