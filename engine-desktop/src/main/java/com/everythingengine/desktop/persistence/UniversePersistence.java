package com.everythingengine.desktop.persistence;

import com.everythingengine.core.attribute.AttributeKey;
import com.everythingengine.core.entity.BasicEntity;
import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;
import com.everythingengine.core.universe.Universe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class UniversePersistence {
    private static final String FORMAT_VERSION = "1";
    private static final int MAX_ENTITIES = 100_000;
    private static final int MAX_RELATIONS = 200_000;
    private static final int MAX_ATTRIBUTES_PER_ENTITY = 1_000;

    public void save(Universe universe, Path file) throws IOException {
        Properties properties = new Properties();
        List<Entity> entities = universe.entities();
        properties.setProperty("format.version", FORMAT_VERSION);
        properties.setProperty("entity.count", Integer.toString(entities.size()));

        for (int entityIndex = 0; entityIndex < entities.size(); entityIndex++) {
            Entity entity = entities.get(entityIndex);
            String prefix = "entity." + entityIndex + ".";
            properties.setProperty(prefix + "id", entity.id().toString());
            properties.setProperty(prefix + "name", entity.name());
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
        if (!FORMAT_VERSION.equals(properties.getProperty("format.version"))) {
            throw new IOException("Unsupported or missing universe file format version.");
        }

        Universe universe = new Universe();
        int entityCount = readCount(properties, "entity.count", MAX_ENTITIES);
        List<PendingRelation> pendingRelations = new ArrayList<>();
        for (int entityIndex = 0; entityIndex < entityCount; entityIndex++) {
            String prefix = "entity." + entityIndex + ".";
            BasicEntity entity = new BasicEntity(
                    readUuid(properties, prefix + "id"),
                    required(properties, prefix + "name"));
            int attributeCount = readCount(properties, prefix + "attribute.count", MAX_ATTRIBUTES_PER_ENTITY);
            for (int attributeIndex = 0; attributeIndex < attributeCount; attributeIndex++) {
                String attributePrefix = prefix + "attribute." + attributeIndex + ".";
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
            universe.registerEntity(entity);
        }

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
