package com.straight8.rambeau.bungee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class YamlConfig {

    public static void saveConfiguration(Configuration configuration, String file) {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, new File(PluginVersionsBungee.getInstance().getDataFolder(), file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createFiles(String file) {
        if (!PluginVersionsBungee.getInstance().getDataFolder().exists()) {
            PluginVersionsBungee.getInstance().getDataFolder().mkdir();
        }
        File fileconfig = new File(PluginVersionsBungee.getInstance().getDataFolder(), file + ".yml");
        if (!fileconfig.exists()) {
            try {
                InputStream in = PluginVersionsBungee.getInstance().getResourceAsStream(file + ".yml");
                Files.copy(in, fileconfig.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Configuration getConfiguration(String file) {
        File configFile = new File(PluginVersionsBungee.getInstance().getDataFolder(), file + ".yml");
        if (!configFile.exists()) {
            return null;
        }
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}