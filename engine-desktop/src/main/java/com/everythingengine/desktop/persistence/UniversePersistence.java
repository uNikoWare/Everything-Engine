package com.everythingengine.desktop.persistence;

import com.everythingengine.core.attribute.AttributeKey;
import com.everythingengine.core.entity.BasicEntity;
import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.galaxy.structure.EllipsoidGeometry;
import com.everythingengine.core.galaxy.structure.GalacticStructureType;
import com.everythingengine.core.galaxy.structure.GalaxyStructure;
import com.everythingengine.core.galaxy.structure.SpiralArmGeometry;
import com.everythingengine.core.galaxy.structure.StructureGeometry;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.stellar.SpectralClass;
import com.everythingengine.core.stellar.Star;
import com.everythingengine.core.stellar.StellarEvolutionState;
import com.everythingengine.core.stellar.StellarObject;
import com.everythingengine.core.stellar.StellarSystem;
import com.everythingengine.core.universe.Universe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.UUID;

public final class UniversePersistence {
    private static final String FORMAT_VERSION = "2";
    private static final int MAX_ENTITIES = 100_000;
    private static final int MAX_RELATIONS = 200_000;
    private static final int MAX_ATTRIBUTES_PER_ENTITY = 1_000;

    public void save(Universe universe, Path file) throws IOException {
        Properties properties = new Properties();
        List<Entity> entities = new ArrayList<>(universe.entities());
        entities.sort(Comparator.comparingInt(UniversePersistence::domainTypeOrder));
        properties.setProperty("format.version", FORMAT_VERSION);
        properties.setProperty("entity.count", Integer.toString(entities.size()));

        for (int entityIndex = 0; entityIndex < entities.size(); entityIndex++) {
            Entity entity = entities.get(entityIndex);
            String prefix = "entity." + entityIndex + ".";
            properties.setProperty(prefix + "id", entity.id().toString());
            properties.setProperty(prefix + "name", entity.name());
            writeDomainEntity(properties, prefix, entity);
            var attributes = entity.attributes();
            properties.setProperty(prefix + "attribute.count", Integer.toString(attributes.size()));
            for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
                var attribute = attributes.get(attributeIndex);
                String attributePrefix = prefix + "attribute." + attributeIndex + ".";
                properties.setProperty(attributePrefix + "name", attribute.name());
                properties.setProperty(attributePrefix + "type", supportedType(attribute.type()).getSimpleName());
                properties.setProperty(attributePrefix + "value", attribute.value().toString());
            }
        }

        List<Relation> relations = universe.relations();
        properties.setProperty("relation.count", Integer.toString(relations.size()));
        for (int relationIndex = 0; relationIndex < relations.size(); relationIndex++) {
            Relation relation = relations.get(relationIndex);
            String prefix = "relation." + relationIndex + ".";
            properties.setProperty(prefix + "id", relation.id().toString());
            properties.setProperty(prefix + "source", relation.source().id().toString());
            properties.setProperty(prefix + "target", relation.target().id().toString());
            properties.setProperty(prefix + "type", relation.type().key());
        }

        Path absoluteFile = file.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        if (parent == null) {
            throw new IOException("Universe file must have a parent directory.");
        }
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "universe-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "Everything Engine universe");
            }
            try {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public Universe load(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        String version = properties.getProperty("format.version");
        if (!FORMAT_VERSION.equals(version) && !"1".equals(version)) {
            throw new IOException("Unsupported or missing universe file format version.");
        }

        Universe universe = new Universe();
        int entityCount = readCount(properties, "entity.count", MAX_ENTITIES);
        for (int entityIndex = 0; entityIndex < entityCount; entityIndex++) {
            String prefix = "entity." + entityIndex + ".";
            UUID id = readUuid(properties, prefix + "id");
            String name = required(properties, prefix + "name");
            Entity entity = "1".equals(version)
                    ? new BasicEntity(id, name)
                    : readDomainEntity(properties, prefix, id, name, universe);
            readAttributes(properties, prefix, entity);
            universe.registerEntity(entity);
        }

        List<PendingRelation> pendingRelations = new ArrayList<>();
        int relationCount = readCount(properties, "relation.count", MAX_RELATIONS);
        for (int relationIndex = 0; relationIndex < relationCount; relationIndex++) {
            String prefix = "relation." + relationIndex + ".";
            pendingRelations.add(new PendingRelation(
                    readUuid(properties, prefix + "id"),
                    readUuid(properties, prefix + "source"),
                    readUuid(properties, prefix + "target"),
                    required(properties, prefix + "type")));
        }
        for (PendingRelation relation : pendingRelations) {
            Entity source = universe.findEntity(relation.source()).orElseThrow(
                    () -> new IOException("Relation refers to a missing source entity."));
            Entity target = universe.findEntity(relation.target()).orElseThrow(
                    () -> new IOException("Relation refers to a missing target entity."));
            universe.addRelation(new Relation(
                    relation.id(), source, target, new RelationType(relation.type())));
        }
        return universe;
    }

    private static int domainTypeOrder(Entity entity) {
        if (entity instanceof Galaxy) return 0;
        if (entity instanceof GalaxyStructure) return 1;
        if (entity instanceof StellarSystem) return 2;
        if (entity instanceof StellarObject) return 3;
        return 4;
    }

    private static void writeDomainEntity(Properties properties, String prefix, Entity entity) throws IOException {
        if (entity instanceof Galaxy galaxy) {
            properties.setProperty(prefix + "domain.type", "galaxy");
            properties.setProperty(prefix + "galaxy.type", galaxy.type().key());
            writePosition(properties, prefix, galaxy.position());
            writeExtent(properties, prefix + "extent.", galaxy.extent());
            putOptionalDouble(properties, prefix + "galaxy.ageYears", galaxy.ageYears());
            putOptionalDouble(properties, prefix + "galaxy.massKilograms", galaxy.estimatedStellarMassKilograms());
            putOptionalLong(properties, prefix + "galaxy.starCount", galaxy.estimatedStarCount());
        } else if (entity instanceof GalaxyStructure structure) {
            properties.setProperty(prefix + "domain.type", "galaxy-structure");
            properties.setProperty(prefix + "structure.galaxy", structure.galaxy().id().toString());
            properties.setProperty(prefix + "structure.type", structure.type().key());
            writePosition(properties, prefix, structure.position());
            if (structure.geometry() instanceof EllipsoidGeometry ellipsoid) {
                properties.setProperty(prefix + "geometry.type", "ellipsoid");
                writeExtent(properties, prefix + "geometry.extent.", ellipsoid.extent());
            } else if (structure.geometry() instanceof SpiralArmGeometry spiral) {
                properties.setProperty(prefix + "geometry.type", "spiral-arm");
                properties.setProperty(prefix + "geometry.innerRadius", Double.toString(spiral.innerRadiusParsecs()));
                properties.setProperty(prefix + "geometry.pitch", Double.toString(spiral.pitchAngleRadians()));
                properties.setProperty(prefix + "geometry.startAngle", Double.toString(spiral.startAngleRadians()));
                properties.setProperty(prefix + "geometry.turns", Double.toString(spiral.turns()));
                properties.setProperty(prefix + "geometry.verticalScale",
                        Double.toString(spiral.verticalScaleParsecs()));
                properties.setProperty(prefix + "geometry.width", Double.toString(spiral.widthParsecs()));
            } else {
                throw new IOException("Unsupported galactic structure geometry: "
                        + structure.geometry().getClass().getName());
            }
        } else if (entity instanceof StellarSystem system) {
            properties.setProperty(prefix + "domain.type", "stellar-system");
            properties.setProperty(prefix + "system.galaxy", system.galaxy().id().toString());
            writePosition(properties, prefix, system.position());
            system.region().ifPresent(region ->
                    properties.setProperty(prefix + "system.region", region.id().toString()));
        } else if (entity instanceof Star star) {
            properties.setProperty(prefix + "domain.type", "star");
            putOptionalDouble(properties, prefix + "star.massSolar", star.massSolar());
            putOptionalDouble(properties, prefix + "star.radiusSolar", star.radiusSolar());
            putOptionalDouble(properties, prefix + "star.luminositySolar", star.luminositySolar());
            putOptionalDouble(properties, prefix + "star.temperatureKelvin", star.effectiveTemperatureKelvin());
            putOptionalDouble(properties, prefix + "star.ageYears", star.ageYears());
            putOptionalDouble(properties, prefix + "star.metallicityFeH", star.metallicityFeH());
            star.spectralClass().ifPresent(value ->
                    properties.setProperty(prefix + "star.spectralClass", value.key()));
            star.evolutionState().ifPresent(value ->
                    properties.setProperty(prefix + "star.evolutionState", value.key()));
        } else if (entity instanceof StellarObject) {
            throw new IOException("Unsupported stellar object type: " + entity.getClass().getName());
        } else {
            properties.setProperty(prefix + "domain.type", "basic");
        }
    }

    private static Entity readDomainEntity(Properties properties, String prefix, UUID id, String name,
            Universe universe) throws IOException {
        String type = properties.getProperty(prefix + "domain.type", "basic");
        try {
            return switch (type) {
                case "basic" -> new BasicEntity(id, name);
                case "galaxy" -> new Galaxy(id, name, new GalaxyType(required(properties, prefix + "galaxy.type")),
                        readPosition(properties, prefix), readExtent(properties, prefix + "extent."),
                        optionalDouble(properties, prefix + "galaxy.ageYears"),
                        optionalDouble(properties, prefix + "galaxy.massKilograms"),
                        optionalLong(properties, prefix + "galaxy.starCount"));
                case "galaxy-structure" -> {
                    Galaxy galaxy = requireEntity(universe, readUuid(properties, prefix + "structure.galaxy"),
                            Galaxy.class);
                    StructureGeometry geometry = switch (required(properties, prefix + "geometry.type")) {
                        case "ellipsoid" -> new EllipsoidGeometry(readExtent(properties, prefix + "geometry.extent."));
                        case "spiral-arm" -> new SpiralArmGeometry(
                                readDouble(properties, prefix + "geometry.innerRadius"),
                                readDouble(properties, prefix + "geometry.pitch"),
                                readDouble(properties, prefix + "geometry.startAngle"),
                                readDouble(properties, prefix + "geometry.turns"),
                                readDouble(properties, prefix + "geometry.verticalScale"),
                                readDouble(properties, prefix + "geometry.width"));
                        default -> throw new IOException("Unsupported persisted structure geometry.");
                    };
                    yield new GalaxyStructure(id, name, galaxy,
                            new GalacticStructureType(required(properties, prefix + "structure.type")),
                            readPosition(properties, prefix), geometry);
                }
                case "stellar-system" -> {
                    Galaxy galaxy = requireEntity(universe, readUuid(properties, prefix + "system.galaxy"),
                            Galaxy.class);
                    Optional<GalaxyStructure> region = properties.containsKey(prefix + "system.region")
                            ? Optional.of(requireEntity(universe,
                                    readUuid(properties, prefix + "system.region"), GalaxyStructure.class))
                            : Optional.empty();
                    yield new StellarSystem(id, name, galaxy, readPosition(properties, prefix), region);
                }
                case "star" -> new Star(id, name,
                        optionalDouble(properties, prefix + "star.massSolar"),
                        optionalDouble(properties, prefix + "star.radiusSolar"),
                        optionalDouble(properties, prefix + "star.luminositySolar"),
                        optionalDouble(properties, prefix + "star.temperatureKelvin"),
                        optional(properties, prefix + "star.spectralClass").map(SpectralClass::new),
                        optional(properties, prefix + "star.evolutionState").map(StellarEvolutionState::new),
                        optionalDouble(properties, prefix + "star.ageYears"),
                        optionalDouble(properties, prefix + "star.metallicityFeH"));
                default -> throw new IOException("Unsupported persisted entity type: " + type);
            };
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid persisted domain entity '" + name + "'.", exception);
        }
    }

    private static void readAttributes(Properties properties, String prefix, Entity entity) throws IOException {
        int count = readCount(properties, prefix + "attribute.count", MAX_ATTRIBUTES_PER_ENTITY);
        for (int index = 0; index < count; index++) {
            String attributePrefix = prefix + "attribute." + index + ".";
            String name = required(properties, attributePrefix + "name");
            String type = required(properties, attributePrefix + "type");
            String value = required(properties, attributePrefix + "value");
            try {
                switch (type) {
                    case "String" -> entity.setAttribute(new AttributeKey<>(name, String.class), value);
                    case "Integer" -> entity.setAttribute(
                            new AttributeKey<>(name, Integer.class), Integer.valueOf(value));
                    case "Double" -> entity.setAttribute(
                            new AttributeKey<>(name, Double.class), Double.valueOf(value));
                    case "Boolean" -> {
                        if (!value.equals("true") && !value.equals("false")) {
                            throw new IllegalArgumentException("Invalid boolean value");
                        }
                        entity.setAttribute(new AttributeKey<>(name, Boolean.class), Boolean.valueOf(value));
                    }
                    default -> throw new IOException("Unsupported attribute type: " + type);
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid attribute value for '" + name + "'.", exception);
            }
        }
    }

    private static void writePosition(Properties properties, String prefix, Position3D position) {
        properties.setProperty(prefix + "position.space.id", position.space().id().toString());
        properties.setProperty(prefix + "position.space.name", position.space().name());
        properties.setProperty(prefix + "position.x", Double.toString(position.x()));
        properties.setProperty(prefix + "position.y", Double.toString(position.y()));
        properties.setProperty(prefix + "position.z", Double.toString(position.z()));
    }

    private static Position3D readPosition(Properties properties, String prefix) throws IOException {
        CoordinateSpace space = new CoordinateSpace(readUuid(properties, prefix + "position.space.id"),
                required(properties, prefix + "position.space.name"));
        return new Position3D(space, readDouble(properties, prefix + "position.x"),
                readDouble(properties, prefix + "position.y"), readDouble(properties, prefix + "position.z"));
    }

    private static void writeExtent(Properties properties, String prefix, GalaxyExtent extent) {
        properties.setProperty(prefix + "x", Double.toString(extent.radiusXParsecs()));
        properties.setProperty(prefix + "y", Double.toString(extent.radiusYParsecs()));
        properties.setProperty(prefix + "z", Double.toString(extent.radiusZParsecs()));
    }

    private static GalaxyExtent readExtent(Properties properties, String prefix) throws IOException {
        return new GalaxyExtent(readDouble(properties, prefix + "x"),
                readDouble(properties, prefix + "y"), readDouble(properties, prefix + "z"));
    }

    private static <T extends Entity> T requireEntity(Universe universe, UUID id, Class<T> type) throws IOException {
        Entity entity = universe.findEntity(id)
                .orElseThrow(() -> new IOException("Domain entity refers to a missing entity."));
        if (!type.isInstance(entity)) {
            throw new IOException("Domain entity reference has an incompatible type.");
        }
        return type.cast(entity);
    }

    private static void putOptionalDouble(Properties properties, String key, OptionalDouble value) {
        if (value.isPresent()) {
            properties.setProperty(key, Double.toString(value.getAsDouble()));
        }
    }

    private static void putOptionalLong(Properties properties, String key, OptionalLong value) {
        if (value.isPresent()) {
            properties.setProperty(key, Long.toString(value.getAsLong()));
        }
    }

    private static OptionalDouble optionalDouble(Properties properties, String key) throws IOException {
        return properties.containsKey(key) ? OptionalDouble.of(readDouble(properties, key)) : OptionalDouble.empty();
    }

    private static OptionalLong optionalLong(Properties properties, String key) throws IOException {
        if (!properties.containsKey(key)) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(properties.getProperty(key)));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer value for '" + key + "'.", exception);
        }
    }

    private static Optional<String> optional(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    private static double readDouble(Properties properties, String key) throws IOException {
        try {
            return Double.parseDouble(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid numeric value for '" + key + "'.", exception);
        }
    }

    private static Class<?> supportedType(Class<?> type) throws IOException {
        if (type == String.class || type == Integer.class || type == Double.class || type == Boolean.class) {
            return type;
        }
        throw new IOException("Unsupported attribute type: " + type.getName());
    }

    private static int readCount(Properties properties, String key, int maximum) throws IOException {
        try {
            int count = Integer.parseInt(required(properties, key));
            if (count < 0 || count > maximum) {
                throw new IOException("Invalid count for " + key + ".");
            }
            return count;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid count for " + key + ".", exception);
        }
    }

    private static UUID readUuid(Properties properties, String key) throws IOException {
        try {
            return UUID.fromString(required(properties, key));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid UUID for " + key + ".", exception);
        }
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("Missing required universe field: " + key);
        }
        return value;
    }

    private record PendingRelation(UUID id, UUID source, UUID target, String type) {
    }
}
