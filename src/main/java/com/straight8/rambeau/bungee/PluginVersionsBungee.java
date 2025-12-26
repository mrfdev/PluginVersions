package com.straight8.rambeau.bungee;

import com.straight8.rambeau.metrics.BungeeMetrics;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
// Imports for Metrics

public class PluginVersionsBungee extends Plugin {

    private static PluginVersionsBungee instance;

    private boolean configurationSendMetrics = true;
    private boolean checkUpdates = true;

    private Configuration config;

    public static PluginVersionsBungee getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        YamlConfig.createFiles("config");

        this.ReadConfigValuesFromFile();

        if (configurationSendMetrics) {
            new BungeeMetrics(this, 13031);
        }

        this.getProxy().getPluginManager().registerCommand(this, new PluginVersionsCmd(this));

        if (checkUpdates) {
            new UpdateChecker(this, (response, version) -> {
                switch (response) {
                    case LATEST: {
                        getLogger().info("Running latest version!");
                        break;
                    }
                    case UNAVAILABLE: {
                        getLogger().info("Unable to check for new version");
                        break;
                    }
                    case FOUND_NEW: {
                        getLogger().warning("Running outdated version! New version available:" + version);
                        break;
                    }
                }
            }).check();
        }
    }

    public void ReadConfigValuesFromFile() {
        Configuration reloadedConfig = YamlConfig.getConfiguration("config");

        // Optimized the code to read the configuration options
        configurationSendMetrics = reloadedConfig.getBoolean("enable-metrics", true);
        checkUpdates = reloadedConfig.getBoolean("check-for-updates", true);
        config = reloadedConfig;
    }

    public Configuration getConfig() {
        return config;
    }

    public void log(String logString) {
        this.getLogger().info("[" + this.getDescription().getName() + "] " + logString);
    }
}
