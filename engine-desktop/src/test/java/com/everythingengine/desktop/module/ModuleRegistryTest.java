package com.everythingengine.desktop.module;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleRegistryTest {
    @Test
    void registersUniqueModulesAndReturnsStableSortedSnapshot() {
        ModuleRegistry registry = new ModuleRegistry();
        ModuleDescriptor desktop = descriptor("desktop");
        ModuleDescriptor core = descriptor("core");
        registry.register(desktop, ModuleStatus.ACTIVE);
        registry.register(core, ModuleStatus.ACTIVE);

        assertEquals(List.of("core", "desktop"),
                registry.modules().stream().map(item -> item.descriptor().id()).toList());
        assertEquals(ModuleStatus.ACTIVE, registry.modules().getFirst().status());
        assertThrows(IllegalArgumentException.class, () -> registry.register(desktop, ModuleStatus.ACTIVE));
        assertThrows(UnsupportedOperationException.class, () -> registry.modules().clear());
    }

    @Test
    void rejectsInvalidMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new ModuleDescriptor(" ", "Core", "1", "desc", List.of(), List.of()));
    }

    private ModuleDescriptor descriptor(String id) {
        return new ModuleDescriptor(id, id, "1.0", "Test module", List.of(), List.of());
    }
}
