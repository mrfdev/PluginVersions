package com.straight8.rambeau.bungee;

import com.straight8.rambeau.util.CommandPageUtils;
import com.straight8.rambeau.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

public class PluginVersionsCmd extends Command implements TabExecutor {
    private static final int LINES_PER_PAGE = 10;

    private final PluginVersionsBungee plugin;

    public PluginVersionsCmd(PluginVersionsBungee plugin) {
        super("pluginversions", "pluginversions.list", "pvb");

        this.plugin = plugin;
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void showUsage(CommandSender source) {
        StringBuilder sb = new StringBuilder();
        if (source.hasPermission("pluginversions.list")) {
            sb.append("/pvb list [page]");
        }
        if (source.hasPermission("pluginversions.reload")) {
            if (sb.length() > 1) {
                sb.append("\n");
            }
            sb.append("/pvb reload");
        }
        source.sendMessage(sb.toString());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return;
        }
        String cmdLowercase = args[0].toLowerCase();

        if (sender instanceof ProxiedPlayer) {
            if (!sender.hasPermission("pluginversions." + cmdLowercase)) {
                String senderName = sender.getName();
                sender.sendMessage("You do not have permission to run this command");
                this.plugin
                        .log(senderName + " attempted to run command pv " + cmdLowercase + ", but lacked permissions");
                return;
            }
        }

        if (cmdLowercase.equals("list")) {
            List<Plugin> pluginList = new ArrayList<>(this.plugin.getProxy().getPluginManager().getPlugins());

            if (pluginList.isEmpty()) {
                sender.sendMessage("No plugins loaded");
                return;
            }

            pluginList.sort(new PluginComparator());

            // Identify the page to display. Page 0 indicates the entire list.
            int page = 0;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (Exception ignored) {
                }
            }
            // Set page to 0 if illegal page was requested
            page = Math.max(page, 0);

            int linesPerPage = 10;
            if (page > 0) {
                int maxSpacing = CommandPageUtils.getMaxNameLength(plugin -> plugin.getDescription().getName(),
                        CommandPageUtils.getPage(pluginList, page, LINES_PER_PAGE));
                if (((page - 1) * linesPerPage) < pluginList.size()) {
                    String msg = color(plugin.getConfig().getString("page-header-format",
                            "PluginVersions ===== page {page} ====="));
                    sender.sendMessage("PluginVersions ===== page " + page + " =====");
                }
                for (int i = ((page - 1) * linesPerPage); i < pluginList.size() && i < (page * linesPerPage); i++) {
                    Plugin p = pluginList.get(i);
                    String msg = color(
                            plugin.getConfig().getString("enabled-version-format", "&a{name}{spacing}&e{version}"));
                    String spacing = CommandPageUtils.getSpacingFor(p.getDescription().getName(), maxSpacing,
                            sender instanceof ProxiedPlayer);
                    sender.sendMessage(msg.replace("{name}", p.getDescription().getName()).replace("{version}",
                            p.getDescription().getVersion()).replace("{spacing}", spacing));
                }
            } else {
                int maxSpacing = CommandPageUtils.getMaxNameLength(plugin -> plugin.getDescription().getName(),
                        pluginList);
                for (Plugin p : pluginList) {
                    String msg = color(
                            plugin.getConfig().getString("enabled-version-format", "&a{name}{spacing}&e{version}"));
                    String spacing = CommandPageUtils.getSpacingFor(p.getDescription().getName(), maxSpacing,
                            sender instanceof ProxiedPlayer);
                    sender.sendMessage(msg.replace("{name}", p.getDescription().getName()).replace("{version}",
                            p.getDescription().getVersion()).replace("{spacing}", spacing));
                }
            }
            // break;
        } else if (cmdLowercase.equals("reload")) {
            YamlConfig.createFiles("config");
            PluginVersionsBungee.getInstance().ReadConfigValuesFromFile();

            sender.sendMessage("Reloaded §bPluginVersions/config.yml");
        } else {
            sender.sendMessage("Unrecognized command option " + cmdLowercase);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("pluginversions.list")) {
                options.add("list");
            }
            if (sender.hasPermission("pluginversions.reload")) {
                options.add("relaod");
            }
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return CommandPageUtils.getNextInteger(args[1],
                    (plugin.getProxy().getPluginManager().getPlugins().size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        } else {
            return Collections.emptyList();
        }
    }

}
