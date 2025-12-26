package com.straight8.rambeau.velocity;

import com.google.inject.Inject;
import com.straight8.rambeau.metrics.VelocityMetrics;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(id = "pluginversions", name = "PluginVersions", version = "1.3.5", description = "List installed plugins and versions alphabetically", authors = {"drives_a_ford", "GabrielHD150", "SlimeDog"})
public class PluginVersionsVelocity {

    private static PluginVersionsVelocity instance;
    private final Path dataDirectory;
    private final ProxyServer server;
    private final Logger logger;
    private final VelocityMetrics.Factory metricsFactory;
    private YamlConfig yamlConfig;
    private boolean configurationSendMetrics = true;
    private boolean checkUpdates = true;

    @Inject
    public PluginVersionsVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, VelocityMetrics.Factory metricsFactory) {
        instance = this;

        this.server = server;
        this.logger = logger;

        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    public static PluginVersionsVelocity getInstance() {
        return instance;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        YamlConfig.createFiles("config");

        try {
            this.yamlConfig = new YamlConfig(new File(this.dataDirectory.toFile(), "config.yml"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        this.ReadConfigValuesFromFile();

        if (configurationSendMetrics) {

            // All you have to do is adding the following two lines in your onProxyInitialization method.
            // You can find the plugin ids of your plugins on the page https://bstats.org/what-is-my-plugin-id
            metricsFactory.make(this, 13032);
        }

        CommandManager commandManager = server.getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("pluginversions")
                // Specify other aliases (optional)
                .aliases("pluginversionsvelocity")
                .aliases("pvv")
                .build();

        commandManager.register(meta, new PluginVersionsCmd(this));

        if (checkUpdates) {
            new UpdateChecker(this, (response, version) -> {
                switch (response) {
                    case LATEST: {
                        this.logger.info("Running latest version!");
                        break;
                    }
                    case UNAVAILABLE: {
                        this.logger.info("Unable to check for new version");
                        break;
                    }
                    case FOUND_NEW: {
                        this.logger.warn("Running outdated version! New version available:" + version);
                        break;
                    }
                }
            }).check();
        }
    }

    public YamlConfig getConfig() {
        return yamlConfig;
    }

    public void ReadConfigValuesFromFile() {
        YamlConfig configNode = this.yamlConfig;

        // Optimized the code to read the configuration options
        configurationSendMetrics = configNode.getBoolean("enable-metrics", true);
        checkUpdates = configNode.getBoolean("check-for-updates", true);
    }

    public void log(String logString) {
        this.logger.info("[PluginVersions] " + logString);
    }

    public ProxyServer getServer() {
        return server;
    }

    public File getDataFolder() {
        return this.dataDirectory.toFile();
    }

    public InputStream getResourceAsStream(String name) {
        return this.getClass().getClassLoader().getResourceAsStream(name);
    }
}
