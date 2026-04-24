package com.straight8.rambeau.bukkit.data;

import com.straight8.rambeau.bukkit.PluginComparator;
import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import com.straight8.rambeau.util.YamlFiles;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class PluginCatalog {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s)>,]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_CATEGORY_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");
    private static final List<String> URL_CATEGORY_SUGGESTIONS = List.of(
            "website", "github", "source", "jenkins", "ci", "download", "modrinth", "hangar", "spigot", "dev-bukkit", "docs"
    );

    private final PluginVersionsBukkit owner;

    public PluginCatalog(PluginVersionsBukkit owner) {
        this.owner = owner;
    }

    public CatalogSummary refreshAndSave() {
        Plugin[] plugins = sortedPlugins();
        File databaseFile = databaseFile();
        YamlConfiguration database = loadDatabase(databaseFile);
        String now = timestamp();

        database.set("metadata.last-scan", now);
        database.set("metadata.server.name", owner.getServer().getName());
        database.set("metadata.server.version", owner.getServer().getVersion());
        database.set("metadata.server.bukkit-version", Bukkit.getBukkitVersion());
        database.set("metadata.plugin-count.total", plugins.length);
        database.set("metadata.plugin-count.enabled", countEnabled(plugins));
        database.set("metadata.plugin-count.disabled", plugins.length - countEnabled(plugins));

        for (Plugin plugin : plugins) {
            updatePlugin(database, plugin, now);
        }

        try {
            ensureParent(databaseFile);
            database.save(databaseFile);
        } catch (IOException e) {
            owner.getLogger().warning("Could not save plugin database: " + e.getMessage());
        }

        return new CatalogSummary(plugins.length, countEnabled(plugins), plugins.length - countEnabled(plugins), databaseFile, now);
    }

    public ExportResult exportMarkdown() throws IOException {
        CatalogSummary summary = refreshAndSave();
        File exportDirectory = exportDirectory();
        Files.createDirectories(exportDirectory.toPath());

        String timestamp = FILE_TIMESTAMP.format(LocalDateTime.now());
        String content = createMarkdown(summary);
        File timestampedFile = new File(exportDirectory, "plugins-" + timestamp + ".md");
        File latestFile = new File(exportDirectory, "plugins-latest.md");
        Files.writeString(timestampedFile.toPath(), content, StandardCharsets.UTF_8);
        Files.writeString(latestFile.toPath(), content, StandardCharsets.UTF_8);
        return new ExportResult("markdown", timestampedFile, latestFile);
    }

    public ExportResult exportDiscord() throws IOException {
        CatalogSummary summary = refreshAndSave();
        File exportDirectory = exportDirectory();
        Files.createDirectories(exportDirectory.toPath());

        String timestamp = FILE_TIMESTAMP.format(LocalDateTime.now());
        String content = createDiscord(summary);
        File timestampedFile = new File(exportDirectory, "plugins-discord-" + timestamp + ".md");
        File latestFile = new File(exportDirectory, "plugins-discord-latest.md");
        Files.writeString(timestampedFile.toPath(), content, StandardCharsets.UTF_8);
        Files.writeString(latestFile.toPath(), content, StandardCharsets.UTF_8);
        return new ExportResult("discord", timestampedFile, latestFile);
    }

    public int getTrackedPluginCount() {
        YamlConfiguration database = loadDatabase();
        return database.getConfigurationSection("plugins") == null
                ? 0
                : database.getConfigurationSection("plugins").getKeys(false).size();
    }

    public File databaseFile() {
        String configured = owner.getConfig().getString("database.file", "plugins-database.yml");
        File file = new File(configured);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(owner.getDataFolder(), configured);
    }

    private YamlConfiguration loadDatabase() {
        return loadDatabase(databaseFile());
    }

    private YamlConfiguration loadDatabase(File databaseFile) {
        return YamlFiles.load(databaseFile, owner.getLogger());
    }

    public File exportDirectory() {
        String configured = owner.getConfig().getString("exports.directory", "exports");
        File directory = new File(configured);
        if (directory.isAbsolute()) {
            return directory;
        }
        return new File(owner.getDataFolder(), configured);
    }

    public Plugin[] sortedPlugins() {
        Plugin[] plugins = owner.getServer().getPluginManager().getPlugins();
        Arrays.sort(plugins, new PluginComparator());
        return plugins;
    }

    public List<String> urlCategorySuggestions() {
        return URL_CATEGORY_SUGGESTIONS;
    }

    private String createMarkdown(CatalogSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Plugin Versions - ").append(summary.lastSaved()).append("\n\n");
        builder.append("- Server: ").append(owner.getServer().getName()).append(" ")
                .append(owner.getServer().getVersion()).append("\n");
        builder.append("- Bukkit API: ").append(Bukkit.getBukkitVersion()).append("\n");
        builder.append("- Plugins: ").append(summary.total()).append(" total, ")
                .append(summary.enabled()).append(" enabled, ")
                .append(summary.disabled()).append(" disabled\n\n");
        YamlConfiguration database = loadDatabase();
        builder.append("| Plugin | Version | Status | Links | Description |\n");
        builder.append("| --- | --- | --- | --- | --- |\n");

        for (Plugin plugin : sortedPlugins()) {
            builder.append("| ")
                    .append(markdown(plugin.getName()))
                    .append(" | ")
                    .append(markdown(plugin.getPluginMeta().getVersion()))
                    .append(" | ")
                    .append(plugin.isEnabled() ? "Enabled" : "Disabled")
                    .append(" | ")
                    .append(markdownLinks(combinedUrls(database, databaseKey(plugin.getPluginMeta().getName()))))
                    .append(" | ")
                    .append(markdown(clean(plugin.getPluginMeta().getDescription())))
                    .append(" |\n");
        }

        builder.append("\nGenerated by PluginVersions ")
                .append(owner.getPluginMeta().getVersion())
                .append(".\n");
        return builder.toString();
    }

    private String createDiscord(CatalogSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Plugin Versions - ").append(summary.lastSaved()).append("\n\n");
        builder.append("**Server:** ").append(owner.getServer().getName()).append(" ")
                .append(owner.getServer().getVersion()).append("\n");
        builder.append("**Bukkit API:** ").append(Bukkit.getBukkitVersion()).append("\n");
        builder.append("**Plugins:** ").append(summary.total()).append(" total, ")
                .append(summary.enabled()).append(" enabled, ")
                .append(summary.disabled()).append(" disabled\n\n");

        YamlConfiguration database = loadDatabase();
        for (Plugin plugin : sortedPlugins()) {
            String links = markdownLinks(combinedUrls(database, databaseKey(plugin.getPluginMeta().getName())));
            builder.append("- **")
                    .append(markdown(plugin.getName()))
                    .append("** `")
                    .append(markdown(plugin.getPluginMeta().getVersion()))
                    .append("` - ")
                    .append(plugin.isEnabled() ? "Enabled" : "Disabled");
            if (!links.isBlank()) {
                builder.append(" - ").append(links);
            }
            String description = markdown(clean(plugin.getPluginMeta().getDescription()));
            if (!description.isBlank()) {
                builder.append(" - ").append(description);
            }
            builder.append("\n");
        }

        builder.append("\nGenerated by PluginVersions ")
                .append(owner.getPluginMeta().getVersion())
                .append(".\n");
        return builder.toString();
    }

    private void updatePlugin(YamlConfiguration database, Plugin plugin, String now) {
        String internalName = plugin.getPluginMeta().getName();
        String path = "plugins." + databaseKey(internalName);
        String firstSeen = database.getString(path + ".first-seen", now);
        int seenCount = database.getInt(path + ".seen-count", 0) + 1;

        database.set(path + ".human-readable-name", clean(plugin.getPluginMeta().getDisplayName()));
        database.set(path + ".internal-name", internalName);
        database.set(path + ".current-version", clean(plugin.getPluginMeta().getVersion()));
        database.set(path + ".enabled", plugin.isEnabled());
        database.set(path + ".first-seen", firstSeen);
        database.set(path + ".last-seen", now);
        database.set(path + ".seen-count", seenCount);
        database.set(path + ".description", clean(plugin.getPluginMeta().getDescription()));
        database.set(path + ".main-class", clean(plugin.getPluginMeta().getMainClass()));
        database.set(path + ".api-version", clean(plugin.getPluginMeta().getAPIVersion()));
        database.set(path + ".website", clean(plugin.getPluginMeta().getWebsite()));
        database.set(path + ".authors", plugin.getPluginMeta().getAuthors());
        database.set(path + ".contributors", plugin.getPluginMeta().getContributors());
        database.set(path + ".dependencies.required", plugin.getPluginMeta().getPluginDependencies());
        database.set(path + ".dependencies.soft", plugin.getPluginMeta().getPluginSoftDependencies());
        database.set(path + ".dependencies.load-before", plugin.getPluginMeta().getLoadBeforePlugins());
        database.set(path + ".provided-plugins", plugin.getPluginMeta().getProvidedPlugins());
        database.set(path + ".detected-urls", detectUrls(plugin));

        List<Map<?, ?>> history = new ArrayList<>(database.getMapList(path + ".history"));
        if (shouldAppendHistory(history, plugin)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("seen", now);
            entry.put("version", clean(plugin.getPluginMeta().getVersion()));
            entry.put("enabled", plugin.isEnabled());
            entry.put("server", owner.getServer().getVersion());
            history.add(entry);
        }

        int historyLimit = Math.max(1, owner.getConfig().getInt("database.max-history-per-plugin", 25));
        while (history.size() > historyLimit) {
            history.remove(0);
        }
        database.set(path + ".history", history);
    }

    public UrlResult addManualUrl(String pluginName, String rawUrl) {
        return addManualUrl(pluginName, null, rawUrl);
    }

    public UrlResult addManualUrl(String pluginName, String category, String rawUrl) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl == null) {
            return UrlResult.invalid(pluginName, rawUrl);
        }
        String normalizedCategory = normalizeCategory(category, normalizedUrl);
        if (normalizedCategory == null) {
            return UrlResult.invalidCategory(pluginName, category, normalizedUrl);
        }

        YamlConfiguration database = loadDatabaseWithPlugin(pluginName);
        String pluginPath = resolvePluginPath(database, pluginName);
        if (pluginPath == null) {
            return UrlResult.notFound(pluginName, normalizedUrl);
        }

        String path = pluginPath + ".manual-urls." + normalizedCategory;
        List<String> urls = new ArrayList<>(database.getStringList(path));
        if (urls.contains(normalizedUrl)) {
            return UrlResult.unchanged(displayPluginName(database, pluginPath), normalizedCategory, normalizedUrl, urls);
        }

        urls.add(normalizedUrl);
        database.set(path, urls);
        saveDatabase(database);
        return UrlResult.changed(displayPluginName(database, pluginPath), normalizedCategory, normalizedUrl, urls);
    }

    public UrlResult removeManualUrl(String pluginName, String rawUrl) {
        return removeManualUrl(pluginName, null, rawUrl);
    }

    public UrlResult removeManualUrl(String pluginName, String category, String rawUrl) {
        String normalizedUrl = normalizeUrl(rawUrl);
        if (normalizedUrl == null) {
            return UrlResult.invalid(pluginName, rawUrl);
        }
        String normalizedCategory = normalizeCategory(category, normalizedUrl);
        if (normalizedCategory == null) {
            return UrlResult.invalidCategory(pluginName, category, normalizedUrl);
        }

        YamlConfiguration database = loadDatabaseWithPlugin(pluginName);
        String pluginPath = resolvePluginPath(database, pluginName);
        if (pluginPath == null) {
            return UrlResult.notFound(pluginName, normalizedUrl);
        }

        String path = pluginPath + ".manual-urls." + normalizedCategory;
        List<String> urls = new ArrayList<>(database.getStringList(path));
        if (category == null && !urls.contains(normalizedUrl)) {
            UrlLocation location = findManualUrl(database, pluginPath, normalizedUrl);
            if (location != null) {
                normalizedCategory = location.category();
                path = location.path();
                urls = new ArrayList<>(location.urls());
            }
        }
        if (!urls.remove(normalizedUrl)) {
            return UrlResult.unchanged(displayPluginName(database, pluginPath), normalizedCategory, normalizedUrl, urls);
        }

        database.set(path, urls.isEmpty() ? null : urls);
        saveDatabase(database);
        return UrlResult.changed(displayPluginName(database, pluginPath), normalizedCategory, normalizedUrl, urls);
    }

    public UrlList manualUrls(String pluginName) {
        YamlConfiguration database = loadDatabaseWithPlugin(pluginName);
        String pluginPath = resolvePluginPath(database, pluginName);
        if (pluginPath == null) {
            return UrlList.notFound(pluginName);
        }

        return new UrlList(true, displayPluginName(database, pluginPath), urlsFromListSection(database, pluginPath + ".manual-urls"));
    }

    public List<UrlAuditEntry> urlAudit() {
        refreshAndSave();
        YamlConfiguration database = loadDatabase();
        List<UrlAuditEntry> entries = new ArrayList<>();
        if (database.getConfigurationSection("plugins") == null) {
            return entries;
        }

        for (String key : database.getConfigurationSection("plugins").getKeys(false)) {
            String pluginPath = "plugins." + key;
            Map<String, List<String>> manualUrls = urlsFromListSection(database, pluginPath + ".manual-urls");
            Map<String, String> detectedUrls = urlsFromStringSection(database, pluginPath + ".detected-urls");
            int manualCount = countUrls(manualUrls);
            int detectedCount = detectedUrls.size();
            String status = "manual";
            if (manualCount == 0 && detectedCount == 0) {
                status = "missing";
            } else if (manualCount == 0) {
                status = "detected-only";
            }

            Set<String> categories = new LinkedHashSet<>();
            categories.addAll(detectedUrls.keySet());
            categories.addAll(manualUrls.keySet());
            entries.add(new UrlAuditEntry(displayPluginName(database, pluginPath), status, detectedCount, manualCount,
                    categories.isEmpty() ? "none" : String.join(", ", categories)));
        }

        entries.sort((first, second) -> first.pluginName().compareToIgnoreCase(second.pluginName()));
        return entries;
    }

    public String urlFor(String pluginName, String category) {
        String normalizedCategory = normalizeCategoryLabel(category);
        if (normalizedCategory == null) {
            return "";
        }

        YamlConfiguration database = loadDatabase();
        String pluginPath = resolvePluginPath(database, pluginName);
        if (pluginPath == null) {
            return "";
        }

        List<String> urls = combinedUrls(database, databaseKeyFromPath(pluginPath)).get(normalizedCategory);
        return urls == null || urls.isEmpty() ? "" : urls.getFirst();
    }

    public List<String> trackedPluginNames() {
        YamlConfiguration database = loadDatabase();
        List<String> names = new ArrayList<>();
        if (database.getConfigurationSection("plugins") != null) {
            for (String key : database.getConfigurationSection("plugins").getKeys(false)) {
                String name = database.getString("plugins." + key + ".internal-name");
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        for (Plugin plugin : sortedPlugins()) {
            if (!names.contains(plugin.getName())) {
                names.add(plugin.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private Map<String, String> detectUrls(Plugin plugin) {
        Map<String, String> urls = new LinkedHashMap<>();
        Set<String> candidates = new LinkedHashSet<>();
        addUrls(candidates, plugin.getPluginMeta().getWebsite());
        addUrls(candidates, plugin.getPluginMeta().getDescription());

        for (String candidate : candidates) {
            urls.putIfAbsent(categorizeUrl(candidate), candidate);
        }

        String website = clean(plugin.getPluginMeta().getWebsite());
        if (!website.isBlank()) {
            urls.putIfAbsent("website", website);
        }
        return urls;
    }

    private Map<String, List<String>> combinedUrls(YamlConfiguration database, String pluginKey) {
        Map<String, List<String>> urls = new LinkedHashMap<>();
        String pluginPath = "plugins." + pluginKey;

        for (Map.Entry<String, String> entry : urlsFromStringSection(database, pluginPath + ".detected-urls").entrySet()) {
            urls.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue());
        }
        for (Map.Entry<String, List<String>> entry : urlsFromListSection(database, pluginPath + ".manual-urls").entrySet()) {
            List<String> target = urls.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>());
            for (String url : entry.getValue()) {
                if (!target.contains(url)) {
                    target.add(url);
                }
            }
        }
        return urls;
    }

    private int countUrls(Map<String, List<String>> urls) {
        int count = 0;
        for (List<String> categoryUrls : urls.values()) {
            count += categoryUrls.size();
        }
        return count;
    }

    private UrlLocation findManualUrl(YamlConfiguration database, String pluginPath, String normalizedUrl) {
        if (database.getConfigurationSection(pluginPath + ".manual-urls") == null) {
            return null;
        }

        for (String category : database.getConfigurationSection(pluginPath + ".manual-urls").getKeys(false)) {
            String path = pluginPath + ".manual-urls." + category;
            List<String> urls = database.getStringList(path);
            if (urls.contains(normalizedUrl)) {
                return new UrlLocation(category, path, urls);
            }
        }
        return null;
    }

    private Map<String, String> urlsFromStringSection(YamlConfiguration database, String path) {
        Map<String, String> urls = new LinkedHashMap<>();
        if (database.getConfigurationSection(path) == null) {
            return urls;
        }
        for (String key : database.getConfigurationSection(path).getKeys(false)) {
            String value = database.getString(path + "." + key);
            if (value != null && !value.isBlank()) {
                urls.put(key, value);
            }
        }
        return urls;
    }

    private Map<String, List<String>> urlsFromListSection(YamlConfiguration database, String path) {
        Map<String, List<String>> urls = new LinkedHashMap<>();
        if (database.getConfigurationSection(path) == null) {
            return urls;
        }
        for (String key : database.getConfigurationSection(path).getKeys(false)) {
            List<String> values = database.getStringList(path + "." + key);
            if (!values.isEmpty()) {
                urls.put(key, values);
            }
        }
        return urls;
    }

    private String markdownLinks(Map<String, List<String>> urls) {
        List<String> links = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : urls.entrySet()) {
            for (String url : entry.getValue()) {
                links.add("[" + markdown(entry.getKey()) + "](" + url + ")");
            }
        }
        return String.join(", ", links);
    }

    private void addUrls(Set<String> urls, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        Matcher matcher = URL_PATTERN.matcher(value);
        while (matcher.find()) {
            urls.add(matcher.group());
        }

        if (value.startsWith("www.")) {
            urls.add("https://" + value);
        } else if (value.contains(".") && !value.contains(" ") && !value.startsWith("http")) {
            urls.add("https://" + value);
        }
    }

    private int countEnabled(Plugin[] plugins) {
        int enabled = 0;
        for (Plugin plugin : plugins) {
            if (plugin.isEnabled()) {
                enabled++;
            }
        }
        return enabled;
    }

    private YamlConfiguration loadDatabaseWithPlugin(String pluginName) {
        refreshAndSave();
        return loadDatabase();
    }

    private String resolvePluginPath(YamlConfiguration database, String pluginName) {
        if (database.getConfigurationSection("plugins") == null) {
            return null;
        }

        String normalizedName = pluginName.toLowerCase(Locale.ROOT);
        for (String key : database.getConfigurationSection("plugins").getKeys(false)) {
            String path = "plugins." + key;
            String internalName = database.getString(path + ".internal-name", "");
            String humanReadableName = database.getString(path + ".human-readable-name", "");
            if (key.equalsIgnoreCase(pluginName) || internalName.equalsIgnoreCase(pluginName) || humanReadableName.equalsIgnoreCase(pluginName)) {
                return path;
            }
            if (databaseKey(internalName).equals(normalizedName)) {
                return path;
            }
        }
        return null;
    }

    private String displayPluginName(YamlConfiguration database, String pluginPath) {
        String internalName = database.getString(pluginPath + ".internal-name", "");
        return internalName.isBlank() ? database.getString(pluginPath + ".human-readable-name", "unknown") : internalName;
    }

    private void saveDatabase(YamlConfiguration database) {
        try {
            ensureParent(databaseFile());
            database.save(databaseFile());
        } catch (IOException e) {
            owner.getLogger().warning("Could not save plugin database: " + e.getMessage());
        }
    }

    private String normalizeCategory(String category, String url) {
        if (category == null || category.isBlank()) {
            return categorizeUrl(url);
        }
        return normalizeCategoryLabel(category);
    }

    private String normalizeCategoryLabel(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
        normalized = switch (normalized) {
            case "git", "git-hub" -> "github";
            case "devbukkit", "bukkitdev" -> "dev-bukkit";
            case "paper-hangar" -> "hangar";
            default -> normalized;
        };
        if (!URL_CATEGORY_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private String normalizeUrl(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return null;
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String categorizeUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("github.com")) {
            return "github";
        }
        if (lower.contains("modrinth.com")) {
            return "modrinth";
        }
        if (lower.contains("hangar.papermc.io")) {
            return "hangar";
        }
        if (lower.contains("spigotmc.org")) {
            return "spigot";
        }
        if (lower.contains("dev.bukkit.org")) {
            return "dev-bukkit";
        }
        if (lower.contains("jenkins") || lower.contains("/job/")) {
            return "jenkins";
        }
        if (lower.contains("ci.")) {
            return "ci";
        }
        return "website";
    }

    private boolean shouldAppendHistory(List<Map<?, ?>> history, Plugin plugin) {
        if (history.isEmpty()) {
            return true;
        }

        Map<?, ?> previous = history.getLast();
        return !Objects.equals(String.valueOf(previous.get("version")), clean(plugin.getPluginMeta().getVersion()))
                || !Objects.equals(previous.get("enabled"), plugin.isEnabled())
                || !Objects.equals(String.valueOf(previous.get("server")), owner.getServer().getVersion());
    }

    private String databaseKey(String internalName) {
        String slug = internalName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "plugin";
        }
        return slug + "-" + Integer.toUnsignedString(internalName.hashCode(), 16);
    }

    private String databaseKeyFromPath(String pluginPath) {
        return pluginPath.substring("plugins.".length());
    }

    private String timestamp() {
        return TIMESTAMP.format(LocalDateTime.now());
    }

    private void ensureParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value;
    }

    private String markdown(String value) {
        return clean(value).replace("|", "\\|").replace("\n", " ").replace("\r", " ");
    }

    public record CatalogSummary(int total, int enabled, int disabled, File databaseFile, String lastSaved) {
    }

    public record ExportResult(String format, File timestampedFile, File latestFile) {
    }

    private record UrlLocation(String category, String path, List<String> urls) {
    }

    public record UrlAuditEntry(String pluginName, String status, int detectedCount, int manualCount, String categories) {
    }

    public record UrlResult(boolean found, boolean valid, boolean categoryValid, boolean changed, String pluginName, String category, String url, List<String> urls) {
        public static UrlResult invalid(String pluginName, String url) {
            return new UrlResult(true, false, true, false, pluginName, "", url, List.of());
        }

        public static UrlResult invalidCategory(String pluginName, String category, String url) {
            return new UrlResult(true, true, false, false, pluginName, category == null ? "" : category, url, List.of());
        }

        public static UrlResult notFound(String pluginName, String url) {
            return new UrlResult(false, true, true, false, pluginName, "", url, List.of());
        }

        public static UrlResult unchanged(String pluginName, String category, String url, List<String> urls) {
            return new UrlResult(true, true, true, false, pluginName, category, url, List.copyOf(urls));
        }

        public static UrlResult changed(String pluginName, String category, String url, List<String> urls) {
            return new UrlResult(true, true, true, true, pluginName, category, url, List.copyOf(urls));
        }
    }

    public record UrlList(boolean found, String pluginName, Map<String, List<String>> urls) {
        public static UrlList notFound(String pluginName) {
            return new UrlList(false, pluginName, Map.of());
        }
    }
}
