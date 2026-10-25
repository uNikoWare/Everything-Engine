package com.everythingengine.desktop;

import com.everythingengine.core.universe.Universe;
import com.everythingengine.desktop.diagnostics.DiagnosticService;
import com.everythingengine.desktop.module.ModuleDescriptor;
import com.everythingengine.desktop.module.ModuleRegistry;
import com.everythingengine.desktop.module.ModuleStatus;
import com.everythingengine.desktop.persistence.UniversePersistence;
import com.everythingengine.desktop.settings.SettingsStore;
import com.everythingengine.desktop.testing.MavenTestService;
import com.everythingengine.desktop.testing.TestRunResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DesktopContext {
    private Universe universe = new Universe();
    private final DiagnosticService diagnostics = new DiagnosticService();
    private final ModuleRegistry modules = new ModuleRegistry();
    private final SettingsStore settings = new SettingsStore(diagnostics);
    private final UniversePersistence universePersistence = new UniversePersistence();
    private final Path universeFile = settings.settingsFile().getParent().resolve("universe.eeu");
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
                "everything-engine.core.spatial", "Coordinate System", "1.0",
                "Typed three-dimensional positions, coordinate spaces and rigid transformations.",
                java.util.List.of("everything-engine.core"), java.util.List.of("Positions", "Coordinate transforms")),
                ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.galaxy", "Galaxy Core", "1.0",
                "Typed galaxy morphology, position, extent and optional population estimates.",
                java.util.List.of("everything-engine.core", "everything-engine.core.spatial"),
                java.util.List.of("Galaxy models", "Extensible morphology")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.galaxy.structure", "Galactic Structure", "1.0",
                "Galactic structure entities and parametric geometry.",
                java.util.List.of("everything-engine.core.galaxy", "everything-engine.core.spatial"),
                java.util.List.of("Cores", "Disks", "Spiral arms", "Haloes")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.distribution", "Spatial Distribution", "1.0",
                "Seeded uniform, disk, radial and spiral position distributions.",
                java.util.List.of("everything-engine.core.spatial", "everything-engine.core.randomization"),
                java.util.List.of("Uniform volume", "Disk", "Radial", "Spiral")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.stellar", "Stellar Objects", "1.0",
                "Typed stellar systems, stars and physical-property plausibility checks.",
                java.util.List.of("everything-engine.core.galaxy", "everything-engine.core.spatial"),
                java.util.List.of("Stellar systems", "Stars", "Stellar validation")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.galaxy.generator", "Galaxy Generator", "1.0",
                "Seeded structural and population generation with validated atomic Universe registration.",
                java.util.List.of("everything-engine.core.galaxy", "everything-engine.core.galaxy.structure",
                        "everything-engine.core.distribution", "everything-engine.core.stellar",
                        "everything-engine.core.naming"),
                java.util.List.of("Structural generation", "Population generation")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.core.spatial.index", "Spatial Index", "1.0",
                "Rebuildable spatial radius, bounds and nearest-neighbor queries.",
                java.util.List.of("everything-engine.core.spatial"),
                java.util.List.of("Radius queries", "Bounds queries", "Nearest objects")), ModuleStatus.ACTIVE);
        modules.register(new ModuleDescriptor(
                "everything-engine.desktop", "Desktop Application", "1.0",
                "JavaFX development and inspection tools.", java.util.List.of(),
                java.util.List.of("Universe inspection", "Diagnostics")), ModuleStatus.ACTIVE);
        diagnostics.info("Application",
                "Engine Core and generation modules initialized; universe autosave is enabled.");
        if (Files.exists(universeFile)) {
            try {
                universe = universePersistence.load(universeFile);
                diagnostics.info("Persistence", "Loaded saved universe from " + universeFile);
            } catch (IOException | RuntimeException exception) {
                diagnostics.error("Persistence", "Could not load saved universe; starting with an empty universe.",
                        exception);
            }
        }
        if (projectRoot == null) {
            diagnostics.warning("Testing", "No Maven project was found from the current working directory.");
        }
    }

    public Universe universe() {
        return universe;
    }

    public Path universeFile() {
        return universeFile;
    }

    public void saveUniverse(Path file) throws IOException {
        universePersistence.save(universe, file);
    }

    public void loadUniverse(Path file) throws IOException {
        Universe loaded = universePersistence.load(file);
        universePersistence.save(loaded, universeFile);
        universe = loaded;
    }

    public void autosaveUniverse() {
        try {
            universePersistence.save(universe, universeFile);
        } catch (IOException exception) {
            diagnostics.error("Persistence", "Could not automatically save the universe.", exception);
            throw new IllegalStateException("Could not automatically save the universe: "
                    + exception.getMessage(), exception);
        }
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
