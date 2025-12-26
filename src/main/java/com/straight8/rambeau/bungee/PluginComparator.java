package com.straight8.rambeau.bungee;

import java.util.Comparator;
import net.md_5.bungee.api.plugin.Plugin;

public class PluginComparator implements Comparator<Plugin> {

    @Override
    public int compare(Plugin p1, Plugin p2) {
        if (!(p1 instanceof Plugin) || !(p2 instanceof Plugin)) {
            throw new ClassCastException();
        }
        return p1.getDescription().getName().compareToIgnoreCase(p2.getDescription().getName());
    }
}
