package com.straight8.rambeau.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlFiles {
    private YamlFiles() {
    }

    public static YamlConfiguration load(File file, Logger logger) {
        YamlConfiguration configuration = new YamlConfiguration();
        if (!file.exists()) {
            return configuration;
        }

        try {
            configuration.loadFromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException e) {
            logger.warning("Could not load YAML file " + file.getPath() + ": " + e.getMessage());
        }
        return configuration;
    }
}
