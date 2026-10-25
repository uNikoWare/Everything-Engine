package com.everythingengine.desktop.module;

import java.util.Objects;

public record ModuleRegistration(ModuleDescriptor descriptor, ModuleStatus status) {
    public ModuleRegistration {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(status, "status");
    }
}
