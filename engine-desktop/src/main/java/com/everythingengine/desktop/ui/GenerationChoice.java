package com.everythingengine.desktop.ui;

import java.util.Optional;

record GenerationChoice<T>(String label, Optional<T> value) {
    static <T> GenerationChoice<T> fixed(String label, T value) {
        return new GenerationChoice<>(label, Optional.of(value));
    }

    static <T> GenerationChoice<T> random() {
        return new GenerationChoice<>("Random", Optional.empty());
    }
}
