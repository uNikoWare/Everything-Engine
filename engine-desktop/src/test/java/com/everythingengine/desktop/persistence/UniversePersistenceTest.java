package com.everythingengine.desktop.persistence;

import com.everythingengine.core.attribute.AttributeKey;
import com.everythingengine.core.entity.BasicEntity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;
import com.everythingengine.core.universe.Universe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversePersistenceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndLoadsEntitiesAttributesAndRelations() throws Exception {
        Universe original = new Universe();
        BasicEntity source = new BasicEntity(UUID.randomUUID(), "Sol");
        source.setAttribute(new AttributeKey<>("classification", String.class), "star");
        source.setAttribute(new AttributeKey<>("inhabited", Boolean.class), false);
        BasicEntity target = new BasicEntity(UUID.randomUUID(), "Earth");
        target.setAttribute(new AttributeKey<>("population", Integer.class), 8_000_000);
        original.registerEntity(source);
        original.registerEntity(target);
        Relation relation = new Relation(source, target, new RelationType("contains"));
        original.addRelation(relation);

        Path file = temporaryDirectory.resolve("test.eeu");
        UniversePersistence persistence = new UniversePersistence();
        persistence.save(original, file);
        Universe restored = persistence.load(file);

        assertEquals(2, restored.entities().size());
        assertEquals(1, restored.relations().size());
        var restoredSource = restored.findEntity(source.id()).orElseThrow();
        assertEquals("Sol", restoredSource.name());
        assertEquals("star", restoredSource.getAttribute(new AttributeKey<>("classification", String.class))
                .orElseThrow());
        assertEquals(false, restoredSource.getAttribute(new AttributeKey<>("inhabited", Boolean.class))
                .orElseThrow());
        assertEquals(target.id(), restored.relations().getFirst().target().id());
        assertEquals(relation.id(), restored.relations().getFirst().id());
        assertEquals("contains", restored.relations().getFirst().type().key());
        assertEquals(8_000_000, restored.findEntity(target.id()).orElseThrow()
                .getAttribute(new AttributeKey<>("population", Integer.class)).orElseThrow());
    }

    @Test
    void rejectsUnsupportedFileVersions() throws Exception {
        Path file = temporaryDirectory.resolve("unsupported.eeu");
        java.nio.file.Files.writeString(file, "format.version=999\n");

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                java.io.IOException.class, () -> new UniversePersistence().load(file));
        assertTrue(exception.getMessage().contains("Unsupported"));
    }
}
