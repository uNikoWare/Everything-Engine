package com.everythingengine.desktop.module;

import java.util.List;
import java.util.Objects;

public record ModuleDescriptor(
        String id,
        String name,
        String version,
        String description,
        List<String> dependencies,
        List<String> capabilities) {
    public ModuleDescriptor {
        id = requireText(id, "id");
        name = requireText(name, "name");
        version = requireText(version, "version");
        description = requireText(description, "description");
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
        capabilities = List.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
