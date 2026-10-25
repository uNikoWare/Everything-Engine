package com.everythingengine.desktop.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MavenTestServiceTest {
    @TempDir
    Path temp;

    @Test
    void locatesNearestMavenProjectWithoutLeavingTheGivenTree() throws Exception {
        Path root = Files.createDirectory(temp.resolve("project"));
        Path nested = Files.createDirectories(root.resolve("module/src"));
        Files.createFile(root.resolve("pom.xml"));

        assertEquals(root, MavenTestService.locateProjectRoot(nested));
        assertNull(MavenTestService.locateProjectRoot(temp));
    }
}
