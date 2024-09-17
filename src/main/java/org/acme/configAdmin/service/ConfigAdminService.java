package org.acme.configAdmin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.acme.configAdmin.config.ConfigLocationConfig;
import org.acme.configAdmin.model.ConfigTracker;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Named("ConfigAdminService")
@ApplicationScoped
public class ConfigAdminService {

    @Inject
    ConfigLocationConfig configLocationConfig;
    @Getter
    private Map<UUID, ConfigTracker> configFiles = new LinkedHashMap<>();

    @PostConstruct
    public void init() throws IOException, ConfigurationException {
        Set<ConfigTracker> files = new LinkedHashSet<>();
        for (ConfigLocationConfig.ConfigLocation curLocationConfig : configLocationConfig.locations()) {
            Path curPath = Path.of(curLocationConfig.location());
            log.info("Processing configuration(s) at {}", curPath);

            if (Files.isDirectory(curPath)) {
                log.debug("Got directory to search.");
                try (Stream<Path> fileStream = Files.walk(curPath)) {
                    //TODO:: filter out not config file extensions
                    files.addAll(
                            fileStream
                                    .sorted()
                                    .filter(Files::isRegularFile)
//                                    .filter((path) -> path.getFileName().toString().endsWith(".yml")) //TODO
                                    .map((path -> {
                                        return ConfigTracker.builder()
                                                .id(UUID.randomUUID())
                                                .path(path)
                                                .title(curLocationConfig.name() + " - " + path.getFileName().toString())
                                                .name(path.getFileName().toString())
                                                .location(Optional.of(path.getParent().toString() + "/"))
                                                .description(curLocationConfig.description())
                                                .build();
                                    }))
                                    .toList()
                    );
                }
            } else if (Files.isRegularFile(curPath)) {
                files.add(ConfigTracker.builder()
                        .id(UUID.randomUUID())
                        .path(curPath)
                        .title(curLocationConfig.name() + " - " + curPath.getFileName().toString())
                        .name(curPath.getFileName().toString())
                        .location(Optional.of(curPath.getParent().toString() + "/"))
                        .description(curLocationConfig.description())
                        .build());
            }
        }


        log.debug("Got files: {}", files);
        //ensure config can be read in
        for (ConfigTracker curConfigTracker : files) {
            log.debug("Read in config: {}", curConfigTracker.getConfiguration());
        }

        files.stream().forEachOrdered((curTracker) -> {
            configFiles.put(curTracker.getId(), curTracker);
        });
    }

    public ConfigTracker getTracker(UUID id) {
        ConfigTracker output = this.getConfigFiles().get(id);
        if (output == null) {
            throw new IllegalArgumentException("No config file found with id " + id);
        }
        return output;
    }

    public void overwriteFile(UUID id, String data) throws IOException {
        ConfigTracker tracker = this.getTracker(id);
        log.info("Overwriting config file {}/{} with string data.", tracker.getId(), tracker.getPath());
        Files.writeString(tracker.getPath(), data, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Done overwriting config file {}/{} with string data.", tracker.getId(), tracker.getPath());
    }

    public void overwriteFile(UUID id, AbstractConfiguration data) throws IOException, ConfigurationException {
        ConfigTracker tracker = this.getTracker(id);

        log.info("Overwriting config file {}/{} with parsed configuration data.", tracker.getId(), tracker.getPath());
        try (
                FileWriter writer = new FileWriter(tracker.getPath().toFile(), false)
        ) {
            ((FileBasedConfiguration) data).write(writer);
        }
        log.info("Done overwriting config file {}/{} with parsed configuration data.", tracker.getId(), tracker.getPath());
    }


    private List<String> fromJson(JsonNode json) {
        List<String> output = new ArrayList<>();
        if (json instanceof ArrayNode) {
            for (JsonNode node : json) {
                output.add(node.asText());
            }
        } else if (json instanceof TextNode) {
            output.add(json.asText());
        }
        return output;
    }

    public void processUpdateRequest(UUID id, ObjectNode requestBody) throws ConfigurationException, IOException {
        log.debug("Request: {}", requestBody);
        ConfigTracker configTracker = this.getTracker(id);

        AbstractConfiguration config = configTracker.getConfiguration();

        {//updates
            List<String> fieldsToUpdate = this.fromJson(requestBody.get("updateFlag[]"));
            List<String> existingKeys = this.fromJson(requestBody.get("existingKeys[]"));
            List<String> existingValues = this.fromJson(requestBody.get("existingValues[]"));
            log.info("Fields to update: {}", fieldsToUpdate);
            for(int i = 0; i < existingKeys.size(); i++) {
                String curKey = existingKeys.get(i);
                String curValue = existingValues.get(i);

                if(fieldsToUpdate.contains(curKey)){
                    config.setProperty(curKey, curValue);
                }
            }
        }
        {//removals
            List<String> fieldsToRemove = this.fromJson(requestBody.get("removeKeys[]"));
            log.info("Fields to remove: {}", fieldsToRemove);
            for(String curKey : fieldsToRemove) {
                config.clearProperty(curKey);
            }
        }
        {//additions
            List<String> fieldKeysToAdd = this.fromJson(requestBody.get("newKeys[]"));
            List<String> fieldValuesToAdd = this.fromJson(requestBody.get("newValues[]"));

            log.info("Fields to add: {}", fieldKeysToAdd);
            for(int i = 0; i < fieldKeysToAdd.size(); i++) {
                String curKey = fieldKeysToAdd.get(i);
                String curValue = fieldValuesToAdd.get(i);
                config.addProperty(curKey, curValue);
            }
        }

        this.overwriteFile(id, config);
    }
}
