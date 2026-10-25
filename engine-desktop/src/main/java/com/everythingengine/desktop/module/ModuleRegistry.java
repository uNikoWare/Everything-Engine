package com.everythingengine.desktop.module;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuleRegistry {
    private final Map<String, ModuleRegistration> modules = new ConcurrentHashMap<>();

    public void register(ModuleDescriptor descriptor, ModuleStatus status) {
        Objects.requireNonNull(descriptor, "descriptor");
        ModuleRegistration registration = new ModuleRegistration(descriptor, status);
        if (modules.putIfAbsent(descriptor.id(), registration) != null) {
            throw new IllegalArgumentException("Module is already registered: " + descriptor.id());
        }
    }

    public List<ModuleRegistration> modules() {
        return modules.values().stream()
                .sorted((left, right) -> left.descriptor().id().compareTo(right.descriptor().id()))
                .toList();
    }
}
