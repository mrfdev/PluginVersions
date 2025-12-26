package com.straight8.rambeau.bukkit.command.sub;

import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import dev.ratas.slimedogcore.api.commands.SDCCommandOptionSet;
import dev.ratas.slimedogcore.api.messaging.recipient.SDCRecipient;
import dev.ratas.slimedogcore.impl.commands.AbstractSubCommand;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;

public class ReloadSub extends AbstractSubCommand {
    private static final String NAME = "reload";
    private static final String PERMS = "pluginversions.reload";
    private static final String USAGE = "/pv reload";
    private final PluginVersionsBukkit plugin;

    public ReloadSub(PluginVersionsBukkit plugin) {
        super(NAME, PERMS, USAGE, true, false);
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(SDCRecipient sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onOptionedCommand(SDCRecipient sender, String[] args, SDCCommandOptionSet opts) {
        plugin.CreateConfigFileIfMissing();
        plugin.ReadConfigValuesFromFile();
        sender.sendRawMessage("Reloaded " + ChatColor.AQUA + this.getName() + "/config.yml");
        return true;
    }

}
