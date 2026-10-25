package com.everythingengine.core.galaxy.generation;

import com.everythingengine.core.distribution.PositionDistribution;
import com.everythingengine.core.galaxy.GalaxyExtent;
import com.everythingengine.core.galaxy.GalaxyType;
import com.everythingengine.core.name.NameProfile;
import com.everythingengine.core.spatial.Position3D;
import com.everythingengine.core.validation.ValidationIssue;
import com.everythingengine.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed configuration for one staged galaxy generation run.
 */
public record GalaxyGenerationConfig(
        String name,
        GalaxyType type,
        Position3D position,
        GalaxyExtent extent,
        GalaxyGenerationMode mode,
        int systemCount,
        Optional<PositionDistribution> distribution,
        Optional<NameProfile> nameProfile,
        boolean createStarPerSystem,
        long seed) {

    public GalaxyGenerationConfig {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(extent, "extent");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(distribution, "distribution");
        Objects.requireNonNull(nameProfile, "nameProfile");
    }

    public ValidationResult validate() {
        var errors = new ArrayList<ValidationIssue>();
        if (mode == GalaxyGenerationMode.STRUCTURAL && systemCount != 0) {
            errors.add(ValidationIssue.error("systemCount",
                    "Structural generation cannot include a system population.", "generation.mode.population"));
        }
        if (mode == GalaxyGenerationMode.STRUCTURAL && createStarPerSystem) {
            errors.add(ValidationIssue.error("createStarPerSystem",
                    "Structural generation cannot include stellar objects.", "generation.mode.stars"));
        }
        if (mode == GalaxyGenerationMode.POPULATION && (systemCount < 0 || systemCount > 10_000)) {
            errors.add(ValidationIssue.error("systemCount",
                    "Population size must be between 0 and 10,000 systems.", "generation.system-count"));
        }
        if (mode == GalaxyGenerationMode.POPULATION && systemCount > 0 && distribution.isEmpty()) {
            errors.add(ValidationIssue.error("distribution",
                    "Population generation requires a spatial distribution.", "generation.distribution"));
        }
        if (!name.isBlank() && name.length() > 200) {
            errors.add(ValidationIssue.error("name", "Galaxy name must not exceed 200 characters.", "galaxy.name"));
        }
        return new ValidationResult(errors, java.util.List.of());
    }
}
