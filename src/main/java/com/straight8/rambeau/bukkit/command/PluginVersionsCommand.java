package com.straight8.rambeau.bukkit.command;

import com.straight8.rambeau.bukkit.PluginVersionsBukkit;
import com.straight8.rambeau.bukkit.command.sub.ListSub;
import com.straight8.rambeau.bukkit.command.sub.ReloadSub;
import dev.ratas.slimedogcore.impl.commands.BukkitFacingParentCommand;

public class PluginVersionsCommand extends BukkitFacingParentCommand {

    public PluginVersionsCommand(PluginVersionsBukkit plugin) {
        addSubCommand(new ListSub(plugin));
        addSubCommand(new ReloadSub(plugin));
    }

}