package com.straight8.rambeau.bukkit.command;

import com.straight8.rambeau.bukkit.Messages;
import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import com.straight8.rambeau.bukkit.data.PluginCatalog.CatalogSummary;
import com.straight8.rambeau.bukkit.data.PluginCatalog.ExportResult;
import com.straight8.rambeau.bukkit.data.PluginCatalog.UrlAuditEntry;
import com.straight8.rambeau.bukkit.data.PluginCatalog.UrlList;
import com.straight8.rambeau.bukkit.data.PluginCatalog.UrlResult;
import com.straight8.rambeau.util.CommandPageUtils;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PluginVersionsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("help", "list", "reload", "debug", "config", "set", "export");
    private static final List<String> HELP_TOPICS = List.of("overview", "commands", "permissions", "placeholders", "config", "set");
    private static final List<String> DEBUG_TOPICS = List.of("status", "plugin", "server", "plugins", "commands", "permissions", "placeholders", "config", "set", "url");
    private static final List<String> URL_ACTIONS = List.of("add", "del", "list", "audit");
    private static final List<String> EXPORT_FORMATS = List.of("markdown", "discord");
    private static final List<String> CONSOLE_ONLY_SET_PATHS = List.of("database.file", "exports.directory");
    private static final Map<String, String> PERMISSIONS = Map.of(
            "help", "pluginversions.help",
            "list", "pluginversions.list",
            "reload", "pluginversions.reload",
            "debug", "pluginversions.debug",
            "config", "pluginversions.config",
            "set", "pluginversions.set",
            "export", "pluginversions.export"
    );

    private final PluginVersionsBukkit plugin;

    public PluginVersionsCommand(PluginVersionsBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subCommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        String[] remaining = args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "help", "?" -> runHelp(sender, remaining);
            case "list" -> runList(sender, remaining);
            case "reload" -> runReload(sender);
            case "debug" -> runDebug(sender, remaining);
            case "config" -> runConfig(sender, remaining);
            case "set" -> runSet(sender, remaining);
            case "export" -> runExport(sender, remaining);
            default -> {
                plugin.getMessages().send(sender, "errors.unknown-command", Messages.token("command", subCommand));
                runHelp(sender, new String[0]);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandPageUtils.filterByPrefix(args[0], allowedSubcommands(sender));
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && ("help".equals(subCommand) || "?".equals(subCommand))) {
            return CommandPageUtils.filterByPrefix(args[1], HELP_TOPICS);
        }
        if ("debug".equals(subCommand)) {
            return completeDebug(sender, args);
        }
        if (args.length == 2 && "list".equals(subCommand)) {
            int totalPages = CommandPageUtils.getTotalPages(plugin.getServer().getPluginManager().getPlugins().length, linesPerPage());
            return CommandPageUtils.getNextInteger(args[1], totalPages);
        }
        if (args.length == 2 && "config".equals(subCommand)) {
            int totalPages = CommandPageUtils.getTotalPages(configLeaves().size(), linesPerPage());
            return CommandPageUtils.getNextInteger(args[1], totalPages);
        }
        if (args.length == 2 && "set".equals(subCommand)) {
            return CommandPageUtils.filterByPrefix(args[1], configLeaves());
        }
        if (args.length == 2 && "export".equals(subCommand)) {
            return CommandPageUtils.filterByPrefix(args[1], EXPORT_FORMATS);
        }
        return List.of();
    }

    private boolean runHelp(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "help")) {
            return true;
        }
        return sendStaticPage(sender, "help", HELP_TOPICS, args.length == 0 ? "overview" : args[0], helpTokens());
    }

    private boolean runList(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "list")) {
            return true;
        }

        if (plugin.getConfig().getBoolean("database.update-on-list", true)) {
            plugin.getCatalog().refreshAndSave();
        }

        Plugin[] plugins = plugin.getCatalog().sortedPlugins();
        if (plugins.length == 0) {
            plugin.getMessages().send(sender, "list.empty");
            return true;
        }

        int linesPerPage = linesPerPage();
        int totalPages = CommandPageUtils.getTotalPages(plugins.length, linesPerPage);
        int page = args.length == 0 ? 0 : parsePage(sender, args[0], totalPages);
        if (page < 0) {
            return true;
        }
        List<Plugin> displayPlugins = page == 0
                ? Arrays.asList(plugins)
                : CommandPageUtils.getPage(Arrays.asList(plugins), page, linesPerPage);

        Messages messages = plugin.getMessages();
        messages.sendRaw(sender, messages.raw(page == 0 ? "list.header" : "list.page-header"),
                Messages.token("count", plugins.length),
                Messages.token("enabled", countEnabled(plugins)),
                Messages.token("disabled", plugins.length - countEnabled(plugins)),
                Messages.token("page", page == 0 ? 1 : page),
                Messages.token("pages", totalPages));

        int maxSpacing = CommandPageUtils.getMaxNameLength(Plugin::getName, displayPlugins);
        boolean player = sender instanceof Player;
        for (Plugin listedPlugin : displayPlugins) {
            String messagePath = listedPlugin.isEnabled() ? "list.enabled-version" : "list.disabled-version";
            messages.sendRaw(sender, messages.raw(messagePath),
                    Messages.token("name", listedPlugin.getName()),
                    Messages.token("display-name", listedPlugin.getPluginMeta().getDisplayName()),
                    Messages.token("internal-name", listedPlugin.getPluginMeta().getName()),
                    Messages.token("spacing", CommandPageUtils.getSpacingFor(listedPlugin.getName(), maxSpacing, player)),
                    Messages.token("version", listedPlugin.getPluginMeta().getVersion()));
        }
        return true;
    }

    private boolean runReload(CommandSender sender) {
        if (!hasPermission(sender, "reload")) {
            return true;
        }
        plugin.reloadPluginVersions();
        CatalogSummary summary = plugin.getCatalog().refreshAndSave();
        plugin.getMessages().send(sender, "reload.complete",
                Messages.token("locale", plugin.getConfig().getString("settings.locale", "EN")),
                Messages.token("database-file", summary.databaseFile().getPath()),
                Messages.token("plugins", summary.total()));
        return true;
    }

    private boolean runDebug(CommandSender sender, String[] args) {
        if (args.length > 0 && "url".equalsIgnoreCase(args[0])) {
            return runDebugUrl(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if (!hasPermission(sender, "debug")) {
            return true;
        }
        String requested = args.length == 0 ? "status" : args[0];
        return sendStaticPage(sender, "debug", DEBUG_TOPICS, requested, debugTokens());
    }

    private boolean runConfig(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "config")) {
            return true;
        }

        List<String> leaves = configLeaves();
        int linesPerPage = linesPerPage();
        int totalPages = CommandPageUtils.getTotalPages(leaves.size(), linesPerPage);
        int page = parsePage(sender, args.length == 0 ? "1" : args[0], totalPages);
        if (page < 0) {
            return true;
        }

        Messages messages = plugin.getMessages();
        messages.sendRaw(sender, messages.raw("config.header"),
                Messages.token("page", page),
                Messages.token("pages", totalPages));

        for (String path : CommandPageUtils.getPage(leaves, page, linesPerPage)) {
            messages.sendRaw(sender, messages.raw("config.line"),
                    Messages.token("path", path),
                    Messages.token("value", plugin.getConfig().get(path)));
        }
        return true;
    }

    private boolean runSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "set")) {
            return true;
        }
        if (args.length < 2) {
            plugin.getMessages().send(sender, "set.usage");
            return true;
        }

        String path = args[0];
        if (!configLeaves().contains(path)) {
            plugin.getMessages().send(sender, "set.unknown-path", Messages.token("path", path));
            return true;
        }
        if (requiresConsoleForSet(path) && !(sender instanceof ConsoleCommandSender)) {
            plugin.getMessages().send(sender, "set.console-only", Messages.token("path", path));
            return true;
        }

        String rawValue = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Object current = plugin.getConfig().get(path);
        Object parsed = parseConfigValue(current, rawValue);
        if (parsed == null) {
            plugin.getMessages().send(sender, "set.invalid-value",
                    Messages.token("path", path),
                    Messages.token("value", rawValue));
            return true;
        }

        plugin.getConfig().set(path, parsed);
        plugin.saveConfig();
        plugin.reloadPluginVersions();
        plugin.getMessages().send(sender, "set.complete",
                Messages.token("path", path),
                Messages.token("value", parsed));
        return true;
    }

    private boolean runExport(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "export")) {
            return true;
        }
        String format = args.length == 0 ? "markdown" : args[0].toLowerCase(Locale.ROOT);
        if (!EXPORT_FORMATS.contains(format)) {
            plugin.getMessages().send(sender, "export.usage");
            return true;
        }
        try {
            ExportResult result = "discord".equals(format)
                    ? plugin.getCatalog().exportDiscord()
                    : plugin.getCatalog().exportMarkdown();
            plugin.getMessages().send(sender, "export.complete",
                    Messages.token("format", result.format()),
                    Messages.token("file", result.timestampedFile().getPath()),
                    Messages.token("latest-file", result.latestFile().getPath()));
        } catch (IOException e) {
            plugin.getMessages().send(sender, "export.failed", Messages.token("error", e.getMessage()));
        }
        return true;
    }

    private boolean runDebugUrl(CommandSender sender, String[] args) {
        if (args.length == 0) {
            plugin.getMessages().send(sender, "url.usage");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if ("list".equals(action)) {
            if (!hasPermissionNode(sender, "pluginversions.url.list")) {
                return true;
            }
            if (args.length < 2) {
                plugin.getMessages().send(sender, "url.usage");
                return true;
            }
            UrlList urls = plugin.getCatalog().manualUrls(args[1]);
            if (!urls.found()) {
                plugin.getMessages().send(sender, "url.plugin-not-found", Messages.token("plugin", args[1]));
                return true;
            }
            if (urls.urls().isEmpty()) {
                plugin.getMessages().send(sender, "url.list-empty", Messages.token("plugin", urls.pluginName()));
                return true;
            }

            plugin.getMessages().send(sender, "url.list-header", Messages.token("plugin", urls.pluginName()));
            for (Map.Entry<String, List<String>> entry : urls.urls().entrySet()) {
                for (String url : entry.getValue()) {
                    plugin.getMessages().send(sender, "url.list-line",
                            Messages.token("category", entry.getKey()),
                            Messages.token("url", url));
                }
            }
            return true;
        }

        if ("audit".equals(action)) {
            if (!hasPermissionNode(sender, "pluginversions.url.list")) {
                return true;
            }
            List<UrlAuditEntry> entries = plugin.getCatalog().urlAudit();
            if (entries.isEmpty()) {
                plugin.getMessages().send(sender, "url.audit-empty");
                return true;
            }

            int totalPages = CommandPageUtils.getTotalPages(entries.size(), linesPerPage());
            int page = parsePage(sender, args.length >= 2 ? args[1] : "1", totalPages);
            if (page < 0) {
                return true;
            }
            plugin.getMessages().sendRaw(sender, plugin.getMessages().raw("url.audit-header"),
                    Messages.token("page", page),
                    Messages.token("pages", totalPages),
                    Messages.token("count", entries.size()));
            for (UrlAuditEntry entry : CommandPageUtils.getPage(entries, page, linesPerPage())) {
                plugin.getMessages().sendRaw(sender, plugin.getMessages().raw("url.audit-line"),
                        Messages.token("plugin", entry.pluginName()),
                        Messages.token("status", entry.status()),
                        Messages.token("detected", entry.detectedCount()),
                        Messages.token("manual", entry.manualCount()),
                        Messages.token("categories", entry.categories()));
            }
            return true;
        }

        if (!"add".equals(action) && !"del".equals(action)) {
            plugin.getMessages().send(sender, "url.usage");
            return true;
        }
        String permission = "add".equals(action) ? "pluginversions.url.add" : "pluginversions.url.del";
        if (!hasPermissionNode(sender, permission)) {
            return true;
        }
        if (args.length < 3) {
            plugin.getMessages().send(sender, "url.usage");
            return true;
        }

        String category = args.length >= 4 ? args[2] : null;
        String rawUrl = args.length >= 4 ? args[3] : args[2];
        UrlResult result = "add".equals(action)
                ? plugin.getCatalog().addManualUrl(args[1], category, rawUrl)
                : plugin.getCatalog().removeManualUrl(args[1], category, rawUrl);
        if (!result.valid()) {
            plugin.getMessages().send(sender, "url.invalid-url", Messages.token("url", result.url()));
            return true;
        }
        if (!result.categoryValid()) {
            plugin.getMessages().send(sender, "url.invalid-category", Messages.token("category", result.category()));
            return true;
        }
        if (!result.found()) {
            plugin.getMessages().send(sender, "url.plugin-not-found", Messages.token("plugin", result.pluginName()));
            return true;
        }

        if ("add".equals(action)) {
            plugin.getMessages().send(sender, result.changed() ? "url.add-complete" : "url.add-existing",
                    Messages.token("plugin", result.pluginName()),
                    Messages.token("category", result.category()),
                    Messages.token("url", result.url()));
        } else {
            plugin.getMessages().send(sender, result.changed() ? "url.del-complete" : "url.del-missing",
                    Messages.token("plugin", result.pluginName()),
                    Messages.token("category", result.category()),
                    Messages.token("url", result.url()));
        }
        return true;
    }

    private boolean sendStaticPage(CommandSender sender, String root, List<String> topics, String requested, Messages.Token[] tokens) {
        int pageIndex = pageIndex(topics, requested);
        if (pageIndex < 0) {
            sendNoSuchPage(sender, requested, topics.size());
            return true;
        }
        String topic = topics.get(pageIndex);
        Messages messages = plugin.getMessages();
        List<Messages.Token> mergedTokens = new ArrayList<>(Arrays.asList(tokens));
        mergedTokens.add(Messages.token("page", pageIndex + 1));
        mergedTokens.add(Messages.token("pages", topics.size()));
        mergedTokens.add(Messages.token("topic", topic));
        mergedTokens.add(Messages.token("title", messages.raw(root + ".pages." + topic + ".title")));

        Messages.Token[] tokenArray = mergedTokens.toArray(Messages.Token[]::new);
        messages.sendRaw(sender, messages.raw(root + ".header"), tokenArray);
        messages.sendList(sender, root + ".pages." + topic + ".lines", tokens);
        return true;
    }

    private Messages.Token[] helpTokens() {
        return Messages.tokens(Map.of(
                "command", "pv",
                "alias", "pv",
                "lines-per-page", linesPerPage()
        ));
    }

    private Messages.Token[] debugTokens() {
        Plugin[] plugins = plugin.getCatalog().sortedPlugins();
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("alias", "pv");
        tokens.put("status", plugin.isEnabled() ? "enabled" : "disabled");
        tokens.put("uptime", formatDuration(plugin.getUptime()));
        tokens.put("locale", plugin.getConfig().getString("settings.locale", "EN"));
        tokens.put("database-file", plugin.getCatalog().databaseFile().getPath());
        tokens.put("database-tracked", plugin.getCatalog().getTrackedPluginCount());
        tokens.put("export-directory", plugin.getCatalog().exportDirectory().getPath());
        tokens.put("plugin-name", plugin.getPluginMeta().getName());
        tokens.put("plugin-version", plugin.getPluginMeta().getVersion());
        tokens.put("plugin-main", plugin.getPluginMeta().getMainClass());
        tokens.put("plugin-api", plugin.getPluginMeta().getAPIVersion());
        tokens.put("plugin-authors", String.join(", ", plugin.getPluginMeta().getAuthors()));
        tokens.put("data-folder", plugin.getDataFolder().getPath());
        tokens.put("server-name", plugin.getServer().getName());
        tokens.put("server-version", plugin.getServer().getVersion());
        tokens.put("bukkit-version", Bukkit.getBukkitVersion());
        tokens.put("java-version", System.getProperty("java.version"));
        tokens.put("online-players", plugin.getServer().getOnlinePlayers().size());
        tokens.put("worlds", plugin.getServer().getWorlds().stream().map(World::getName).collect(Collectors.joining(", ")));
        tokens.put("plugin-count", plugins.length);
        tokens.put("enabled-count", countEnabled(plugins));
        tokens.put("disabled-count", plugins.length - countEnabled(plugins));
        tokens.put("lines-per-page", linesPerPage());
        tokens.put("check-for-updates", plugin.getConfig().getBoolean("check-for-updates", false));
        tokens.put("update-on-enable", plugin.getConfig().getBoolean("database.update-on-enable", true));
        tokens.put("update-on-list", plugin.getConfig().getBoolean("database.update-on-list", true));
        tokens.put("history-limit", plugin.getConfig().getInt("database.max-history-per-plugin", 25));
        return Messages.tokens(tokens);
    }

    private List<String> completeDebug(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return CommandPageUtils.filterByPrefix(args[1], allowedDebugTopics(sender));
        }
        if (!"url".equalsIgnoreCase(args[1])) {
            return List.of();
        }
        if (args.length == 3) {
            return CommandPageUtils.filterByPrefix(args[2], allowedUrlActions(sender));
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if (args.length == 4 && "audit".equals(action)) {
            int totalPages = CommandPageUtils.getTotalPages(plugin.getCatalog().trackedPluginNames().size(), linesPerPage());
            return CommandPageUtils.getNextInteger(args[3], totalPages);
        }
        if (args.length == 4 && ("add".equals(action) || "del".equals(action) || "list".equals(action))) {
            return CommandPageUtils.filterByPrefix(args[3], plugin.getCatalog().trackedPluginNames());
        }
        if (args.length == 5 && "add".equals(action)) {
            return CommandPageUtils.filterByPrefix(args[4], plugin.getCatalog().urlCategorySuggestions());
        }
        if (args.length == 5 && "del".equals(action)) {
            List<String> suggestions = new ArrayList<>(plugin.getCatalog().urlCategorySuggestions());
            suggestions.addAll(manualUrlSuggestions(args[3]));
            return CommandPageUtils.filterByPrefix(args[4], suggestions);
        }
        if (args.length == 6 && "del".equals(action)) {
            return CommandPageUtils.filterByPrefix(args[5], manualUrlSuggestions(args[3], args[4]));
        }
        return List.of();
    }

    private List<String> manualUrlSuggestions(String pluginName) {
        return manualUrlSuggestions(pluginName, null);
    }

    private List<String> manualUrlSuggestions(String pluginName, String category) {
        UrlList urls = plugin.getCatalog().manualUrls(pluginName);
        if (!urls.found()) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        if (category == null || category.isBlank()) {
            for (List<String> categoryUrls : urls.urls().values()) {
                suggestions.addAll(categoryUrls);
            }
            return suggestions;
        }
        List<String> categoryUrls = urls.urls().get(category.toLowerCase(Locale.ROOT));
        if (categoryUrls != null) {
            suggestions.addAll(categoryUrls);
        }
        return suggestions;
    }

    private List<String> allowedSubcommands(CommandSender sender) {
        return SUBCOMMANDS.stream()
                .filter(subCommand -> canUseSubcommand(sender, subCommand))
                .toList();
    }

    private boolean canUseSubcommand(CommandSender sender, String subCommand) {
        if ("debug".equals(subCommand)) {
            return canUse(sender, PERMISSIONS.get(subCommand))
                    || canUse(sender, "pluginversions.url.list")
                    || canUse(sender, "pluginversions.url.add")
                    || canUse(sender, "pluginversions.url.del");
        }
        return canUse(sender, PERMISSIONS.get(subCommand));
    }

    private List<String> allowedDebugTopics(CommandSender sender) {
        List<String> topics = new ArrayList<>();
        if (canUse(sender, "pluginversions.debug")) {
            topics.addAll(DEBUG_TOPICS);
        } else if (canUse(sender, "pluginversions.url.list") || canUse(sender, "pluginversions.url.add") || canUse(sender, "pluginversions.url.del")) {
            topics.add("url");
        }
        return topics;
    }

    private List<String> allowedUrlActions(CommandSender sender) {
        List<String> actions = new ArrayList<>();
        if (canUse(sender, "pluginversions.url.add")) {
            actions.add("add");
        }
        if (canUse(sender, "pluginversions.url.del")) {
            actions.add("del");
        }
        if (canUse(sender, "pluginversions.url.list")) {
            actions.add("list");
            actions.add("audit");
        }
        return actions;
    }

    private boolean hasPermission(CommandSender sender, String subCommand) {
        String permission = PERMISSIONS.get(subCommand);
        return hasPermissionNode(sender, permission);
    }

    private boolean hasPermissionNode(CommandSender sender, String permission) {
        if (canUse(sender, permission)) {
            return true;
        }
        plugin.getMessages().send(sender, "errors.no-permission", Messages.token("permission", permission));
        return false;
    }

    private boolean canUse(CommandSender sender, String permission) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission(permission);
    }

    private boolean requiresConsoleForSet(String path) {
        return CONSOLE_ONLY_SET_PATHS.contains(path);
    }

    private int pageIndex(List<String> topics, String requested) {
        if (CommandPageUtils.isInteger(requested)) {
            int page = Integer.parseInt(requested);
            return isPageInRange(page, topics.size()) ? page - 1 : -1;
        }

        int index = topics.indexOf(requested.toLowerCase(Locale.ROOT));
        return Math.max(index, 0);
    }

    private int linesPerPage() {
        return Math.max(1, plugin.getConfig().getInt("settings.lines-per-page", 10));
    }

    private int parsePage(CommandSender sender, String value, int totalPages) {
        if (!CommandPageUtils.isInteger(value)) {
            sendNoSuchPage(sender, value, totalPages);
            return -1;
        }

        int page = Integer.parseInt(value);
        if (!isPageInRange(page, totalPages)) {
            sendNoSuchPage(sender, value, totalPages);
            return -1;
        }
        return page;
    }

    private boolean isPageInRange(int page, int totalPages) {
        return page >= 1 && page <= totalPages;
    }

    private void sendNoSuchPage(CommandSender sender, String requestedPage, int totalPages) {
        plugin.getMessages().send(sender, "errors.no-such-page",
                Messages.token("page", requestedPage),
                Messages.token("pages", totalPages));
    }

    private int countEnabled(Plugin[] plugins) {
        int enabled = 0;
        for (Plugin listedPlugin : plugins) {
            if (listedPlugin.isEnabled()) {
                enabled++;
            }
        }
        return enabled;
    }

    private List<String> configLeaves() {
        ConfigurationSection section = plugin.getConfig();
        List<String> leaves = new ArrayList<>();
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                leaves.add(key);
            }
        }
        return leaves;
    }

    private Object parseConfigValue(Object current, String rawValue) {
        if (current instanceof Boolean) {
            if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
                return Boolean.parseBoolean(rawValue);
            }
            return null;
        }
        if (current instanceof Integer) {
            try {
                return Integer.parseInt(rawValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (current instanceof Long) {
            try {
                return Long.parseLong(rawValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (current instanceof Double) {
            try {
                return Double.parseDouble(rawValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return rawValue;
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }
        return remainingSeconds + "s";
    }
}
