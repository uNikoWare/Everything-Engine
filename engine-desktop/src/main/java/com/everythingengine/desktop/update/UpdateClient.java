package com.everythingengine.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

public final class UpdateClient {
    private static final int MAXIMUM_MANIFEST_BYTES = 16 * 1024;
    private static final long MAXIMUM_INSTALLER_BYTES = 1024L * 1024 * 1024;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Optional<UpdateManifest> check(String manifestUrl, String currentVersion)
            throws IOException, InterruptedException {
        URI manifestUri = validateHttpsUri(manifestUrl);
        HttpRequest request = HttpRequest.newBuilder(manifestUri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "text/plain")
                .header("User-Agent", "EverythingEngine/" + currentVersion)
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        byte[] bytes;
        try (InputStream body = response.body()) {
            requireSuccessfulHttps(response.statusCode(), response.uri());
            bytes = body.readNBytes(MAXIMUM_MANIFEST_BYTES + 1);
        }
        if (bytes.length > MAXIMUM_MANIFEST_BYTES) {
            throw new IOException("Update manifest exceeds the maximum allowed size.");
        }
        try {
            UpdateManifest manifest = UpdateManifest.parse(new String(bytes, StandardCharsets.UTF_8));
            return UpdateManifest.compareVersions(manifest.version(), currentVersion) > 0
                    ? Optional.of(manifest)
                    : Optional.empty();
        } catch (IllegalArgumentException exception) {
            throw new IOException("Could not validate update manifest: " + exception.getMessage(), exception);
        }
    }

    public Path download(UpdateManifest manifest, Path updateDirectory)
            throws IOException, InterruptedException {
        Files.createDirectories(updateDirectory);
        Path installer = Files.createTempFile(updateDirectory, "everything-engine-update-", ".msi");
        boolean verified = false;
        try {
            HttpRequest request = HttpRequest.newBuilder(manifest.installerUri())
                    .timeout(Duration.ofMinutes(15))
                    .header("Accept", "application/octet-stream")
                    .header("User-Agent", "EverythingEngine-Updater")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            long size = 0;
            try (InputStream input = response.body()) {
                requireSuccessfulHttps(response.statusCode(), response.uri());
                try (OutputStream output = Files.newOutputStream(installer,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        size += read;
                        if (size > MAXIMUM_INSTALLER_BYTES) {
                            throw new IOException("Downloaded installer exceeds the maximum allowed size.");
                        }
                        output.write(buffer, 0, read);
                    }
                }
            }
            if (size == 0) {
                throw new IOException("Downloaded installer has an invalid size.");
            }
            if (!MessageDigest.isEqual(
                    HexFormat.of().parseHex(manifest.sha256()),
                    HexFormat.of().parseHex(sha256(installer)))) {
                throw new IOException("Downloaded installer SHA-256 does not match the update manifest.");
            }
            verified = true;
            return installer;
        } finally {
            if (!verified) {
                Files.deleteIfExists(installer);
            }
        }
    }

    public static URI validateHttpsUri(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Update URLs must be valid HTTPS URLs.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Update URLs must use HTTPS and must not contain credentials or fragments.");
        }
        return uri;
    }

    private void requireSuccessfulHttps(int status, URI responseUri) throws IOException {
        if (!"https".equalsIgnoreCase(responseUri.getScheme())) {
            throw new IOException("Update server redirected to a non-HTTPS URL.");
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Update server returned HTTP " + status + ".");
        }
    }

    private String sha256(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in this Java runtime.", exception);
        }
    }
}
