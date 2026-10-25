package com.everythingengine.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DesktopVersion {
    private DesktopVersion() {
    }

    public static String current() {
        String packagedVersion = System.getProperty("jpackage.app-version");
        if (packagedVersion != null && !packagedVersion.isBlank()) {
            return packagedVersion;
        }
        Properties properties = new Properties();
        try (InputStream input = DesktopVersion.class.getResourceAsStream("/everything-engine.properties")) {
            if (input != null) {
                properties.load(input);
                return properties.getProperty("desktop.version", "0.0.0");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read application version.", exception);
        }
        return "0.0.0";
    }
}
