package com.straight8.rambeau.bukkit;

import dev.ratas.slimedogcore.api.config.SDCCustomConfig;
import dev.ratas.slimedogcore.api.messaging.factory.SDCSingleContextMessageFactory;
import dev.ratas.slimedogcore.api.messaging.factory.SDCTripleContextMessageFactory;
import dev.ratas.slimedogcore.impl.messaging.MessagesBase;
import dev.ratas.slimedogcore.impl.messaging.factory.MsgUtil;

public class Messages extends MessagesBase {
    private SDCSingleContextMessageFactory<Integer> pageHeader;
    private SDCTripleContextMessageFactory<String, String, String> enabledVersion;
    private SDCTripleContextMessageFactory<String, String, String> disabledVersion;

    protected Messages(SDCCustomConfig config) {
        super(config);
        load();
    }

    private void load() {
        pageHeader = MsgUtil.singleContext("{page}", String::valueOf,
                getRawMessage("page-header-format", "PluginVersions ===== page {page} ====="));
        enabledVersion = MsgUtil.tripleContext("{name}", name -> name, "{spacing}", spacing -> spacing, "{version}",
                version -> version,
                getRawMessage("enabled-version-format", " - &a{name}{spacing}&e{version}"));
        disabledVersion = MsgUtil.tripleContext("{name}", name -> name, "{spacing}", spacing -> spacing, "{version}",
                version -> version, getRawMessage("disabled-version-format", " - &c{name}{spacing}&e{version}"));
    }

    @Override
    public void reload() {
        super.reload();
        load();
    }

    public SDCSingleContextMessageFactory<Integer> getPageHeader() {
        return pageHeader;
    }

    public SDCTripleContextMessageFactory<String, String, String> getEnabledVersion() {
        return enabledVersion;
    }

    public SDCTripleContextMessageFactory<String, String, String> getDisabledVersion() {
        return disabledVersion;
    }

}
