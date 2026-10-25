package com.everythingengine.desktop.update;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.nio.file.Path;

public final class UpdateDownloadService extends Service<Path> {
    private final UpdateClient client;
    private final UpdateManifest manifest;
    private final Path updateDirectory;

    public UpdateDownloadService(UpdateClient client, UpdateManifest manifest, Path updateDirectory) {
        this.client = client;
        this.manifest = manifest;
        this.updateDirectory = updateDirectory;
    }

    @Override
    protected Task<Path> createTask() {
        return new Task<>() {
            @Override
            protected Path call() throws Exception {
                updateMessage("Downloading and verifying update...");
                return client.download(manifest, updateDirectory);
            }
        };
    }
}
