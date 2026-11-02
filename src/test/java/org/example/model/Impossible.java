package org.example.model;

import org.example.generator.Generatable;
import org.example.generator.Invariant;

@Generatable
public class Impossible {
    public Impossible(int value) {
    }

    @Invariant
    boolean never() {
        return false;
    }
}
