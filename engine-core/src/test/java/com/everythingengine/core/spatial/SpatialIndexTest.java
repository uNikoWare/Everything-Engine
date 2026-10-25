package com.everythingengine.core.spatial;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialIndexTest {
    @Test
    void supportsRadiusBoundsNearestAndUpdates() {
        CoordinateSpace space = new CoordinateSpace("space");
        SpatialIndex<String> index = new SpatialIndex<>(space);
        SpatialItem<String> first = item("first", 1, 0, 0, space);
        SpatialItem<String> second = item("second", 4, 0, 0, space);
        index.rebuild(List.of(first, second));

        assertEquals(List.of(first), index.withinRadius(new Position3D(space, 0, 0, 0), 2));
        assertEquals(List.of(first), index.withinBounds(
                new SpatialBounds(new Position3D(space, 0, 0, 0), new Vector3D(2, 1, 1))));
        assertEquals(List.of(first, second), index.nearest(new Position3D(space, 0, 0, 0), 2));

        SpatialItem<String> moved = item("first", 10, 0, 0, space);
        index.upsert(moved);
        assertEquals(List.of(second), index.withinRadius(new Position3D(space, 0, 0, 0), 5));
        assertEquals(moved, index.find(first.id()).orElseThrow());
        assertTrue(index.remove(first.id()));
        assertFalse(index.remove(first.id()));
        assertEquals(1, index.size());
    }

    @Test
    void keepsRebuildAtomicAndRejectsInvalidQueries() {
        CoordinateSpace space = new CoordinateSpace("space");
        SpatialIndex<String> index = new SpatialIndex<>(space);
        SpatialItem<String> existing = item("existing", 0, 0, 0, space);
        index.rebuild(List.of(existing));
        SpatialItem<String> duplicate = new SpatialItem<>(existing.id(), "other",
                new Position3D(space, 1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> index.rebuild(List.of(duplicate, existing)));
        assertEquals(existing, index.find(existing.id()).orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> index.withinRadius(existing.position(), Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> index.nearest(new Position3D(new CoordinateSpace("other"), 0, 0, 0), 1));
    }

    private SpatialItem<String> item(String name, double x, double y, double z, CoordinateSpace space) {
        return new SpatialItem<>(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                name, new Position3D(space, x, y, z));
    }
}
