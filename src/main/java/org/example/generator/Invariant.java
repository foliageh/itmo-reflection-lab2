package org.example.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает метод класса, который проверяет корректность созданного
 * объекта (должен возвращать boolean и не иметь параметров).
 * Если метод вернул false, генератор пробует создать объект заново.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Invariant {
}
