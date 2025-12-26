// PluginVersions
// List versions of all loaded plugins, sorted alphabetically.
//		This is a combinaion of /plugins and /version plugin for each member of the list.
// Reload the config.yml file. If config.yml does not exist, copy it from the jar.
// FUTURE? Report available updates for loaded plugins.
// FUTURE? Update specific plugins or all loaded plugins.

package com.straight8.rambeau.bukkit;

import com.straight8.rambeau.bukkit.command.PluginVersionsCommand;
import com.straight8.rambeau.metrics.SpigotMetrics;
import dev.ratas.slimedogcore.impl.SlimeDogCore;
import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

// import org.bukkit.Bukkit;
// Imports for Metrics

public class PluginVersionsBukkit extends SlimeDogCore {
    public final Logger logger = Logger.getLogger("Minecraft");

    private Messages messages;

    // Configuration values:
    private boolean configurationSendMetrics = true;

    // Fired when plugin is first enabled
    @Override
    public void pluginEnabled() {
        // Enable is logged automatically.

        // Create config.yml and plugin directory tree, if they do not exist.
        CreateConfigFileIfMissing();

        // Read the configuration values from config.yml.
        ReadConfigValuesFromFile();

        // Submit plugin usage data to MCStats.org.
        if (configurationSendMetrics) {
            new SpigotMetrics(this, 5509);
        }
        messages = new Messages(getDefaultConfig());
        Objects.requireNonNull(getCommand("pluginversions")).setExecutor(new PluginVersionsCommand(this));
    }

    public Messages getMessages() {
        return messages;
    }

    // Fired when plugin is disabled
    @Override
    public void pluginDisabled() {
        // Disable is logged automatically.
    }

    public void CreateConfigFileIfMissing() {
        try {
            String pdfFile = this.getPluginMeta().getDescription();

            if (!getDataFolder().exists()) {
                this.log(pdfFile + ": folder doesn't exist");
                this.log(pdfFile + ": creating folder");
                try {
                    //noinspection ResultOfMethodCallIgnored
                    getDataFolder().mkdirs();
                } catch (Exception e) {
                    this.log(pdfFile + ": could not create folder");
                    return;
                }
                this.log(pdfFile + ": folder created at " + getDataFolder());
            }

            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                this.log(pdfFile + ": config.yml not found, creating!");
                // Copy config.yml from the jar.
                try {
                    saveDefaultConfig();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void ReadConfigValuesFromFile() {
        this.reloadConfig();
        FileConfiguration reloadedConfig = getConfig();
        configurationSendMetrics = reloadedConfig.getBoolean("enable-metrics", true);
        if (messages != null) {
            messages.reload();
        }
    }

    public void log(String logString) {
        this.logger.info("[" + this.getName() + "] " + logString);
    }
}
