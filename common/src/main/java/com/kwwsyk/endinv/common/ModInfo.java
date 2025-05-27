package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.client.option.IClientConfig;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.util.SortType;

import java.util.UUID;

public final class ModInfo {

    public static final String MOD_ID = "endless_inventory";

    public static final UUID DEFAULT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    private static IServerConfig serverConfig;

    private static IClientConfig clientConfig;

    public static SortType.ISortHelper sortHelper;

    public static IClientConfig getClientConfig() {
        return clientConfig;
    }

    public static void setClientConfig(IClientConfig clientConfig) {
        if(ModInfo.clientConfig!=null) throw new IllegalStateException("Try to set config when config has been initialized.");
        ModInfo.clientConfig = clientConfig;
    }

    public static IServerConfig getServerConfig() {
        return serverConfig;
    }

    public static void setServerConfig(IServerConfig serverConfig) {
        if(ModInfo.clientConfig!=null) throw new IllegalStateException("Try to set config when config has been initialized.");
        ModInfo.serverConfig = serverConfig;
    }
}
