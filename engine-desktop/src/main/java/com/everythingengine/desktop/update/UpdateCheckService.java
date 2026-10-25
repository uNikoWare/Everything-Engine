package com.everythingengine.desktop.update;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public final class UpdateCheckService extends Service<UpdateCheckResult> {
    private final UpdateClient client;
    private final String manifestUrl;
    private final String currentVersion;

    public UpdateCheckService(UpdateClient client, String manifestUrl, String currentVersion) {
        this.client = client;
        this.manifestUrl = manifestUrl;
        this.currentVersion = currentVersion;
    }

    @Override
    protected Task<UpdateCheckResult> createTask() {
        return new Task<>() {
            @Override
            protected UpdateCheckResult call() throws Exception {
                updateMessage("Checking for updates...");
                return new UpdateCheckResult(currentVersion, client.check(manifestUrl, currentVersion));
            }
        };
    }
}
