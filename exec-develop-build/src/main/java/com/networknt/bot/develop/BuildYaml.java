package com.networknt.bot.develop;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        // Serialize first so unsupported values do not truncate an existing config file.
        Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
    }

    private static final class ConfigRepresenter extends StandardRepresenter {
        private ConfigRepresenter(DumpSettings settings) {
            super(settings);
            parentClassRepresenters.put(Date.class, value ->
                    representScalar(Tag.STR, Instant.ofEpochMilli(((Date) value).getTime()).toString()));
            parentClassRepresenters.put(Calendar.class, value ->
                    representScalar(Tag.STR, ((Calendar) value).toInstant().toString()));
            parentClassRepresenters.put(TemporalAccessor.class, value -> representScalar(Tag.STR, value.toString()));
            parentClassRepresenters.put(Enum.class, value -> representScalar(Tag.STR, ((Enum<?>) value).name()));
        }
    }
}
