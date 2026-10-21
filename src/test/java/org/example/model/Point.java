package org.example.model;

import org.example.generator.Generatable;

@Generatable
public record Point(int x, int y) {
}
