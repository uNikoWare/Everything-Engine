package com.everythingengine.core.galaxy;

import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GalaxyTest {
    @Test
    void supportsAnUnpopulatedGalaxyWithOptionalEstimates() {
        CoordinateSpace space = new CoordinateSpace("universe");
        Galaxy galaxy = new Galaxy("Test Galaxy", GalaxyType.SPIRAL,
                new Position3D(space, 1, 2, 3), GalaxyExtent.spherical(100_000));

        assertEquals("Test Galaxy", galaxy.name());
        assertEquals(GalaxyType.SPIRAL, galaxy.type());
        assertTrue(galaxy.ageYears().isEmpty());
        assertTrue(galaxy.estimatedStarCount().isEmpty());
    }

    @Test
    void supportsExtensibleTypesAndValidatesOptionalEstimates() {
        CoordinateSpace space = new CoordinateSpace("universe");
        Galaxy galaxy = new Galaxy("Custom", GalaxyType.custom("ring"), new Position3D(space, 0, 0, 0),
                GalaxyExtent.spherical(2), OptionalDouble.of(1.0e9), OptionalDouble.empty(),
                OptionalLong.of(5_000_000_000L));

        assertEquals("ring", galaxy.type().key());
        assertEquals(5_000_000_000L, galaxy.estimatedStarCount().orElseThrow());
        assertThrows(IllegalArgumentException.class,
                () -> new Galaxy("Bad", GalaxyType.ELLIPTICAL, new Position3D(space, 0, 0, 0),
                        GalaxyExtent.spherical(2), OptionalDouble.of(Double.NaN), OptionalDouble.empty(),
                        OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> GalaxyExtent.spherical(0));
    }
}
