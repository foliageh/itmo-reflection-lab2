package org.example.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Разрешает генератору создавать объекты этого класса или использовать помеченный конструктор.
 * <ul>
 *     <li><b>Если аннотация висит на классе:</b>
 *     Генератор понимает, что этот класс разрешено создавать. Если попытаться сгенерировать класс
 *     без этой аннотации, программа упадет с ошибкой {@link GenerationException}.</li>
 *
 *     <li><b>Если аннотация висит на конструкторе (или нескольких):</b>
 *     Генератор будет создавать объект строго через эти помеченные конструкторы (в том числе приватные).
 *     Если на конструкторах аннотации нет, генератор возьмет любой публичный конструктор.</li>
 *
 *     <li><b>Если мы запрашиваем генерацию интерфейса или абстрактного класса:</b>
 *     Сам интерфейс пометить нельзя. Генератор будет искать среди всех доступных классов-реализаций только те,
 *     на которых висит аннотация {@code @Generatable}, и выберет одну из них для создания объекта.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR})
public @interface Generatable {
}
