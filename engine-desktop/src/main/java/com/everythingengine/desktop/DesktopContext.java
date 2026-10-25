package com.everythingengine.desktop;

import com.everythingengine.core.universe.Universe;
import com.everythingengine.desktop.diagnostics.DiagnosticService;
import com.everythingengine.desktop.module.ModuleDescriptor;
import com.everythingengine.desktop.module.ModuleRegistry;
import com.everythingengine.desktop.module.ModuleStatus;
import com.everythingengine.desktop.settings.SettingsStore;
import com.everythingengine.desktop.testing.MavenTestService;
import com.everythingengine.desktop.testing.TestRunResult;

import java.nio.file.Path;

public final class DesktopContext {
    private final Universe universe = new Universe();
    private final DiagnosticService diagnostics = new DiagnosticService();
    private final ModuleRegistry modules = new ModuleRegistry();
    private final SettingsStore settings = new SettingsStore(diagnostics);
    private final Path projectRoot = MavenTestService.locateProjectRoot(Path.of(System.getProperty("user.dir")));
    private TestRunResult lastTestResult;

    public DesktopContext() {
        modules.register(new ModuleDescriptor(
                "everything-engine.core", "Engine Core", "1.0",
                "Entity, attribute, relation and universe APIs.",
                java.util.List.of(), java.util.List.of("Universe management")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.randomization", "Randomization Utilities", "1.0",
                "Deterministic seeded generation, weighted choice selection and reproducible random services.",
                java.util.List.of("everything-engine.core"),
                java.util.List.of("Seeded RNG", "Weighted choice", "Deterministic generation")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.validation", "Validation Framework", "1.0",
                "Reusable validation rules, error reporting, and issue aggregation.",
                java.util.List.of("everything-engine.core"),
                java.util.List.of("Validation rules", "Issue collection", "Warnings/errors")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.naming", "Name Generation", "1.0",
                "Profile-driven naming strategies with deterministic generation and duplicate avoidance.",
                java.util.List.of("everything-engine.core", "everything-engine.core.randomization"),
                java.util.List.of("Syllable generation", "Word generation", "Template generation")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.desktop", "Desktop Application", "1.0",
                "JavaFX development and inspection tools.", java.util.List.of(),
                java.util.List.of("Universe inspection", "Diagnostics")), ModuleStatus.ACTIVE);
        diagnostics.info("Application",
                "Engine Core and generation modules initialized; current universe is in-memory only.");
        if (projectRoot == null) {
            diagnostics.warning("Testing", "No Maven project was found from the current working directory.");
        }
    }

    public Universe universe() {
        return universe;
    }

    public DiagnosticService diagnostics() {
        return diagnostics;
    }

    public ModuleRegistry modules() {
        return modules;
    }

    public SettingsStore settings() {
        return settings;
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public TestRunResult lastTestResult() {
        return lastTestResult;
    }

    public void setLastTestResult(TestRunResult lastTestResult) {
        this.lastTestResult = lastTestResult;
    }
}
