package com.everythingengine.core.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinateSystemTest {
    @Test
    void computesDistanceAndDirectionInTheSameSpace() {
        CoordinateSpace space = new CoordinateSpace("galaxy");
        Position3D origin = new Position3D(space, 0, 0, 0);
        Position3D point = new Position3D(space, 3, 4, 0);

        assertEquals(5, origin.distanceTo(point), 1.0e-12);
        assertEquals(0.6, origin.directionTo(point).x(), 1.0e-12);
        assertEquals(0.8, origin.directionTo(point).y(), 1.0e-12);
    }

    @Test
    void rejectsInvalidValuesAndCrossSpaceComparisons() {
        CoordinateSpace first = new CoordinateSpace("first");
        CoordinateSpace second = new CoordinateSpace("second");

        assertThrows(IllegalArgumentException.class, () -> new Position3D(first, Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Direction3D(2, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Position3D(first, 0, 0, 0).distanceTo(new Position3D(second, 0, 0, 0)));
    }

    @Test
    void transformsAndInvertsPositionsAcrossSpaces() {
        CoordinateSpace source = new CoordinateSpace("source");
        CoordinateSpace target = new CoordinateSpace("target");
        CoordinateTransform transform = new CoordinateTransform(source, target,
                new double[][] {{0, -1, 0}, {1, 0, 0}, {0, 0, 1}},
                new Vector3D(10, 20, 30));
        Position3D original = new Position3D(source, 2, 3, 4);

        Position3D transformed = transform.transform(original);

        assertEquals(new Position3D(target, 7, 22, 34), transformed);
        assertEquals(original, transform.inverse().transform(transformed));
    }

    @Test
    void rejectsImproperRotationMatricesAndWrongSourceSpace() {
        CoordinateSpace source = new CoordinateSpace("source");
        CoordinateSpace target = new CoordinateSpace("target");
        assertThrows(IllegalArgumentException.class,
                () -> new CoordinateTransform(source, target,
                        new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, -1}}, new Vector3D(0, 0, 0)));

        CoordinateTransform transform = CoordinateTransform.translation(source, target, new Vector3D(0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> transform.transform(new Position3D(target, 0, 0, 0)));
    }
}
