package com.everythingengine.core.universe;

import com.everythingengine.core.entity.BasicEntity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniverseBatchTest {
    @Test
    void registersEntityAndRelationBatchesTogether() {
        Universe universe = new Universe();
        BasicEntity source = new BasicEntity("Source");
        BasicEntity target = new BasicEntity("Target");

        universe.registerBatch(List.of(source, target),
                List.of(new Relation(source, target, new RelationType("connects"))));

        assertEquals(2, universe.entities().size());
        assertEquals(1, universe.relations().size());
    }

    @Test
    void validatesEveryBatchEndpointBeforeChangingTheUniverse() {
        Universe universe = new Universe();
        BasicEntity included = new BasicEntity("Included");
        BasicEntity missing = new BasicEntity("Missing");
        Relation invalid = new Relation(UUID.randomUUID(), included, missing, new RelationType("invalid"));

        assertThrows(IllegalStateException.class, () -> universe.registerBatch(List.of(included), List.of(invalid)));
        assertEquals(0, universe.entities().size());
        assertEquals(0, universe.relations().size());
    }
}
