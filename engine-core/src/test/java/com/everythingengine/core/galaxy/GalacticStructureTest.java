package com.everythingengine.core.galaxy;

import com.everythingengine.core.galaxy.structure.EllipsoidGeometry;
import com.everythingengine.core.galaxy.structure.GalacticStructureType;
import com.everythingengine.core.galaxy.structure.GalaxyStructure;
import com.everythingengine.core.galaxy.structure.SpiralArmGeometry;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GalacticStructureTest {
    @Test
    void supportsDifferentSpatialGeometryWithoutEnforcingATypeHierarchy() {
        CoordinateSpace space = new CoordinateSpace("galaxy");
        Galaxy galaxy = new Galaxy("Galaxy", GalaxyType.ELLIPTICAL, new Position3D(space, 0, 0, 0),
                GalaxyExtent.spherical(1000));
        GalaxyStructure bulge = new GalaxyStructure("Bulge", galaxy, GalacticStructureType.BULGE,
                galaxy.position(), new EllipsoidGeometry(GalaxyExtent.spherical(100)));

        assertEquals(galaxy, bulge.galaxy());
        assertEquals(GalacticStructureType.BULGE, bulge.type());
        assertThrows(IllegalArgumentException.class, () -> new GalaxyStructure("Mismatched", galaxy,
                GalacticStructureType.REGION, new Position3D(new CoordinateSpace("other"), 0, 0, 0),
                new SpiralArmGeometry(1, Math.toRadians(10), 0, 1, 1, 1)));
    }
}
