package com.everythingengine.desktop.update;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

public record UpdateManifest(String version, URI installerUri, String sha256, String releaseNotes) {
    private static final Pattern VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");

    public UpdateManifest {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(installerUri, "installerUri");
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(releaseNotes, "releaseNotes");
        requireVersion(version);
        UpdateClient.validateHttpsUri(installerUri.toString());
        if (!SHA256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("Update manifest must include a 64-character SHA-256 checksum.");
        }
    }

    public static UpdateManifest parse(String content) {
        Objects.requireNonNull(content, "content");
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Update manifest is not valid properties text.", exception);
        }
        String version = required(properties, "version");
        String installerUrl = required(properties, "installerUrl");
        String checksum = required(properties, "sha256");
        String releaseNotes = properties.getProperty("releaseNotes", "").trim();
        requireVersion(version);
        try {
            return new UpdateManifest(version, URI.create(installerUrl), checksum, releaseNotes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Update manifest contains invalid update metadata.", exception);
        }
    }

    public static int compareVersions(String left, String right) {
        String[] leftParts = requireVersion(left).split("\\.");
        String[] rightParts = requireVersion(right).split("\\.");
        for (int index = 0; index < leftParts.length; index++) {
            int comparison = new BigInteger(leftParts[index]).compareTo(new BigInteger(rightParts[index]));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Update manifest is missing '" + key + "'.");
        }
        return value.trim();
    }

    private static String requireVersion(String version) {
        Objects.requireNonNull(version, "version");
        if (!VERSION.matcher(version).matches()) {
            throw new IllegalArgumentException("Version must use the numeric major.minor.patch format.");
        }
        return version;
    }
}
