package com.straight8.rambeau.bukkit.placeholder;

import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

public final class PluginVersionsExpansion extends PlaceholderExpansion {
    private final PluginVersionsBukkit plugin;

    public PluginVersionsExpansion(PluginVersionsBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "pluginversions";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String normalized = params.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "total", "plugin_count" -> String.valueOf(plugins().length);
            case "enabled", "enabled_count" -> String.valueOf(countEnabled());
            case "disabled", "disabled_count" -> String.valueOf(plugins().length - countEnabled());
            case "database_tracked", "tracked" -> String.valueOf(plugin.getCatalog().getTrackedPluginCount());
            default -> resolveDynamicPlaceholder(params);
        };
    }

    private String resolveDynamicPlaceholder(String params) {
        String normalized = params.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("version_")) {
            Plugin foundPlugin = findPlugin(params.substring("version_".length()));
            return foundPlugin == null ? "" : foundPlugin.getPluginMeta().getVersion();
        }

        if (normalized.startsWith("url_")) {
            String remaining = params.substring("url_".length());
            int categorySeparator = remaining.lastIndexOf('_');
            if (categorySeparator <= 0 || categorySeparator == remaining.length() - 1) {
                return "";
            }
            String pluginName = remaining.substring(0, categorySeparator);
            String category = remaining.substring(categorySeparator + 1);
            return plugin.getCatalog().urlFor(pluginName, category);
        }

        return "";
    }

    private Plugin findPlugin(String pluginName) {
        Plugin exactMatch = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (exactMatch != null) {
            return exactMatch;
        }

        String alternateName = pluginName.replace('_', '-');
        exactMatch = plugin.getServer().getPluginManager().getPlugin(alternateName);
        if (exactMatch != null) {
            return exactMatch;
        }

        for (Plugin loadedPlugin : plugins()) {
            if (loadedPlugin.getName().equalsIgnoreCase(pluginName)
                    || loadedPlugin.getPluginMeta().getName().equalsIgnoreCase(pluginName)
                    || loadedPlugin.getPluginMeta().getDisplayName().equalsIgnoreCase(pluginName)) {
                return loadedPlugin;
            }
        }
        return null;
    }

    private Plugin[] plugins() {
        return plugin.getServer().getPluginManager().getPlugins();
    }

    private int countEnabled() {
        int enabled = 0;
        for (Plugin loadedPlugin : plugins()) {
            if (loadedPlugin.isEnabled()) {
                enabled++;
            }
        }
        return enabled;
    }
}
