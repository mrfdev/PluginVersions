package com.straight8.rambeau.bukkit.config;

import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import com.straight8.rambeau.util.YamlFiles;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PluginConfig {
    private static final List<String> HEADER = List.of(
            "PluginVersions configuration",
            "",
            "Player-facing phrases, colors, command text, and placeholders live in translations/Locale_EN.yml.",
            "This file only contains operational settings.",
            "Existing values are preserved when the plugin adds missing defaults or refreshes comments."
    );
    private static final Map<String, List<String>> COMMENTS = comments();

    private final PluginVersionsBukkit plugin;
    private final File configFile;
    private YamlConfiguration config;

    public PluginConfig(PluginVersionsBukkit plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public FileConfiguration get() {
        if (config == null) {
            reload();
        }
        return config;
    }

    public void reload() {
        YamlConfiguration loaded = YamlFiles.loadWithComments(configFile, plugin.getLogger());
        loaded.setDefaults(loadBundledDefaults());
        loaded.options().parseComments(true);

        boolean changed = !configFile.exists();
        changed |= copyMissingDefaults(loaded);
        changed |= applyComments(loaded);

        config = loaded;
        if (changed) {
            save();
        }
    }

    public void save() {
        if (config == null) {
            reload();
            return;
        }

        applyComments(config);
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
    }

    private YamlConfiguration loadBundledDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.options().parseComments(true);
        try (InputStream input = plugin.getResource("config.yml")) {
            if (input == null) {
                plugin.getLogger().warning("Bundled config.yml was not found inside the plugin jar.");
                return defaults;
            }
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            defaults.loadFromString(content);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Could not load bundled config.yml defaults: " + e.getMessage());
        }
        return defaults;
    }

    private boolean copyMissingDefaults(YamlConfiguration loaded) {
        ConfigurationSection defaults = loaded.getDefaults();
        if (defaults == null) {
            return false;
        }

        boolean changed = false;
        for (String path : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(path)) {
                if (!loaded.isSet(path)) {
                    loaded.createSection(path);
                    changed = true;
                }
                continue;
            }
            if (!loaded.isSet(path)) {
                loaded.set(path, defaults.get(path));
                changed = true;
            }
        }
        return changed;
    }

    private boolean applyComments(YamlConfiguration loaded) {
        boolean changed = false;
        List<String> mergedHeader = mergeComments(HEADER, loaded.options().getHeader());
        if (!Objects.equals(loaded.options().getHeader(), mergedHeader)) {
            loaded.options().setHeader(mergedHeader);
            changed = true;
        }

        for (Map.Entry<String, List<String>> entry : COMMENTS.entrySet()) {
            List<String> mergedComments = mergeComments(entry.getValue(), loaded.getComments(entry.getKey()));
            if (!Objects.equals(loaded.getComments(entry.getKey()), mergedComments)) {
                loaded.setComments(entry.getKey(), mergedComments);
                changed = true;
            }
        }
        return changed;
    }

    private List<String> mergeComments(List<String> managedComments, List<String> existingComments) {
        List<String> merged = new ArrayList<>(managedComments);
        for (String existing : existingComments) {
            if (!merged.contains(existing)) {
                merged.add(existing);
            }
        }
        return List.copyOf(merged);
    }

    private static Map<String, List<String>> comments() {
        Map<String, List<String>> comments = new LinkedHashMap<>();
        comments.put("check-for-updates", List.of(
                "Controls whether PluginVersions may run its own update checks when an updater is available.",
                "Default: false.",
                "Safe values: true or false. This build does not start an external updater while this is false.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately."
        ));
        comments.put("settings", List.of(
                "General runtime settings."
        ));
        comments.put("settings.enable-metrics", List.of(
                "Reserved opt-in switch for anonymous bStats-style metrics if metrics support is added.",
                "Default: false.",
                "Safe values: true or false. Global bStats settings may still override plugin-level metrics.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately."
        ));
        comments.put("settings.locale", List.of(
                "Translation locale suffix loaded from the translations folder.",
                "Default: EN, which loads translations/Locale_EN.yml.",
                "Expected format: a locale suffix that matches an existing Locale_<suffix>.yml file; missing files fall back to EN.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately."
        ));
        comments.put("settings.lines-per-page", List.of(
                "Number of rows shown on paged command output such as /pv list <page>, /pv config, /pv help, and /pv debug.",
                "Default: 10.",
                "Safe values: whole numbers of 1 or higher. Values below 1 are treated as 1 at runtime.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately."
        ));
        comments.put("database", List.of(
                "Plugin inventory database settings. The database is generated YAML data, not a second config file."
        ));
        comments.put("database.file", List.of(
                "YAML database file used to build a long-lived plugin inventory.",
                "Default: plugins-database.yml.",
                "Expected format: a file path. Relative paths are stored inside plugins/1MB-PluginVersions/; absolute paths are allowed from console/config only.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately and is console-only for this path.",
                "Changing this path does not move or delete the old database file."
        ));
        comments.put("database.update-on-enable", List.of(
                "Scans loaded plugins and updates the database when PluginVersions is enabled.",
                "Default: true.",
                "Safe values: true or false.",
                "Manual config edits take effect the next time the plugin is enabled, such as after a server restart or plugin manager reload."
        ));
        comments.put("database.update-on-list", List.of(
                "Scans loaded plugins and updates the database whenever /pv list is used.",
                "Default: true.",
                "Safe values: true or false. Disable this if you only want scans from startup, reload, URL audit, or export actions.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately."
        ));
        comments.put("database.max-history-per-plugin", List.of(
                "Maximum number of scan history entries retained per tracked plugin.",
                "Default: 25.",
                "Safe values: whole numbers of 1 or higher. Values below 1 are treated as 1 at runtime.",
                "Applies during the next database scan; /pv set reloads immediately, then later scans use the new limit."
        ));
        comments.put("exports", List.of(
                "Markdown and Discord-friendly export settings."
        ));
        comments.put("exports.directory", List.of(
                "Folder for timestamped export files created by /pv export markdown and /pv export discord.",
                "Default: exports.",
                "Expected format: a directory path. Relative paths are stored inside plugins/1MB-PluginVersions/; absolute paths are allowed from console/config only.",
                "Manual config edits take effect after /pv reload or a server restart; /pv set reloads immediately and is console-only for this path.",
                "Changing this path does not move or delete older export files."
        ));
        return Map.copyOf(comments);
    }
}
