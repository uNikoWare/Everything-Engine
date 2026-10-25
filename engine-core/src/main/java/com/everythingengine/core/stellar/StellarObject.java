package com.everythingengine.core.stellar;

import com.everythingengine.core.entity.Entity;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Base for stellar objects. Mass is expressed in solar masses; age in years.
 */
public abstract class StellarObject extends Entity {
    private final StellarObjectType type;
    private final OptionalDouble massSolar;
    private final OptionalDouble ageYears;
    private final OptionalDouble metallicityFeH;

    protected StellarObject(String name, StellarObjectType type, OptionalDouble massSolar,
            OptionalDouble ageYears, OptionalDouble metallicityFeH) {
        this(UUID.randomUUID(), name, type, massSolar, ageYears, metallicityFeH);
    }

    protected StellarObject(UUID id, String name, StellarObjectType type, OptionalDouble massSolar,
            OptionalDouble ageYears, OptionalDouble metallicityFeH) {
        super(id, name);
        this.type = Objects.requireNonNull(type, "type");
        this.massSolar = validate(massSolar, "massSolar", true);
        this.ageYears = validate(ageYears, "ageYears", false);
        this.metallicityFeH = validate(metallicityFeH, "metallicityFeH", false);
    }

    public final StellarObjectType type() {
        return type;
    }

    public final OptionalDouble massSolar() {
        return massSolar;
    }

    public final OptionalDouble ageYears() {
        return ageYears;
    }

    /**
     * Metallicity relative to solar [Fe/H], in dex.
     */
    public final OptionalDouble metallicityFeH() {
        return metallicityFeH;
    }

    private static OptionalDouble validate(OptionalDouble value, String name, boolean strictlyPositive) {
        Objects.requireNonNull(value, name);
        if (value.isPresent()) {
            double number = value.getAsDouble();
            if (!Double.isFinite(number)
                    || strictlyPositive && number <= 0
                    || !strictlyPositive && !name.equals("metallicityFeH") && number < 0) {
                throw new IllegalArgumentException(name + " must be finite and physically non-negative");
            }
        }
        return value;
    }
}
