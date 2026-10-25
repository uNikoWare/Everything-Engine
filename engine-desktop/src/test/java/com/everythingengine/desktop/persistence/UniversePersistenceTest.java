package com.everythingengine.desktop.persistence;

import com.everythingengine.core.attribute.AttributeKey;
import com.everythingengine.core.entity.BasicEntity;
import com.everythingengine.core.distribution.DiskDistribution;
import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.galaxy.generation.GalaxyGenerationConfig;
import com.everythingengine.core.galaxy.generation.GalaxyGenerationMode;
import com.everythingengine.core.galaxy.generation.GalaxyGenerator;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.stellar.Star;
import com.everythingengine.core.stellar.StellarSystem;
import com.everythingengine.core.universe.Universe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.Optional;

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

    @Test
    void remainsCompatibleWithVersionOneUniverseFiles() throws Exception {
        UUID id = UUID.randomUUID();
        Path file = temporaryDirectory.resolve("legacy.eeu");
        java.nio.file.Files.writeString(file, """
                format.version=1
                entity.count=1
                entity.0.id=%s
                entity.0.name=Legacy
                entity.0.attribute.count=0
                relation.count=0
                """.formatted(id));

        Universe restored = new UniversePersistence().load(file);

        assertEquals("Legacy", restored.findEntity(id).orElseThrow().name());
        assertTrue(restored.findEntity(id).orElseThrow() instanceof BasicEntity);
    }

    @Test
    void roundTripsTypedGalaxyPopulationAndRelationships() throws Exception {
        CoordinateSpace space = new CoordinateSpace("galactic-frame");
        GalaxyGenerationConfig config = new GalaxyGenerationConfig(
                "Persisted Galaxy", GalaxyType.SPIRAL, new Position3D(space, 1, 2, 3),
                GalaxyExtent.spherical(1_000), GalaxyGenerationMode.POPULATION, 4,
                Optional.of(new DiskDistribution(500, 20)), Optional.empty(), true, 77);
        Universe original = new Universe();
        new GalaxyGenerator().generate(config).register(original);

        Path file = temporaryDirectory.resolve("galaxy.eeu");
        UniversePersistence persistence = new UniversePersistence();
        persistence.save(original, file);
        Universe restored = persistence.load(file);

        Galaxy galaxy = restored.entitiesOfType(Galaxy.class).getFirst();
        StellarSystem system = restored.entitiesOfType(StellarSystem.class).getFirst();
        Star star = restored.entitiesOfType(Star.class).getFirst();
        assertEquals("Persisted Galaxy", galaxy.name());
        assertEquals(GalaxyType.SPIRAL, galaxy.type());
        assertEquals(space, galaxy.position().space());
        assertEquals(4, restored.entitiesOfType(StellarSystem.class).size());
        assertEquals(4, restored.entitiesOfType(Star.class).size());
        assertEquals(original.relations().size(), restored.relations().size());
        assertEquals(system.galaxy().id(), galaxy.id());
        assertTrue(restored.relations().stream().anyMatch(relation -> relation.target().id().equals(star.id())));
    }
}
