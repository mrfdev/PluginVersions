package com.straight8.rambeau.bukkit;

import java.util.Comparator;
import org.bukkit.plugin.Plugin;

public class PluginComparator implements Comparator<Plugin> {

    @Override
    public int compare(Plugin p1, Plugin p2) {
        if (p1 == null || p2 == null) {
            throw new ClassCastException();
        }
        return p1.getName().compareToIgnoreCase(p2.getName());
    }
}
