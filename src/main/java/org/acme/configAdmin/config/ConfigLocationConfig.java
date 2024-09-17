package org.acme.configAdmin.config;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "configManagement")
public interface ConfigLocationConfig {
    List<ConfigLocation> locations();

    public interface ConfigLocation {
        String name();
        Optional<String> description();
        String location();
    }
}