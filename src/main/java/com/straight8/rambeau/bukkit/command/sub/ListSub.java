package com.straight8.rambeau.bukkit.command.sub;

import com.straight8.rambeau.bukkit.PluginComparator;
import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import com.straight8.rambeau.util.CommandPageUtils;
import dev.ratas.slimedogcore.api.commands.SDCCommandOptionSet;
import dev.ratas.slimedogcore.api.messaging.recipient.SDCPlayerRecipient;
import dev.ratas.slimedogcore.api.messaging.recipient.SDCRecipient;
import dev.ratas.slimedogcore.impl.commands.AbstractSubCommand;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

public class ListSub extends AbstractSubCommand {

    private static final String NAME = "list";
    private static final String PERMS = "pluginversions.list";
    private static final String USAGE = "/pv list [page]";
    private static final int LINES_PER_PAGE = 10;

    private final PluginVersionsBukkit plugin;

    public ListSub(PluginVersionsBukkit plugin) {
        super(NAME, PERMS, USAGE, true, false);
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(SDCRecipient sender, String @NonNull [] args) {
        if (args.length == 1 && !(sender instanceof SDCPlayerRecipient)) {
            var totalPages = (plugin.getServer().getPluginManager().getPlugins().length + LINES_PER_PAGE - 1) / LINES_PER_PAGE;
            return CommandPageUtils.getNextInteger(args[0], totalPages);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onOptionedCommand(SDCRecipient sender, String[] args, SDCCommandOptionSet opts) {
        var pluginList = plugin.getServer().getPluginManager().getPlugins();
        if (pluginList.length == 0) {
            sender.sendRawMessage("No plugins loaded");
            return true;
        }

        Arrays.sort(pluginList, new PluginComparator());

        // Determine page to display
        int page = Arrays.stream(args).findFirst().map(arg -> {
            try {
                return Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                return 0;
            }
        }).orElse(0);
        page = Math.max(page, 0);

        if (page > 0) {
            if ((page - 1) * LINES_PER_PAGE < pluginList.length) {
                sender.sendMessage(plugin.getMessages().getPageHeader().createWith(page));
            }
            int maxSpacing = CommandPageUtils.getMaxNameLength(Plugin::getName, CommandPageUtils.getPage(Arrays.asList(pluginList), page, LINES_PER_PAGE));

            for (int i = (page - 1) * LINES_PER_PAGE; i < pluginList.length && i < page * LINES_PER_PAGE; i++) {
                sendPluginMessage(sender, pluginList[i], maxSpacing);
            }
        } else {
            int maxSpacing = CommandPageUtils.getMaxNameLength(Plugin::getName, Arrays.asList(pluginList));
            for (var p : pluginList) {
                sendPluginMessage(sender, p, maxSpacing);
            }
        }

        return true;
    }

    private void sendPluginMessage(@NonNull SDCRecipient sender, @NonNull Plugin p, int maxSpacing) {
        var msg = p.isEnabled() ? plugin.getMessages().getEnabledVersion() : plugin.getMessages().getDisabledVersion();
        String spacing = CommandPageUtils.getSpacingFor(p.getName(), maxSpacing, sender instanceof SDCPlayerRecipient);
        sender.sendMessage(msg.createWith(p.getName(), spacing, p.getPluginMeta().getVersion()));
    }
}