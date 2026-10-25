package com.everythingengine.core.stellar;

import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.galaxy.structure.GalacticStructureType;
import com.everythingengine.core.galaxy.structure.GalaxyStructure;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StellarModelsTest {
    @Test
    void supportsSystemsWithMultipleRelatedStarsAndOptionalProperties() {
        CoordinateSpace space = new CoordinateSpace("galaxy");
        Position3D origin = new Position3D(space, 0, 0, 0);
        Galaxy galaxy = new Galaxy("Galaxy", GalaxyType.SPIRAL, origin, GalaxyExtent.spherical(1000));
        GalaxyStructure region = new GalaxyStructure("Arm", galaxy, GalacticStructureType.SPIRAL_ARM,
                origin, new com.everythingengine.core.galaxy.structure.EllipsoidGeometry(
                        GalaxyExtent.spherical(100)));
        StellarSystem system = new StellarSystem("Binary", galaxy, origin, Optional.of(region));
        Star primary = new Star("Primary", OptionalDouble.of(1), OptionalDouble.of(1),
                OptionalDouble.of(1), OptionalDouble.of(5772), Optional.of(SpectralClass.G),
                Optional.of(StellarEvolutionState.MAIN_SEQUENCE), OptionalDouble.of(4.5e9),
                OptionalDouble.of(0));
        Star secondary = new Star("Secondary", OptionalDouble.empty(), OptionalDouble.empty(),
                OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(), OptionalDouble.empty());

        assertEquals(galaxy, system.galaxy());
        assertEquals(2, java.util.List.of(primary, secondary).size());
        assertTrue(secondary.massSolar().isEmpty());
        assertTrue(StellarValidation.validate(primary).isValid());
    }

    @Test
    void rejectsInvalidPhysicalValuesAndReportsUnusualButValidValuesAsWarnings() {
        assertThrows(IllegalArgumentException.class,
                () -> new Star("Bad", OptionalDouble.of(-1), OptionalDouble.empty(), OptionalDouble.empty(),
                        OptionalDouble.empty(), Optional.empty(), Optional.empty(), OptionalDouble.empty(),
                        OptionalDouble.empty()));
        Star unusual = new Star("Unusual", OptionalDouble.of(200), OptionalDouble.empty(),
                OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), Optional.empty(),
                OptionalDouble.empty(), OptionalDouble.of(3));
        assertTrue(StellarValidation.validate(unusual).isValid());
        assertEquals(2, StellarValidation.validate(unusual).warnings().size());
    }
}
