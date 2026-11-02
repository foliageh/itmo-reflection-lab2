package org.example.model;

import org.example.generator.Generatable;

@Generatable
public class AlwaysFails {
    public AlwaysFails() {
        throw new IllegalStateException("boom");
    }
}
