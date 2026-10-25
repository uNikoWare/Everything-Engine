# Everything Engine

Modular Java foundation for a science-fiction universe engine, with a Windows-oriented JavaFX desktop workspace for inspection and development. The first release contains no domain-specific universe data, generators, persistence, or dynamically loaded extensions.

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
- `desktop` depends on the public core API only. `DesktopContext` explicitly assembles the in-memory universe, diagnostics, settings, and module registry.

Entity and relation equality uses UUID identity. Duplicate UUIDs and incompatible attribute types are rejected. Relation endpoints must be the exact registered entity objects; self-relations are allowed. Collection results are immutable snapshots. There is no separate `EntityManager`, because `Universe` already owns that responsibility.

The Maven reactor has separate `engine-core` and `engine-desktop` modules. JavaFX is a desktop-only dependency; the core module can be used without UI libraries. Future domain modules can depend on `engine-core` without a dependency on the desktop application.

## Desktop application

The JavaFX workspace includes Dashboard, Universe, Entities, Relations, Modules, Testing, Logs, and Settings views. The entity view can create, rename, remove, and inspect generic entities and typed attributes. Relation records use the core API; entity removal also cleans up its relations. The universe is in-memory only and is never presented as saved.

The module manager lists the two actual running components (core and desktop). There are no domain extension modules in this project. Dynamic JAR loading, module activation/deactivation, and plugin discovery are not implemented. The dark/light theme preference is persisted in the user's application-data directory. Diagnostics use `java.util.logging`.

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

The packaging script requires a JDK installation that provides `jpackage`. It creates an application image under `dist\windows\Everything Engine\`, including the runtime. An MSI installer is not configured. Put a future application icon at `engine-desktop\src\main\resources\icons\` and pass it to `jpackage` once a project icon is available.

## Tests and limitations

Core and desktop-service logic is covered by JUnit 5 without a display server. The GUI is not persisted between runs; there is no database, serialization, generator, graph visualization, localization, module classloader, or full UI automation. Application-created entities are held only for the current process.
