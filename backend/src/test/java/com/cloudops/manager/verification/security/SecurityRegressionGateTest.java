package com.cloudops.manager.verification.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityRegressionGateTest {

    private static final Path BACKEND_SRC = Path.of("src/main/java");
    private static final Path FRONTEND_SRC = Path.of("../frontend/src");

    @Test
    @DisplayName("Security Invariant: 0 ProcessBuilder or Runtime.exec executions in backend source")
    void testZeroProcessExecution() throws IOException {
        if (!Files.exists(BACKEND_SRC)) return;
        try (Stream<Path> paths = Files.walk(BACKEND_SRC)) {
            long count = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .mapToLong(p -> {
                        try {
                            return Files.lines(p).filter(line -> line.contains("ProcessBuilder") || line.contains("Runtime.getRuntime().exec")).count();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
            assertEquals(0, count, "Must have zero ProcessBuilder or Runtime.exec calls");
        }
    }

    @Test
    @DisplayName("Security Invariant: 0 Database/JPA Entity annotations in backend source")
    void testZeroDatabasePersistence() throws IOException {
        if (!Files.exists(BACKEND_SRC)) return;
        try (Stream<Path> paths = Files.walk(BACKEND_SRC)) {
            long count = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .mapToLong(p -> {
                        try {
                            return Files.lines(p).filter(line -> line.contains("@Entity") || line.contains("@Table") || line.contains("JpaRepository")).count();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
            assertEquals(0, count, "Must have zero database JPA/Entity annotations");
        }
    }

    @Test
    @DisplayName("Security Invariant: 0 AWS SDK imports in frontend TypeScript files")
    void testZeroFrontendAwsSdk() throws IOException {
        if (!Files.exists(FRONTEND_SRC)) return;
        try (Stream<Path> paths = Files.walk(FRONTEND_SRC)) {
            long count = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".tsx"))
                    .mapToLong(p -> {
                        try {
                            return Files.lines(p).filter(line -> line.contains("@aws-sdk") || line.contains("aws-sdk")).count();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
            assertEquals(0, count, "Must have zero AWS SDK imports in frontend source");
        }
    }
}