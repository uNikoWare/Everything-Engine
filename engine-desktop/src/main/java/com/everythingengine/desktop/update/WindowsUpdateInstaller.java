package com.everythingengine.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class WindowsUpdateInstaller {
    private WindowsUpdateInstaller() {
    }

    public static Optional<Path> currentApplicationPath() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return Optional.empty();
        }
        return ProcessHandle.current().info().command()
                .map(Path::of)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equalsIgnoreCase("Everything Engine.exe"));
    }

    public static void installAfterApplicationExit(Path installer, Path application, long applicationPid)
            throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            throw new IllegalStateException("Automatic MSI updates are only supported on Windows.");
        }
        Path normalizedInstaller = installer.toAbsolutePath().normalize();
        Path normalizedApplication = application.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedInstaller)
                || !normalizedInstaller.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".msi")) {
            throw new IllegalArgumentException("The verified MSI installer is not available.");
        }
        if (!Files.isRegularFile(normalizedApplication) || applicationPid <= 0) {
            throw new IllegalArgumentException("The installed application process could not be identified.");
        }
        Path updateDirectory = normalizedInstaller.getParent();
        Path helper = Files.createTempFile(updateDirectory, "apply-everything-engine-update-", ".ps1");
        try (InputStream resource = WindowsUpdateInstaller.class.getResourceAsStream("/apply-update.ps1")) {
            if (resource == null) {
                throw new IOException("The Windows update helper script is missing.");
            }
            Files.write(helper, resource.readAllBytes());
        } catch (IOException exception) {
            Files.deleteIfExists(helper);
            throw exception;
        }
        try {
            String powershell = System.getenv("SystemRoot")
                    + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
            new ProcessBuilder(
                    powershell,
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-File", helper.toString(),
                    "-AppPid", Long.toString(applicationPid),
                    "-InstallerPath", normalizedInstaller.toString(),
                    "-ApplicationPath", normalizedApplication.toString(),
                    "-HelperPath", helper.toString())
                    .start();
        } catch (IOException exception) {
            Files.deleteIfExists(helper);
            throw exception;
        }
    }
}
