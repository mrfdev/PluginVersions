package com.straight8.rambeau.velocity;

import net.minecrell.serverlistplus.core.CoreDescription;
import net.minecrell.serverlistplus.core.ServerListPlusCore;

public class SLPUtils {

    public static String getSLPName() {
        CoreDescription info = CoreDescription.load(ServerListPlusCore.getInstance());

        return info.getName();
    }

    public static String getSLPVersion() {
        CoreDescription info = CoreDescription.load(ServerListPlusCore.getInstance());

        return info.getVersion();
    }
}
