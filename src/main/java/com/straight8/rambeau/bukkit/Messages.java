package com.straight8.rambeau.bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

public final class Messages {
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private final File localeFile;
    private YamlConfiguration locale;

    public Messages(PluginVersionsBukkit plugin) {
        String configuredLocale = plugin.getConfig().getString("settings.locale", "EN");
        File configuredFile = new File(plugin.getDataFolder(), "translations/Locale_" + configuredLocale + ".yml");
        if (!configuredFile.exists() && !"EN".equalsIgnoreCase(configuredLocale)) {
            plugin.getLogger().warning("Locale_" + configuredLocale + ".yml was not found. Falling back to Locale_EN.yml.");
            configuredFile = new File(plugin.getDataFolder(), "translations/Locale_EN.yml");
        }
        localeFile = configuredFile;
        if (!localeFile.exists() && "Locale_EN.yml".equals(localeFile.getName())) {
            plugin.saveResource("translations/Locale_EN.yml", false);
        }
        reload();
    }

    public void reload() {
        locale = YamlConfiguration.loadConfiguration(localeFile);
    }

    public String raw(String path) {
        String value = locale.getString(path);
        if (value == null) {
            return "<#f28482>Missing language phrase: " + MINI_MESSAGE.escapeTags(path);
        }
        return legacyToMiniMessage(value);
    }

    public List<String> list(String path) {
        if (locale.isList(path)) {
            List<String> values = new ArrayList<>();
            for (String value : locale.getStringList(path)) {
                values.add(legacyToMiniMessage(value));
            }
            return values;
        }

        String value = locale.getString(path);
        if (value == null) {
            return Collections.emptyList();
        }
        return List.of(legacyToMiniMessage(value));
    }

    public void send(CommandSender sender, String path, Token... tokens) {
        sendRaw(sender, raw("prefix") + format(raw(path), tokens));
    }

    public void sendRaw(CommandSender sender, String message, Token... tokens) {
        sender.sendRichMessage(format(message, tokens));
    }

    public void sendList(CommandSender sender, String path, Token... tokens) {
        for (String line : list(path)) {
            sendRaw(sender, line, tokens);
        }
    }

    public String format(String message, Token... tokens) {
        String formatted = message;
        for (Token token : tokens) {
            formatted = formatted.replace(token.name(), token.value());
        }
        return formatted;
    }

    public static Token token(String name, Object value) {
        return new Token("{" + name + "}", MINI_MESSAGE.escapeTags(String.valueOf(value)));
    }

    public static String legacyToMiniMessage(String value) {
        if (value == null || !LEGACY_COLOR_PATTERN.matcher(value).find()) {
            return value;
        }
        return MINI_MESSAGE.serialize(LEGACY_AMPERSAND.deserialize(value));
    }

    public record Token(String name, String value) {
    }

    public static Token[] tokens(Map<String, ?> values) {
        return values.entrySet().stream()
                .map(entry -> token(entry.getKey(), entry.getValue()))
                .toArray(Token[]::new);
    }
}
