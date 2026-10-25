package com.everythingengine.desktop.testing;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenTestService extends Service<TestRunResult> {
    private static final Pattern SUITE = Pattern.compile("<testsuite\\b([^>]*)>");
    private static final Pattern ATTRIBUTE = Pattern.compile("\\b(tests|failures|errors|skipped)=\"(\\d+)\"");
    private final Path projectRoot;

    public MavenTestService(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public static Path locateProjectRoot(Path start) {
        Path current = start.toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    protected Task<TestRunResult> createTask() {
        return new Task<>() {
            @Override
            protected TestRunResult call() throws Exception {
                if (projectRoot == null) {
                    return result(TestRunResult.Status.ERROR, 0, 0, 0, Instant.now(),
                            "No Maven project was found.", "");
                }
                updateMessage("Starting Maven tests...");
                Instant started = Instant.now();
                List<String> command = testCommand();
                Process process;
                try {
                    process = new ProcessBuilder(command)
                            .directory(projectRoot.toFile())
                            .redirectErrorStream(true)
                            .start();
                } catch (IOException exception) {
                    return result(TestRunResult.Status.ERROR, 0, 0, 0, started,
                            "Could not start Maven. Install Maven and ensure it is on PATH.", exception.getMessage());
                }

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                        updateMessage(line);
                        updateProgress(-1, 1);
                    }
                }
                int exitCode = process.waitFor();
                SuiteCounts counts = readSuiteCounts(projectRoot.resolve("target/surefire-reports"));
                TestRunResult.Status status = exitCode == 0
                        ? TestRunResult.Status.SUCCESSFUL
                        : TestRunResult.Status.FAILED;
                String message = exitCode == 0
                        ? "Maven tests completed successfully."
                        : "Maven test process exited with code " + exitCode + ".";
                return new TestRunResult(status, counts.passed(), counts.failed(), counts.skipped(),
                        Duration.between(started, Instant.now()), Instant.now(), message, output.toString());
            }
        };
    }

    private List<String> testCommand() {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            return List.of("cmd.exe", "/c", "mvn.cmd", "-B", "test");
        }
        return List.of("mvn", "-B", "test");
    }

    private SuiteCounts readSuiteCounts(Path reports) throws IOException {
        if (!Files.isDirectory(reports)) {
            return new SuiteCounts(0, 0, 0);
        }
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        try (var files = Files.list(reports)) {
            for (Path file : files.filter(path -> path.getFileName().toString().startsWith("TEST-")
                    && path.getFileName().toString().endsWith(".xml")).toList()) {
                Matcher suiteMatcher = SUITE.matcher(Files.readString(file));
                if (suiteMatcher.find()) {
                    int tests = 0;
                    int failures = 0;
                    int errors = 0;
                    int suiteSkipped = 0;
                    Matcher attributes = ATTRIBUTE.matcher(suiteMatcher.group(1));
                    while (attributes.find()) {
                        int value = Integer.parseInt(attributes.group(2));
                        switch (attributes.group(1)) {
                            case "tests" -> tests = value;
                            case "failures" -> failures = value;
                            case "errors" -> errors = value;
                            case "skipped" -> suiteSkipped = value;
                            default -> throw new IllegalStateException("Unexpected report attribute.");
                        }
                    }
                    failed += failures + errors;
                    passed += tests - failures - errors - suiteSkipped;
                    skipped += suiteSkipped;
                }
            }
        }
        return new SuiteCounts(passed, failed, skipped);
    }

    private TestRunResult result(
            TestRunResult.Status status, int passed, int failed, int skipped, Instant completed, String message, String output) {
        return new TestRunResult(status, passed, failed, skipped, Duration.ZERO, completed, message, output);
    }

    private record SuiteCounts(int passed, int failed, int skipped) {
    }
}
