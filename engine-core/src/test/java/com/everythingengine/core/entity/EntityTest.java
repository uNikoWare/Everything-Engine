package com.everythingengine.core.entity;

import com.everythingengine.core.attribute.AttributeKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {
    private static final AttributeKey<Integer> POPULATION = new AttributeKey<>("population", Integer.class);

    @Test
    void identityIsStableAndIndependentOfName() {
        TestEntity first = new TestEntity("Shared name");
        TestEntity second = new TestEntity("Shared name");

        assertNotEquals(first, second);
        assertNotEquals(first.id(), second.id());
        assertEquals(first, new TestEntity(first.id(), "Different name"));
        assertEquals(first.hashCode(), new TestEntity(first.id(), "Different name").hashCode());
    }

    @Test
    void validatesNameAndSupportsChangingIt() {
        TestEntity entity = new TestEntity("Before");

        entity.setName("After");

        assertEquals("After", entity.name());
        assertThrows(IllegalArgumentException.class, () -> entity.setName("  "));
        assertThrows(NullPointerException.class, () -> entity.setName(null));
    }

    @Test
    void managesTypedAttributesAndReportsMissingValues() {
        TestEntity entity = new TestEntity("Entity");

        assertTrue(entity.getAttribute(POPULATION).isEmpty());
        assertFalse(entity.hasAttribute(POPULATION));
        entity.setAttribute(POPULATION, 10);
        entity.setAttribute(POPULATION, 20);

        assertEquals(20, entity.getAttribute(POPULATION).orElseThrow());
        assertTrue(entity.hasAttribute(POPULATION));
        assertTrue(entity.removeAttribute(POPULATION));
        assertFalse(entity.removeAttribute(POPULATION));
    }

    @Test
    void rejectsIncompatibleAttributeKeyAndNullValue() {
        TestEntity entity = new TestEntity("Entity");
        AttributeKey<String> conflictingKey = new AttributeKey<>("population", String.class);

        entity.setAttribute(POPULATION, 10);

        assertThrows(IllegalArgumentException.class, () -> entity.getAttribute(conflictingKey));
        assertThrows(IllegalArgumentException.class, () -> entity.setAttribute(conflictingKey, "many"));
        assertThrows(NullPointerException.class, () -> entity.setAttribute(POPULATION, null));
    }

    @Test
    void exposesImmutableTypedAttributeSnapshots() {
        TestEntity entity = new TestEntity("Entity");
        entity.setAttribute(POPULATION, 10);

        var attributes = entity.attributes();

        assertEquals("population", attributes.getFirst().name());
        assertEquals(Integer.class, attributes.getFirst().type());
        assertEquals(10, attributes.getFirst().value());
        assertThrows(UnsupportedOperationException.class, attributes::clear);
    }

    private static final class TestEntity extends Entity {
        private TestEntity(String name) {
            super(name);
        }

        private TestEntity(UUID id, String name) {
            super(id, name);
        }
    }
}
