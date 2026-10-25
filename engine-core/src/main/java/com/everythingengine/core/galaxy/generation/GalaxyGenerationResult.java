package com.everythingengine.core.galaxy.generation;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.structure.GalaxyStructure;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.stellar.Star;
import com.everythingengine.core.stellar.StellarSystem;
import com.everythingengine.core.universe.Universe;
import com.everythingengine.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

public record GalaxyGenerationResult(
        Optional<Galaxy> galaxy,
        List<GalaxyStructure> structures,
        List<StellarSystem> systems,
        List<Star> stars,
        List<Relation> relations,
        long seed,
        ValidationResult validation) {

    public GalaxyGenerationResult {
        Objects.requireNonNull(galaxy, "galaxy");
        structures = List.copyOf(structures);
        systems = List.copyOf(systems);
        stars = List.copyOf(stars);
        relations = List.copyOf(relations);
        Objects.requireNonNull(validation, "validation");
    }

    public boolean isValid() {
        return validation.isValid() && galaxy.isPresent();
    }

    public void register(Universe universe) {
        if (!isValid()) {
            validation.assertValid();
            throw new IllegalStateException("Cannot register a generation result without a galaxy");
        }
        Galaxy generatedGalaxy = galaxy.orElseThrow();
        List<Entity> entities = new ArrayList<>(1 + structures.size() + systems.size() + stars.size());
        entities.add(generatedGalaxy);
        entities.addAll(structures);
        entities.addAll(systems);
        entities.addAll(stars);
        universe.registerBatch(entities, relations);
    }

    public static GalaxyGenerationResult invalid(long seed, ValidationResult validation) {
        return new GalaxyGenerationResult(Optional.empty(), List.of(), List.of(), List.of(), List.of(), seed, validation);
    }
}
