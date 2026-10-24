package com.networknt.bot.develop;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/** YAML settings shared by build configuration readers and writers. */
final class BuildYaml {
    private BuildYaml() {
    }

    static Load newLoad() {
        return new Load(LoadSettings.builder().setSchema(new CoreSchema())
                .setAllowDuplicateKeys(true).build());
    }

    static void write(File file, Map<String, Object> config) throws IOException {
        DumpSettings settings = DumpSettings.builder().setSchema(new CoreSchema())
                .setDefaultFlowStyle(FlowStyle.BLOCK).build();
        String yaml;
        try {
            yaml = new Dump(settings, new ConfigRepresenter(settings)).dumpToString(config);
        } catch (YamlEngineException e) {
            throw new IOException("Unable to serialize merged configuration to " + file, e);
        }
        Path destination = file.toPath().toAbsolutePath();
        Path temporary = Files.getFileAttributeView(destination.getParent(), PosixFileAttributeView.class) == null
                ? Files.createTempFile(destination.getParent(), ".merged-config-", ".tmp")
                : Files.createTempFile(destination.getParent(), ".merged-config-", ".tmp",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-rw-")));
        try {
            if (Files.exists(destination)
                    && Files.getFileAttributeView(destination, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(destination));
            }
            Files.writeString(temporary, yaml, StandardCharsets.UTF_8);
            // No non-atomic fallback: failure leaves the previous config intact.
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class ConfigRepresenter extends StandardRepresenter {
        private ConfigRepresenter(DumpSettings settings) {
            super(settings);
            representers.put(String.class, value -> quoted((String) value));
            parentClassRepresenters.put(Date.class, value ->
                    quoted(Instant.ofEpochMilli(((Date) value).getTime()).toString()));
            parentClassRepresenters.put(Calendar.class, value ->
                    quoted(((Calendar) value).toInstant().toString()));
            parentClassRepresenters.put(TemporalAccessor.class, value -> quoted(value.toString()));
            parentClassRepresenters.put(Enum.class, value -> quoted(((Enum<?>) value).name()));
        }

        private Node quoted(String value) {
            return representScalar(Tag.STR, value, ScalarStyle.DOUBLE_QUOTED);
        }
    }
}
