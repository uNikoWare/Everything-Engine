package com.everythingengine.core.stellar;

import com.everythingengine.core.validation.ValidationIssue;
import com.everythingengine.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Plausibility checks are advisory and do not replace astrophysical modeling.
 */
public final class StellarValidation {
    private StellarValidation() {
    }

    public static ValidationResult validate(StellarObject object) {
        Objects.requireNonNull(object, "object");
        var warnings = new ArrayList<ValidationIssue>();
        if (object.massSolar().isPresent() && object.massSolar().getAsDouble() > 150) {
            warnings.add(ValidationIssue.warning("massSolar",
                    "Mass exceeds 150 solar masses; verify this unusual value."));
        }
        if (object.ageYears().isPresent() && object.ageYears().getAsDouble() > 13_800_000_000d) {
            warnings.add(ValidationIssue.warning("ageYears",
                    "Age exceeds the approximate age of the observable universe."));
        }
        if (object.metallicityFeH().isPresent()
                && (object.metallicityFeH().getAsDouble() < -5 || object.metallicityFeH().getAsDouble() > 2)) {
            warnings.add(ValidationIssue.warning("metallicityFeH",
                    "Metallicity is outside the common stellar range; this is unusual, not automatically invalid."));
        }
        if (object instanceof Star star && star.effectiveTemperatureKelvin().isPresent()
                && star.effectiveTemperatureKelvin().getAsDouble() > 200_000) {
            warnings.add(ValidationIssue.warning("effectiveTemperatureKelvin",
                    "Temperature exceeds 200,000 K; verify this unusual stellar value."));
        }
        return new ValidationResult(java.util.List.of(), warnings);
    }
}
