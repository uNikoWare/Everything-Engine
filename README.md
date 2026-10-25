# Everything Engine

Modular Java foundation for a science-fiction universe engine, with a Windows-oriented JavaFX desktop workspace for inspection and development. The desktop includes core name-generation examples and local universe persistence; it does not load dynamic extensions.

## Architecture

```text
engine-desktop  ───────→  engine-core
 JavaFX views             Entities
 Application services    Attributes
 Explicit modules        Relations
```

- `core.entity.Entity` has an immutable UUID, a validated mutable name, and typed attributes. `BasicEntity` is the general-purpose entity used by the desktop editor; domain modules can provide their own subtypes.
- `core.attribute.AttributeKey<T>` couples a stable key to a Java value type. `Entity.attributes()` exposes read-only attribute snapshots for inspection. Values are held by reference, so immutable values are recommended.
- `core.relation.Relation` is an immutable directed edge with a UUID, source, target, and extensible `RelationType` key.
- `core.universe.Universe` owns entities and relations. Its UUID maps and source, target, and type indexes support lookups; removing an entity also removes all connected relations.
- `core.random.*` adds deterministic seeded generation, weighted-choice selection, and reproducible helpers for data generation and tests.
- `core.validation.*` provides reusable validation rules, issue aggregation, and warnings/errors reporting for core and future domain logic.
- `core.name.*` contains profile-driven name generation with syllable, word, and template strategies, plus duplicate-avoidance support.
- `desktop` depends on the public core API only. `DesktopContext` explicitly assembles the in-memory universe, diagnostics, settings, and module registry.

Entity and relation equality uses UUID identity. Duplicate UUIDs and incompatible attribute types are rejected. Relation endpoints must be the exact registered entity objects; self-relations are allowed. Collection results are immutable snapshots. There is no separate `EntityManager`, because `Universe` already owns that responsibility.

The Maven reactor has separate `engine-core` and `engine-desktop` modules. JavaFX is a desktop-only dependency; the core module can be used without UI libraries. Future domain modules can depend on `engine-core` without a dependency on the desktop application.

## Desktop application

The JavaFX workspace includes Dashboard, Universe, Entities, Relations, Modules, Name Generation, Testing, Logs, and Settings views. The interface supports English and German, with English as the default; choose the language in **Settings**. The entity view can create, rename, remove, and inspect generic entities and typed attributes. Relation records use the core API; entity removal also cleans up its relations.

Universe changes are automatically saved to `universe.eeu` in the user's application-data directory (`%APPDATA%\EverythingEngine` on Windows). The Universe view also provides **Save Universe** and **Load Universe** for manually exporting or importing `.eeu` files. Files preserve entity and relation UUIDs, names, and String, Integer, Double, and Boolean attributes. Loading a file replaces the current universe and updates the automatic save. Settings (including language and theme) are also persisted in the user's application-data directory.

The module manager lists the built-in core and desktop components and the generation-related core modules (randomization, validation, and naming). There are no dynamically loaded extension modules in this project. Dynamic JAR loading, module activation/deactivation, and plugin discovery are not implemented. Diagnostics use `java.util.logging`.

The Testing Center invokes only the fixed `mvn -B test` goal, asynchronously. It requires a Maven project directory to be discoverable from the app's working directory and Maven installed on `PATH`; test totals are read from Surefire reports. If either prerequisite is missing, testing reports an error rather than fabricated results.

## Requirements and commands

- JDK 25 (LTS), including JavaFX-compatible Windows runtime support.
- Maven 3.9 or later.

Run the desktop application during development:

```sh
mvn -pl engine-desktop -am install
mvn -pl engine-desktop javafx:run
```

Compile/package both modules and execute their tests:

```sh
mvn clean package
mvn test
```

On Windows, the equivalent convenience scripts are `scripts\build.bat` and `scripts\test.bat`. Build a self-contained Windows application image with:

```bat
scripts\package-windows.bat
```

The packaging script requires a JDK installation that provides `jpackage`. It creates an application image under `dist\windows\Everything Engine\`, including the runtime. To create a per-user Windows MSI installer with Start Menu and desktop shortcuts, install WiX Toolset 3.14.1 and run:

```bat
scripts\package-windows-installer.bat
```

The MSI is written to `dist\windows\`. Its stable upgrade UUID allows a later MSI release to upgrade the installed application. Set the app version in `desktop.app.version` in the root `pom.xml` before each release.
The supplied Everything Engine logo appears on the dashboard; its matching icon is used by the app window, packaged application, and installer.

### Version scheme

The canonical version is `v.a.b.c` with an optional development-branch suffix `_d`, for example `v.0.1.1` or `v.0.1.1_a`. The current version is **`v.0.1.1`**. The canonical value is maintained in the root `pom.xml` as `desktop.app.version`; the app displays it and the update manifest uses the same format. Windows packaging uses numeric version `(a + 1).b.c` and omits the branch suffix: this keeps Windows Installer upgrades monotonic and allows the new `v.0.1.1` package to upgrade an earlier `1.0.0` MSI. The `jpackage` version is an installer compatibility value, not the version shown inside the app.

Version components are increased from left to right: `a` starts at 0 and increases only when the engine is ready for release; `b` starts at 0 and increases for foundational features that change the app's structure; `c` starts at 0 and increases for other changes. When a component increases, every component to its right resets to 0. Use `_a` through `_z` only when development branches need distinct version labels; a branch suffix does not reset when numeric components change. For versions with the same numeric components, branch letters sort alphabetically and an unsuffixed version sorts after branch builds.

### In-app updates

In **Settings**, enter and save the HTTPS URL of a trusted update manifest, then select **Check for Updates**. When a newer version is available, the app downloads its MSI, verifies the SHA-256 checksum, closes, applies the per-user upgrade, and restarts. The updater rejects non-HTTPS URLs and invalid manifests; updates are never installed without a valid checksum.

Host the MSI and a UTF-8 Java-properties manifest at an HTTPS location. Publish the MSI first, then publish a manifest such as:

```properties
version=v.0.1.2
installerUrl=https://downloads.example.org/Everything-Engine-v.0.1.2.msi
sha256=64-character-lowercase-or-uppercase-SHA-256-hex-digest
releaseNotes=What changed in this release
```

Replace the checksum placeholder with the 64-character SHA-256 digest of the exact MSI at `installerUrl`. Keep the manifest URL configurable in each installation; this project does not provide or host a release feed. Automatic MSI installation is available in the packaged Windows app, not when running from Maven or a non-Windows development environment.

## Tests and limitations

Core and desktop-service logic is covered by JUnit 5 without a display server. Universe files use a versioned local format and are not synchronized or encrypted. There is no database, graph visualization, module classloader, or full UI automation.
