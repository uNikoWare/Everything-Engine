package com.everythingengine.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopVersion {
    private static final Pattern VERSION = Pattern.compile(
            "v\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:_([a-z]))?");

    private DesktopVersion() {
    }

    public static String current() {
        Properties properties = new Properties();
        try (InputStream input = DesktopVersion.class.getResourceAsStream("/everything-engine.properties")) {
            if (input == null) {
                throw new IllegalStateException("Application version metadata is missing.");
            }
            properties.load(input);
            String version = properties.getProperty("desktop.version");
            if (version == null) {
                throw new IllegalStateException("Application version metadata is missing.");
            }
            requireValid(version);
            return version;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read application version.", exception);
        }
    }

    public static String packageVersion(String version) {
        Matcher matcher = requireValidMatcher(version);
        return new BigInteger(matcher.group(1)).add(BigInteger.ONE)
                + "." + matcher.group(2) + "." + matcher.group(3);
    }

    public static int compare(String left, String right) {
        Matcher leftMatcher = requireValidMatcher(left);
        Matcher rightMatcher = requireValidMatcher(right);
        for (int group = 1; group <= 3; group++) {
            int comparison = new BigInteger(leftMatcher.group(group))
                    .compareTo(new BigInteger(rightMatcher.group(group)));
            if (comparison != 0) {
                return comparison;
            }
        }
        String leftBranch = leftMatcher.group(4);
        String rightBranch = rightMatcher.group(4);
        if (Objects.equals(leftBranch, rightBranch)) {
            return 0;
        }
        if (leftBranch == null) {
            return 1;
        }
        if (rightBranch == null) {
            return -1;
        }
        return leftBranch.compareTo(rightBranch);
    }

    public static String requireValid(String version) {
        requireValidMatcher(version);
        return version;
    }

    private static Matcher requireValidMatcher(String version) {
        Matcher matcher = VERSION.matcher(Objects.requireNonNull(version, "version"));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Version must use v.a.b.c format with an optional lowercase branch suffix, for example v.0.1.1_a.");
        }
        return matcher;
    }
}
