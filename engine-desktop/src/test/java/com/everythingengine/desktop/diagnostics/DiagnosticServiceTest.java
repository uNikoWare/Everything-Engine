package com.everythingengine.desktop.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticServiceTest {
    @Test
    void recordsMessagesWithTheirSourcesAndCountsErrors() {
        DiagnosticService diagnostics = new DiagnosticService();

        diagnostics.info("Core", "Initialized");
        diagnostics.error("Testing", "Maven failed", null);

        assertEquals(2, diagnostics.entries().size());
        assertEquals("Core", diagnostics.entries().getFirst().source());
        assertEquals("Testing", diagnostics.entries().getLast().source());
        assertEquals(Level.SEVERE, diagnostics.entries().getLast().level());
        assertTrue(diagnostics.entries().getLast().message().contains("Maven failed"));
        assertEquals(1, diagnostics.errorCount());
    }
}
