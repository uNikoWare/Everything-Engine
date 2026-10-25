package com.everythingengine.core.distribution;

import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.spatial.Position3D;

/**
 * Generates positions without creating or registering domain entities.
 */
@FunctionalInterface
public interface PositionDistribution {
    Position3D sample(SeededRandom random, Position3D center);
}
