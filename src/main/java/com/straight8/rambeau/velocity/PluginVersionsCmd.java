package com.straight8.rambeau.velocity;

import com.straight8.rambeau.util.CommandPageUtils;
import com.straight8.rambeau.util.StringUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class PluginVersionsCmd implements RawCommand {
    private static final int LINES_PER_PAGE = 10;
    private static final char COLOR_STR = '&';
    private final PluginVersionsVelocity plugin;

    public PluginVersionsCmd(PluginVersionsVelocity plugin) {
        this.plugin = plugin;
    }

    private void showUsage(Invocation invocation) {
        CommandSource source = invocation.source();
        StringBuilder sb = new StringBuilder();
        if (source.hasPermission("pluginversions.list")) {
            sb.append("/pvv list [page]");
        }
        if (source.hasPermission("pluginversions.reload")) {
            if (sb.length() > 1) {
                sb.append("\n");
            }
            sb.append("/pvv reload");
        }
        source.sendMessage(Component.text(sb.toString()));
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        // Get the arguments after the command alias
        String[] args = invocation.arguments().split(" ");

        if (args.length == 0 || args[0].isEmpty()) {
            showUsage(invocation);
            return;
        }
        String cmdLowercase = args[0].toLowerCase();

        if (sender instanceof Player) {
            if (!sender.hasPermission("pluginversions." + cmdLowercase)) {
                String senderName = ((Player) sender).getUsername();
                sender.sendMessage(Component.text("You do not have permission to run this command"));
                this.plugin
                        .log(senderName + " attempted to run command pv " + cmdLowercase + ", but lacked permissions");
                return;
            }
        }

        if (cmdLowercase.equals("list")) {
            List<PluginContainer> pluginList = new ArrayList<>(this.plugin.getServer().getPluginManager().getPlugins());

            if (pluginList.isEmpty()) {
                sender.sendMessage(Component.text("No plugins loaded"));
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

            if (page > 0) {
                if (((page - 1) * LINES_PER_PAGE) < pluginList.size()) {
                    String msg = plugin.getConfig().getString("page-header-format",
                            "PluginVersions ===== page {page} =====");
                    sender.sendMessage(Color.color(msg.replace("{page}", String.valueOf(page))));
                }
                int maxSpacing = CommandPageUtils.getMaxNameLength(plugin -> {
                    Optional<String> name = plugin.getDescription().getName();
                    return name.isPresent() ? name.get() : "N/A";
                }, CommandPageUtils.getPage(pluginList, page, LINES_PER_PAGE));
                for (int i = ((page - 1) * LINES_PER_PAGE); i < pluginList.size() && i < (page * LINES_PER_PAGE); i++) {
                    PluginContainer p = pluginList.get(i);

                    String msg = plugin.getConfig().getString("enabled-version-format", "&a{name}{spacing}&e{version}");
                    String spacing = CommandPageUtils.getSpacingFor(p.getDescription().getName().get(), maxSpacing,
                            sender instanceof Player);
                    if (p.getDescription().getName().isPresent() && p.getDescription().getVersion().isPresent()) {
                        Component comp = Color
                                .color(msg.replace("{name}", p.getDescription().getName().get()).replace("{version}",
                                        p.getDescription().getVersion().get()).replace("{spacing}", spacing));
                        sender.sendMessage(comp);
                    } else if (p.getDescription().getId().equalsIgnoreCase("serverlistplus")) {
                        Component comp = Color.color(msg.replace("{name}", SLPUtils.getSLPName()).replace("{version}",
                                SLPUtils.getSLPVersion()).replace("{spacing}", spacing));
                        sender.sendMessage(comp);
                    }
                }
            } else {
                int maxSpacing = CommandPageUtils.getMaxNameLength(plugin -> {
                    Optional<String> name = plugin.getDescription().getName();
                    return name.isPresent() ? name.get() : "N/A";
                }, pluginList);
                for (PluginContainer p : pluginList) {
                    String msg = plugin.getConfig().getString("enabled-version-format", "&a{name}{spacing}&e{version}");
                    String spacing = CommandPageUtils.getSpacingFor(p.getDescription().getName().get(), maxSpacing,
                            sender instanceof Player);
                    if (p.getDescription().getName().isPresent() && p.getDescription().getVersion().isPresent()) {
                        Component comp = Color
                                .color(msg.replace("{name}", p.getDescription().getName().get()).replace("{version}",
                                        p.getDescription().getVersion().get()).replace("{spacing}", spacing));
                        sender.sendMessage(comp);
                    } else if (p.getDescription().getId().equalsIgnoreCase("serverlistplus")) {
                        Component comp = Color
                                .color(msg.replace("{name}", p.getDescription().getName().get()).replace("{version}",
                                        p.getDescription().getVersion().get()).replace("{spacing}", spacing));
                        sender.sendMessage(comp);
                    }
                }
            }
            // break;
        } else if (cmdLowercase.equals("reload")) {
            YamlConfig.createFiles("config");
            PluginVersionsVelocity.getInstance().ReadConfigValuesFromFile();

            sender.sendMessage(Component.text("Reloaded §bPluginVersions/config.yml"));
        } else {
            sender.sendMessage(Component.text("Unrecognized command option " + cmdLowercase));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments().split(" ");
        if ((args.length == 1 && !invocation.arguments().endsWith(" "))
                || (args.length == 0 && invocation.arguments().endsWith(" "))) {
            List<String> options = new ArrayList<>();
            if (invocation.source().hasPermission("pluginversions.list")) {
                options.add("list");
            }
            if (invocation.source().hasPermission("pluginversions.reload")) {
                options.add("relaod");
            }
            if (args.length < 1) {
                return options;
            }
            return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
        } else if (args[0].equalsIgnoreCase("list") && ((args.length == 2 && !invocation.arguments().endsWith(" ")) ||
                args.length == 1 && invocation.arguments().endsWith(" "))) {
            String arg = args.length == 2 ? args[1] : "";
            return CommandPageUtils.getNextInteger(arg,
                    (plugin.getServer().getPluginManager().getPlugins().size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        } else {
            return Collections.emptyList();
        }
    }

    private enum Color {
        BLACK('0', NamedTextColor.BLACK),
        DARK_BLUE('1', NamedTextColor.DARK_BLUE),
        DARK_GREEN('2', NamedTextColor.DARK_GREEN),
        DARK_AQUA('3', NamedTextColor.DARK_AQUA),
        DARK_RED('4', NamedTextColor.DARK_RED),
        DARK_PURPLE('5', NamedTextColor.DARK_PURPLE),
        GOLD('6', NamedTextColor.GOLD),
        GRAY('7', NamedTextColor.GRAY),
        DARK_GRAY('8', NamedTextColor.DARK_GRAY),
        BLUE('9', NamedTextColor.BLUE),
        GREEN('a', NamedTextColor.GREEN),
        AQUA('b', NamedTextColor.AQUA),
        RED('c', NamedTextColor.RED),
        LIGHT_PURPLE('d', NamedTextColor.LIGHT_PURPLE),
        YELLOW('e', NamedTextColor.YELLOW),
        WHITE('f', NamedTextColor.WHITE),
        // MAGIC('k', NamedTextColor.MAGIC),
        BOLD('l', NamedTextColor.BLUE),
        // UNDERLINE('n', NamedTextColor.UNDERLINE),
        // ITALIC('o', NamedTextColor.ITALIC),
        // RESET('r', NamedTextColor.RESET),
        ;

        private static final String COLOR_CHARS;

        static {
            StringBuilder colorChars = new StringBuilder();
            for (Color color : values()) {
                colorChars.append(color.c);
            }
            COLOR_CHARS = colorChars.toString();
        }

        private final char c;
        private final TextColor color;

        Color(char c, TextColor color) {
            this.c = c;
            this.color = color;
        }

        public static Component color(String msg) {
            List<Component> compList = new ArrayList<>();
            char[] charArray = msg.toCharArray();
            TextColor nextColor = NamedTextColor.WHITE;
            boolean prevHadColor = false;
            int lastEnd = -2;
            for (int i = 0; i < charArray.length - 1; i++) {
                if (prevHadColor) {
                    prevHadColor = false;
                    continue;
                }
                char potColorChar = charArray[i];
                char charAfter = charArray[i + 1];
                int index = COLOR_CHARS.indexOf(String.valueOf(charAfter));
                if (potColorChar == COLOR_STR && index >= 0) {
                    String msgUntil = msg.substring(lastEnd + 2, i);
                    compList.add(Component.text(msgUntil, nextColor));
                    nextColor = values()[index].color;
                    lastEnd = i;
                    prevHadColor = true;
                }
            }
            if (lastEnd + 2 < msg.length()) {
                compList.add(Component.text(msg.substring(lastEnd + 2, msg.length()), nextColor));
            }
            return Component.join(JoinConfiguration.noSeparators(), compList);
        }

    }

}
