package com.everythingengine.core.distribution;

import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.galaxy.structure.SpiralArmGeometry;
import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionDistributionTest {
    @Test
    void samplesWithinConfiguredRegionsAndReproducesSeeds() {
        CoordinateSpace space = new CoordinateSpace("galaxy");
        Position3D center = new Position3D(space, 10, 20, 30);
        UniformEllipsoidDistribution ellipsoid =
                new UniformEllipsoidDistribution(new GalaxyExtent(100, 50, 10));
        DiskDistribution disk = new DiskDistribution(100, 20);
        RadialDistribution radial = new RadialDistribution(100, 2);

        SeededRandom first = new SeededRandom(42);
        SeededRandom second = new SeededRandom(42);
        SeededRandom diskRandom = new SeededRandom(17);
        SeededRandom radialRandom = new SeededRandom(18);
        for (int index = 0; index < 100; index++) {
            Position3D ellipsoidPosition = ellipsoid.sample(first, center);
            assertTrue(new GalaxyExtent(100, 50, 10).containsOffset(
                    new com.everythingengine.core.spatial.Vector3D(
                            ellipsoidPosition.x() - center.x(),
                            ellipsoidPosition.y() - center.y(),
                            ellipsoidPosition.z() - center.z())));
            assertEquals(ellipsoidPosition, ellipsoid.sample(second, center));

            Position3D diskPosition = disk.sample(diskRandom, center);
            assertTrue(Math.hypot(diskPosition.x() - center.x(), diskPosition.y() - center.y()) <= 100);
            assertTrue(Math.abs(diskPosition.z() - center.z()) <= 10);

            Position3D radialPosition = radial.sample(radialRandom, center);
            assertTrue(center.distanceTo(radialPosition) <= 100);
        }
    }

    @Test
    void samplesAroundAParametricSpiralAndRejectsInvalidConfiguration() {
        CoordinateSpace space = new CoordinateSpace("galaxy");
        Galaxy galaxy = new Galaxy("Spiral", GalaxyType.SPIRAL, new Position3D(space, 4, 5, 6),
                GalaxyExtent.spherical(1000));
        SpiralArmGeometry arm = new SpiralArmGeometry(10, Math.toRadians(12), 0, 1.5, 4, 2);
        SpiralDistribution distribution = new SpiralDistribution(arm);

        Position3D sample = distribution.sample(new SeededRandom(11), galaxy.position());

        assertEquals(space, sample.space());
        assertTrue(sample.distanceTo(galaxy.position()) >= 0);
        assertThrows(IllegalArgumentException.class, () -> new DiskDistribution(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> new RadialDistribution(1, -1));
        assertThrows(IllegalArgumentException.class,
                () -> arm.centerlineAt(galaxy, Double.NaN));
    }
}
