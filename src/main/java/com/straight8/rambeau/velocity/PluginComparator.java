package com.straight8.rambeau.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import java.util.Comparator;

public class PluginComparator implements Comparator<PluginContainer> {

    @Override
    public int compare(PluginContainer p1, PluginContainer p2) {
        if (!(p1 instanceof PluginContainer) || !(p2 instanceof PluginContainer)) {
            throw new ClassCastException();
        }

        String name1;
        String name2;

        if (p1.getDescription().getName().isPresent()) {
            name1 = p1.getDescription().getName().get();
        } else if (p1.getDescription().getId().equalsIgnoreCase("serverlistplus")) {
            name1 = SLPUtils.getSLPName();
        } else {
            name1 = "";
        }

        if (p2.getDescription().getName().isPresent()) {
            name2 = p2.getDescription().getName().get();
        } else if (p2.getDescription().getId().equalsIgnoreCase("serverlistplus")) {
            name2 = SLPUtils.getSLPName();
        } else {
            name2 = "";
        }

        if (name1 != null && name2 != null) {
            return name1.compareToIgnoreCase(name2);
        }
        return -1;
    }
}
