package com.everythingengine.core.galaxy.generation;

import com.everythingengine.core.distribution.PositionDistribution;
import com.everythingengine.core.galaxy.Galaxy;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.galaxy.structure.EllipsoidGeometry;
import com.everythingengine.core.galaxy.structure.GalacticRelations;
import com.everythingengine.core.galaxy.structure.GalacticStructureType;
import com.everythingengine.core.galaxy.structure.GalaxyStructure;
import com.everythingengine.core.galaxy.structure.SpiralArmGeometry;
import com.everythingengine.core.name.NameGenerator;
import com.everythingengine.core.name.NameProfile;
import com.everythingengine.core.name.NameProfiles;
import com.everythingengine.core.random.SeededRandom;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.spatial.Vector3D;
import com.everythingengine.core.stellar.Star;
import com.everythingengine.core.stellar.StellarSystem;
import com.everythingengine.core.validation.ValidationIssue;
import com.everythingengine.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Coordinates existing naming, randomization, spatial, and domain APIs into a staged result.
 */
public final class GalaxyGenerator {
    private static final long NAME_SEED_SALT = 0x4E414D455F454E47L;

    public GalaxyGenerationResult generate(GalaxyGenerationConfig config) {
        ValidationResult configValidation = config.validate();
        if (!configValidation.isValid()) {
            return GalaxyGenerationResult.invalid(config.seed(), configValidation);
        }

        NameProfile profile = config.nameProfile().orElseGet(NameProfiles::compoundLocationProfile);
        NameGenerator names = new NameGenerator(config.seed() ^ NAME_SEED_SALT);
        names.registerProfile(profile);
        SeededRandom random = new SeededRandom(config.seed());

        try {
            String galaxyName = config.name().isBlank() ? names.generate(profile) : config.name().trim();
            Galaxy galaxy = new Galaxy(id(config.seed(), "galaxy"), galaxyName, config.type(),
                    config.position(), config.extent(), OptionalDouble.empty(), OptionalDouble.empty(),
                    java.util.OptionalLong.empty());
            List<GalaxyStructure> structures = createStructures(galaxy, config.extent(), config.seed());
            List<StellarSystem> systems = new ArrayList<>(config.systemCount());
            List<Star> stars = new ArrayList<>();
            List<Relation> relations = new ArrayList<>();

            for (int index = 0; index < structures.size(); index++) {
                GalaxyStructure structure = structures.get(index);
                relations.add(new Relation(id(config.seed(), "relation:structure:" + index),
                        galaxy, structure, GalacticRelations.HAS_STRUCTURE));
            }

            if (config.mode() == GalaxyGenerationMode.POPULATION && config.systemCount() > 0) {
                PositionDistribution distribution = config.distribution().orElseThrow();
                List<String> systemNames = names.generateMany(profile, config.systemCount(), false);
                for (int index = 0; index < config.systemCount(); index++) {
                    Position3D position = distribution.sample(random, galaxy.position());
                    Vector3D offset = new Vector3D(
                            position.x() - galaxy.position().x(),
                            position.y() - galaxy.position().y(),
                            position.z() - galaxy.position().z());
                    if (!galaxy.extent().containsOffset(offset)) {
                        return invalid(config.seed(), "distribution",
                                "The selected distribution produced a system outside the galaxy extent.");
                    }
                    StellarSystem system = new StellarSystem(id(config.seed(), "system:" + index),
                            systemNames.get(index), galaxy, position, Optional.empty());
                    systems.add(system);
                    relations.add(new Relation(id(config.seed(), "relation:system:" + index),
                            galaxy, system, GalacticRelations.CONTAINS_SYSTEM));
                    if (config.createStarPerSystem()) {
                        Star star = new Star(id(config.seed(), "star:" + index),
                                system.name() + " A", OptionalDouble.empty(), OptionalDouble.empty(),
                                OptionalDouble.empty(), OptionalDouble.empty(), Optional.empty(), Optional.empty(),
                                OptionalDouble.empty(), OptionalDouble.empty());
                        stars.add(star);
                        relations.add(new Relation(id(config.seed(), "relation:star:" + index),
                                system, star, GalacticRelations.SYSTEM_CONTAINS_STAR));
                    }
                }
            }
            return new GalaxyGenerationResult(Optional.of(galaxy), structures, systems, stars, relations,
                    config.seed(), ValidationResult.valid());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return invalid(config.seed(), "generation", exception.getMessage());
        }
    }

    private List<GalaxyStructure> createStructures(Galaxy galaxy, GalaxyExtent extent, long seed) {
        List<GalaxyStructure> structures = new ArrayList<>();
        addEllipsoid(structures, galaxy, seed, "core", "Core", GalacticStructureType.CORE,
                GalaxyExtent.spherical(extent.radiusXParsecs() * 0.05));
        addEllipsoid(structures, galaxy, seed, "halo", "Halo", GalacticStructureType.HALO, extent);
        switch (galaxy.type().key()) {
            case "spiral" -> {
                addEllipsoid(structures, galaxy, seed, "disk", "Disk", GalacticStructureType.DISK, extent);
                addSpiralArms(structures, galaxy, extent, seed);
            }
            case "barred-spiral" -> {
                addEllipsoid(structures, galaxy, seed, "disk", "Disk", GalacticStructureType.DISK, extent);
                addEllipsoid(structures, galaxy, seed, "bar", "Bar", GalacticStructureType.BAR,
                        new GalaxyExtent(extent.radiusXParsecs() * 0.4, extent.radiusYParsecs() * 0.08,
                                extent.radiusZParsecs()));
                addSpiralArms(structures, galaxy, extent, seed);
            }
            case "elliptical" -> addEllipsoid(structures, galaxy, seed, "bulge", "Bulge",
                    GalacticStructureType.BULGE, new GalaxyExtent(extent.radiusXParsecs() * 0.5,
                            extent.radiusYParsecs() * 0.5, extent.radiusZParsecs() * 0.5));
            case "lenticular" -> {
                addEllipsoid(structures, galaxy, seed, "bulge", "Bulge", GalacticStructureType.BULGE,
                        new GalaxyExtent(extent.radiusXParsecs() * 0.3, extent.radiusYParsecs() * 0.3,
                                extent.radiusZParsecs() * 0.3));
                addEllipsoid(structures, galaxy, seed, "disk", "Disk", GalacticStructureType.DISK, extent);
            }
            default -> addEllipsoid(structures, galaxy, seed, "region", "Main Region",
                    GalacticStructureType.REGION, extent);
        }
        return List.copyOf(structures);
    }

    private void addSpiralArms(List<GalaxyStructure> structures, Galaxy galaxy, GalaxyExtent extent, long seed) {
        double innerRadius = Math.min(extent.radiusXParsecs(), extent.radiusYParsecs()) * 0.1;
        for (int arm = 0; arm < 2; arm++) {
            structures.add(new GalaxyStructure(id(seed, "structure:arm:" + arm), "Spiral Arm " + (arm + 1), galaxy,
                    GalacticStructureType.SPIRAL_ARM, galaxy.position(),
                    new SpiralArmGeometry(innerRadius, Math.toRadians(12), arm * Math.PI, 0.4,
                            extent.radiusZParsecs() * 0.1, extent.radiusZParsecs() * 0.05)));
        }
    }

    private void addEllipsoid(List<GalaxyStructure> structures, Galaxy galaxy, long seed, String key, String name,
            GalacticStructureType type, GalaxyExtent extent) {
        structures.add(new GalaxyStructure(id(seed, "structure:" + key), name, galaxy, type,
                galaxy.position(), new EllipsoidGeometry(extent)));
    }

    private UUID id(long seed, String key) {
        return UUID.nameUUIDFromBytes((seed + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private GalaxyGenerationResult invalid(long seed, String field, String message) {
        return GalaxyGenerationResult.invalid(seed, new ValidationResult(
                List.of(ValidationIssue.error(field, message == null ? "Generation failed." : message,
                        "generation.failed")), List.of()));
    }
}
