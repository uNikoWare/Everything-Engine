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
                "everything-engine.core", "Engine Core", "1.0", "Entity, attribute, relation and universe APIs.",
                java.util.List.of(), java.util.List.of("Universe management")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.desktop", "Desktop Application", "1.0",
                "JavaFX development and inspection tools.", java.util.List.of(),
                java.util.List.of("Universe inspection", "Diagnostics")), ModuleStatus.ACTIVE);
        diagnostics.info("Application", "Engine Core initialized; current universe is in-memory only.");
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
