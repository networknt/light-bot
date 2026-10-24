package com.networknt.bot.develop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildYamlTest {
    @TempDir
    Path directory;

    @Test
    void usesCoreScalarsAndLastDuplicateValue() throws IOException {
        Files.writeString(directory.resolve("scalars.yml"),
                "enabled: true\nlegacy: on\ndecimal: 0755\noctal: 0o755\nbinary: 0b11\n"
                        + "separated: 1_000\ntime: 1:30\nfloat: 1:30.5\ndate: 2026-10-24\n"
                        + "duplicate: first\nduplicate: last\n");
        Map<String, Object> values = ConfigUtils.getInstance().loadMapConfig("scalars", directory.toFile());
        assertEquals(true, values.get("enabled"));
        assertEquals("on", values.get("legacy"));
        assertEquals(755, values.get("decimal"));
        assertEquals(493, values.get("octal"));
        assertEquals("0b11", values.get("binary"));
        assertEquals("1_000", values.get("separated"));
        assertEquals("1:30", values.get("time"));
        assertEquals("1:30.5", values.get("float"));
        assertEquals("2026-10-24", values.get("date"));
        assertEquals("last", values.get("duplicate"));
    }

    @Test
    void supportsYamlAndJsonFallbacks() throws IOException {
        Files.writeString(directory.resolve("fallback.yaml"), "value: yaml\n");
        Files.writeString(directory.resolve("json-only.json"), "{\"value\":\"json\"}");
        assertEquals("yaml", ConfigUtils.getInstance().loadMapConfig("fallback", directory.toFile()).get("value"));
        assertEquals("json", ConfigUtils.getInstance().loadMapConfig("json-only", directory.toFile()).get("value"));
        Files.writeString(directory.resolve("fallback.yml"), "value: preferred\n");
        assertEquals("preferred", ConfigUtils.getInstance().loadMapConfig("fallback", directory.toFile()).get("value"));
    }

    @Test
    void loadsClasspathStreamsWithoutFileInputStreamCast() {
        assertEquals("root", ConfigUtils.getInstance().loadMapConfig("engine-root", directory.toFile()).get("source"));
        assertEquals("config", ConfigUtils.getInstance().loadMapConfig("engine-config", directory.toFile()).get("source"));
    }

    @Test
    void missingConfigStillReturnsNull() {
        assertNull(ConfigUtils.getInstance().loadMapConfig("missing-engine-config", directory.toFile()));
    }

    @Test
    void parseFailureNamesFileAndDoesNotFallBack() throws IOException {
        Files.writeString(directory.resolve("broken.yml"), "value: [unfinished");
        Files.writeString(directory.resolve("broken.json"), "{\"value\":\"must not load\"}");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConfigUtils.getInstance().loadMapConfig("broken", directory.toFile()));
        assertEquals("Unable to parse broken.yml using YAML 1.2 Core", failure.getMessage());
        assertInstanceOf(YamlEngineException.class, failure.getCause());
    }

    @Test
    void unsupportedLegacyTagHasContext() throws IOException {
        Files.writeString(directory.resolve("timestamp.yaml"), "value: !!timestamp 2026-10-24\n");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConfigUtils.getInstance().loadMapConfig("timestamp", directory.toFile()));
        assertEquals("Unable to parse timestamp.yaml using YAML 1.2 Core", failure.getMessage());
        assertInstanceOf(YamlEngineException.class, failure.getCause());
    }

    @Test
    void roundTripsMapsSequencesUnicodeAndAmbiguousStrings() throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("text", "café λ\u0085line\u2029end");
        values.put("sequence", List.of("true", "0755", "null", "on"));
        values.put("nested", Map.of("enabled", true, "number", 755));
        values.put("empty", null);
        BuildYaml.write(directory.resolve("roundtrip.yml").toFile(), values);
        assertEquals(values, ConfigUtils.getInstance().loadMapConfig("roundtrip", directory.toFile()));
        assertTrue(Files.readString(directory.resolve("roundtrip.yml")).contains("\"nested\":\n"));
    }

    @Test
    void serializesDatesAndTemporalValuesAsIsoStrings() throws IOException {
        Instant instant = Instant.parse("2026-10-24T12:00:00Z");
        BuildYaml.write(directory.resolve("dates.yml").toFile(),
                Map.of("date", Date.from(instant), "instant", instant));
        assertEquals(Map.of("date", instant.toString(), "instant", instant.toString()),
                ConfigUtils.getInstance().loadMapConfig("dates", directory.toFile()));
    }

    @Test
    void unsupportedDumpDoesNotTruncateExistingFile() throws IOException {
        Path output = directory.resolve("existing.yml");
        Files.writeString(output, "sentinel");
        IOException failure = assertThrows(IOException.class,
                () -> BuildYaml.write(output.toFile(), Map.of("unsupported", new Object())));
        assertInstanceOf(YamlEngineException.class, failure.getCause());
        assertTrue(failure.getMessage().contains(output.toString()));
        assertEquals("sentinel", Files.readString(output));
    }

    @Test
    void reportsOutputIoFailure() {
        assertThrows(IOException.class,
                () -> BuildYaml.write(directory.resolve("missing/output.yml").toFile(), Map.of("key", "value")));
    }

    @Test
    void emittedStringsStayStringsWithClassicYaml11Reader() throws IOException {
        List<String> ambiguous = List.of("on", "yes", "no", "off", "0b11", "1_000", "1:30", "1:30.5",
                "2026-10-24", "0755", "true", "null", "1_0.5");
        Map<String, Object> values = new LinkedHashMap<>();
        for (String value : ambiguous) {
            values.put(value, value);
        }
        values.put("list", ambiguous);
        values.put("boolean", true);
        values.put("integer", 123);
        values.put("text", "café λ\u0085line\u2029end");
        Path output = directory.resolve("classic.yml");
        BuildYaml.write(output.toFile(), values);
        assertEquals(values, new Yaml().load(Files.readString(output)));
        assertEquals(values, ConfigUtils.getInstance().loadMapConfig("classic", directory.toFile()));
    }

    @Test
    void nonMappingRootHasShapeDiagnostic() throws IOException {
        Files.writeString(directory.resolve("sequence.yml"), "- one\n- two\n");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ConfigUtils.getInstance().loadMapConfig("sequence", directory.toFile()));
        assertEquals("Configuration sequence.yml must have a mapping root", failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test
    void atomicReplacementReplacesExistingConfigWithoutTemporaryFiles() throws IOException {
        Path output = directory.resolve("replace.yml");
        Files.writeString(output, "sentinel");
        BuildYaml.write(output.toFile(), Map.of("value", "yes"));
        assertEquals(Map.of("value", "yes"), new Yaml().load(Files.readString(output)));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(output), files.toList());
        }
    }

    @Test
    void failedReplacementLeavesDestinationAndCleansTemporaryFile() throws IOException {
        Path output = Files.createDirectory(directory.resolve("destination"));
        Path sentinel = output.resolve("sentinel");
        Files.writeString(sentinel, "previous config");
        assertThrows(IOException.class, () -> BuildYaml.write(output.toFile(), Map.of("value", "replacement")));
        assertEquals("previous config", Files.readString(sentinel));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(output), files.toList());
        }
    }

    @Test
    void newFilesUseNormalPermissionsAndReplacementPreservesExistingMode() throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.getFileStore(directory).supportsFileAttributeView("posix"));
        Path ordinary = directory.resolve("ordinary.yml");
        Files.writeString(ordinary, "ordinary");
        Path output = directory.resolve("permissions.yml");
        BuildYaml.write(output.toFile(), Map.of("value", "new"));
        assertEquals(Files.getPosixFilePermissions(ordinary), Files.getPosixFilePermissions(output));
        var permissions = PosixFilePermissions.fromString("rw-r-----");
        Files.setPosixFilePermissions(output, permissions);
        BuildYaml.write(output.toFile(), Map.of("value", "replacement"));
        assertEquals(permissions, Files.getPosixFilePermissions(output));
    }
}
