package com.everythingengine.core.galaxy.generation;

import com.everythingengine.core.distribution.DiskDistribution;
import com.everythingengine.core.distribution.PositionDistribution;
import com.everythingengine.core.distribution.RadialDistribution;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.spatial.CoordinateSpace;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.universe.Universe;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalaxyGeneratorTest {
    private final GalaxyGenerator generator = new GalaxyGenerator();
    private final CoordinateSpace space = new CoordinateSpace("universe");
    private final Position3D origin = new Position3D(space, 0, 0, 0);

    @Test
    void createsAndAtomicallyRegistersAStructuralGalaxy() {
        GalaxyGenerationResult result = generator.generate(new GalaxyGenerationConfig(
                "Andromeda", GalaxyType.SPIRAL, origin, GalaxyExtent.spherical(100_000),
                GalaxyGenerationMode.STRUCTURAL, 0, Optional.empty(), Optional.empty(), false, 101));
        Universe universe = new Universe();

        assertTrue(result.isValid());
        assertTrue(result.systems().isEmpty());
        assertEquals(5, result.structures().size());
        result.register(universe);
        assertEquals(6, universe.entities().size());
        assertEquals(5, universe.relations().size());
    }

    @Test
    void populationGenerationIsSeededAndCanCreateStars() {
        GalaxyGenerationConfig config = new GalaxyGenerationConfig(
                "Seeded", GalaxyType.SPIRAL, origin, GalaxyExtent.spherical(1000),
                GalaxyGenerationMode.POPULATION, 40, Optional.of(new DiskDistribution(500, 40)),
                Optional.empty(), true, 2026);

        GalaxyGenerationResult first = generator.generate(config);
        GalaxyGenerationResult second = generator.generate(config);

        assertTrue(first.isValid());
        assertEquals(first.galaxy().orElseThrow().id(), second.galaxy().orElseThrow().id());
        assertEquals(40, first.systems().size());
        assertEquals(40, first.stars().size());
        for (int index = 0; index < first.systems().size(); index++) {
            assertEquals(first.systems().get(index).id(), second.systems().get(index).id());
            assertEquals(first.systems().get(index).name(), second.systems().get(index).name());
            assertEquals(first.systems().get(index).position(), second.systems().get(index).position());
        }
        assertEquals(first.relations().size(), second.relations().size());
    }

    @Test
    void failedPopulationDoesNotRegisterPartialObjects() {
        PositionDistribution outsideExtent = (random, center) -> new Position3D(
                center.space(), center.x() + 500, center.y(), center.z());
        GalaxyGenerationConfig config = new GalaxyGenerationConfig(
                "Rejected", GalaxyType.ELLIPTICAL, origin, GalaxyExtent.spherical(100),
                GalaxyGenerationMode.POPULATION, 3, Optional.of(outsideExtent),
                Optional.empty(), false, 19);
        GalaxyGenerationResult result = generator.generate(config);
        Universe universe = new Universe();

        assertFalse(result.isValid());
        assertTrue(result.galaxy().isEmpty());
        assertEquals(0, universe.entities().size());
        assertEquals(0, universe.relations().size());
    }

    @Test
    void generatesAThousandExplicitSystemsWithoutMaterializingTheEstimatedStarPopulation() {
        GalaxyGenerationResult result = generator.generate(new GalaxyGenerationConfig(
                "Large Sample", GalaxyType.ELLIPTICAL, origin, GalaxyExtent.spherical(20_000),
                GalaxyGenerationMode.POPULATION, 1_000, Optional.of(new RadialDistribution(10_000, 2)),
                Optional.empty(), false, 88));

        assertTrue(result.isValid());
        assertEquals(1_000, result.systems().size());
        assertTrue(result.stars().isEmpty());
    }
}
