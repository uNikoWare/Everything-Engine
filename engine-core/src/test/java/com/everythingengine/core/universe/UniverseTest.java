package com.everythingengine.core.universe;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UniverseTest {
    private static final RelationType CONNECTS = new RelationType("test.connects");
    private static final RelationType CONTROLS = new RelationType("test.controls");

    @Test
    void registersFindsFiltersAndRemovesEntities() {
        Universe universe = new Universe();
        TestEntity first = new TestEntity("First");
        TestEntity second = new TestEntity("Second");
        OtherEntity other = new OtherEntity("Other");
        universe.registerEntity(first);
        universe.registerEntity(second);
        universe.registerEntity(other);

        assertSame(first, universe.findEntity(first.id()).orElseThrow());
        assertTrue(universe.containsEntity(first));
        assertEquals(2, universe.entitiesOfType(TestEntity.class).size());
        assertThrows(IllegalArgumentException.class, () -> universe.registerEntity(first));
        assertThrows(IllegalArgumentException.class,
                () -> universe.registerEntity(new TestEntity(first.id(), "Duplicate identity")));

        assertTrue(universe.removeEntity(first.id()));
        assertFalse(universe.containsEntity(first));
        assertFalse(universe.removeEntity(first.id()));
    }

    @Test
    void indexesRelationsAndRemovesThemAlongWithEndpoint() {
        Universe universe = new Universe();
        TestEntity source = new TestEntity("Source");
        TestEntity target = new TestEntity("Target");
        universe.registerEntity(source);
        universe.registerEntity(target);
        Relation forward = new Relation(source, target, CONNECTS);
        Relation reverse = new Relation(target, source, CONTROLS);
        universe.addRelation(forward);
        universe.addRelation(reverse);

        assertSame(forward, universe.findRelation(forward.id()).orElseThrow());
        assertEquals(1, universe.relationsFrom(source).size());
        assertEquals(1, universe.relationsTo(source).size());
        assertEquals(2, universe.relationsOf(source).size());
        assertEquals(1, universe.relationsOfType(CONNECTS).size());

        assertTrue(universe.removeEntity(source.id()));
        assertTrue(universe.relations().isEmpty());
        assertTrue(universe.relationsOf(target).isEmpty());
    }

    @Test
    void rejectsRelationsWithUnregisteredEndpointsAndDuplicateIds() {
        Universe universe = new Universe();
        TestEntity source = new TestEntity("Source");
        TestEntity target = new TestEntity("Target");
        universe.registerEntity(source);

        assertThrows(IllegalStateException.class, () -> universe.addRelation(new Relation(source, target, CONNECTS)));
        universe.registerEntity(target);
        Relation relation = new Relation(source, target, CONNECTS);
        universe.addRelation(relation);
        assertThrows(IllegalArgumentException.class, () -> universe.addRelation(relation));
        assertTrue(universe.removeRelation(relation.id()));
        assertFalse(universe.removeRelation(relation.id()));
        assertTrue(universe.relations().isEmpty());
    }

    @Test
    void relationCollectionsAreImmutableSnapshotsAndSelfRelationsAreAllowed() {
        Universe universe = new Universe();
        TestEntity entity = new TestEntity("Entity");
        universe.registerEntity(entity);
        Relation selfRelation = new Relation(entity, entity, CONNECTS);
        universe.addRelation(selfRelation);

        var snapshot = universe.relations();
        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        assertEquals(1, universe.relationsOf(entity).size());
        assertTrue(universe.removeEntity(entity.id()));
        assertTrue(universe.relations().isEmpty());
    }

    private static final class TestEntity extends Entity {
        private TestEntity(String name) {
            super(name);
        }

        private TestEntity(UUID id, String name) {
            super(id, name);
        }
    }

    private static final class OtherEntity extends Entity {
        private OtherEntity(String name) {
            super(name);
        }
    }
}
