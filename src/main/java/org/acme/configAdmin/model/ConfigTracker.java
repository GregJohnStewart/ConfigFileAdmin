package org.acme.configAdmin.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.builder.EqualsExclude;
import org.apache.commons.lang3.builder.HashCodeExclude;
import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Builder
@Data
public class ConfigTracker {
    private static final Tika TIKA = new Tika();
    private static final Configurations CONFIGURATIONS = new Configurations();

    @EqualsExclude
    @HashCodeExclude
    private UUID id;

    @EqualsExclude
    @HashCodeExclude
    @NonNull
    private String title;

    @EqualsExclude
    @HashCodeExclude
    @NonNull
    private String name;

    @EqualsExclude
    @HashCodeExclude
    @Builder.Default
    @NonNull
    private Optional<String> location = Optional.empty();

    @EqualsExclude
    @HashCodeExclude
    @NonNull
    private Optional<String> description = Optional.empty();

    @NonNull
    private Path path;

    public AbstractConfiguration getConfiguration() throws IOException, ConfigurationException {
        String type = TIKA.detect(this.getPath());

        log.debug("Attempting to read in configuration file {} of type {}", this.getPath(), type);
        return switch (type) {
            case "text/x-java-properties" -> CONFIGURATIONS.properties(this.getPath().toFile());
            case "text/x-yaml" -> {
                YAMLConfiguration yaml = new YAMLConfiguration();
                try (InputStream is = Files.newInputStream(this.getPath())) {
                    yaml.read(is);
                }
                yield yaml;
            }
            default -> throw new IllegalArgumentException("Unsupported configuration file type: " + type);
        };
    };

    public Map<String, String> getProperties() throws ConfigurationException, IOException {
        AbstractConfiguration configuration = this.getConfiguration();

        Map<String, String> properties = new LinkedHashMap<>();
        Iterator<String> keys = configuration.getKeys();
        while (keys.hasNext()) {
            String curKey = keys.next();
            properties.put(curKey, configuration.getString(curKey));
        }
        return properties;
    }

    public String getContent() throws IOException {
        return Files.readString(this.getPath());
    }
}
