package com.everythingengine.desktop.update;

import java.util.Optional;
import java.util.Objects;

public record UpdateCheckResult(String currentVersion, Optional<UpdateManifest> update) {
    public UpdateCheckResult {
        Objects.requireNonNull(currentVersion, "currentVersion");
        Objects.requireNonNull(update, "update");
    }

    public boolean updateAvailable() {
        return update.isPresent();
    }
}
