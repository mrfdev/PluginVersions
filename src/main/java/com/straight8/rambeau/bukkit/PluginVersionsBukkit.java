package com.straight8.rambeau.bukkit;

import com.straight8.rambeau.bukkit.command.PluginVersionsCommand;
import com.straight8.rambeau.bukkit.config.PluginConfig;
import com.straight8.rambeau.bukkit.data.PluginCatalog;
import com.straight8.rambeau.bukkit.placeholder.PluginVersionsExpansion;
import com.straight8.rambeau.util.YamlFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginVersionsBukkit extends JavaPlugin {
    private static final String DEFAULT_LOCALE_FILE = "translations/Locale_EN.yml";
    private static final int LANGUAGE_FILE_VERSION = 5;
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final char LEGACY_COLOR_CHAR = '\u0026';
    private static final Map<String, String> LEGACY_MESSAGE_KEYS = Map.of(
            "page-header-format", "list.page-header",
            "enabled-version-format", "list.enabled-version",
            "disabled-version-format", "list.disabled-version"
    );
    private static final Set<String> LEGACY_CONFIG_KEYS = Set.of(
            "update-source",
            "page-header-format",
            "enabled-version-format",
            "disabled-version-format"
    );

    private long enabledAt;
    private PluginConfig pluginConfig;
    private Messages messages;
    private PluginCatalog catalog;
    private PluginVersionsExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        enabledAt = System.currentTimeMillis();

        saveDefaultConfig();
        ensureDefaultLocaleFile();
        migrateLegacyMessages();
        reloadPluginVersions();

        PluginVersionsCommand commandExecutor = new PluginVersionsCommand(this);
        PluginCommand command = Objects.requireNonNull(getCommand("pv"), "pv command");
        command.setExecutor(commandExecutor);
        command.setTabCompleter(commandExecutor);
        registerPluginVersionsNamespace(command);

        if (getConfig().getBoolean("database.update-on-enable", true)) {
            catalog.refreshAndSave();
        }
        registerPlaceholderExpansion();
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }

    public Messages getMessages() {
        return messages;
    }

    public PluginCatalog getCatalog() {
        return catalog;
    }

    @Override
    public FileConfiguration getConfig() {
        return pluginConfig().get();
    }

    @Override
    public void reloadConfig() {
        pluginConfig().reload();
    }

    @Override
    public void saveConfig() {
        pluginConfig().save();
    }

    @Override
    public void saveDefaultConfig() {
        pluginConfig().reload();
    }

    public void reloadPluginVersions() {
        reloadConfig();
        upgradeConfig();
        ensureDefaultLocaleFile();
        messages = new Messages(this);
        catalog = new PluginCatalog(this);
    }

    public Duration getUptime() {
        return Duration.ofMillis(System.currentTimeMillis() - enabledAt);
    }

    private PluginConfig pluginConfig() {
        if (pluginConfig == null) {
            pluginConfig = new PluginConfig(this);
        }
        return pluginConfig;
    }

    private void registerPluginVersionsNamespace(PluginCommand command) {
        if (getServer().getCommandMap().getCommand("pluginversions:pv") == null) {
            getServer().getCommandMap().register("pluginversions", command);
        }
    }

    private void registerPlaceholderExpansion() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        placeholderExpansion = new PluginVersionsExpansion(this);
        if (placeholderExpansion.register()) {
            getLogger().info("Registered PlaceholderAPI expansion.");
        }
    }

    private void ensureDefaultLocaleFile() {
        File localeFile = new File(getDataFolder(), DEFAULT_LOCALE_FILE);
        if (!localeFile.exists()) {
            saveResource(DEFAULT_LOCALE_FILE, false);
            return;
        }

        YamlConfiguration locale = YamlFiles.load(localeFile, getLogger());
        if (locale.getInt("language-file-version", 0) >= LANGUAGE_FILE_VERSION) {
            return;
        }

        File backupFile = new File(localeFile.getParentFile(),
                localeFile.getName() + ".bak-" + BACKUP_TIMESTAMP.format(LocalDateTime.now()));
        try {
            Files.copy(localeFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            saveResource(DEFAULT_LOCALE_FILE, true);
            getLogger().info("Updated default English language file. Previous copy backed up to " + backupFile.getName() + ".");
        } catch (IOException e) {
            getLogger().warning("Could not update default English language file: " + e.getMessage());
        }
    }

    private void migrateLegacyMessages() {
        File localeFile = new File(getDataFolder(), DEFAULT_LOCALE_FILE);
        YamlConfiguration locale = YamlFiles.load(localeFile, getLogger());
        boolean changed = false;

        for (Map.Entry<String, String> entry : LEGACY_MESSAGE_KEYS.entrySet()) {
            String legacyValue = getConfig().getString(entry.getKey());
            if (legacyValue != null && !isLegacyDefaultMessage(entry.getKey(), legacyValue)) {
                locale.set(entry.getValue(), Messages.legacyToMiniMessage(legacyValue));
                changed = true;
            }
        }

        if (changed) {
            try {
                locale.save(localeFile);
                getLogger().info("Migrated legacy message formats to " + DEFAULT_LOCALE_FILE + ".");
            } catch (IOException e) {
                getLogger().warning("Could not save migrated language messages: " + e.getMessage());
            }
        }
    }

    private boolean isLegacyDefaultMessage(String key, String value) {
        return switch (key) {
            case "page-header-format" -> "PluginVersions ===== page {page} =====".equals(value);
            case "enabled-version-format" -> (" - " + LEGACY_COLOR_CHAR + "a{name}{spacing}" + LEGACY_COLOR_CHAR + "e{version}").equals(value);
            case "disabled-version-format" -> (" - " + LEGACY_COLOR_CHAR + "c{name}{spacing}" + LEGACY_COLOR_CHAR + "e{version}").equals(value);
            default -> false;
        };
    }

    private void upgradeConfig() {
        boolean changed = false;
        for (String key : LEGACY_CONFIG_KEYS) {
            if (getConfig().isSet(key)) {
                getConfig().set(key, null);
                changed = true;
            }
        }

        if (changed || !new File(getDataFolder(), "config.yml").exists()) {
            saveConfig();
        }
    }
}
