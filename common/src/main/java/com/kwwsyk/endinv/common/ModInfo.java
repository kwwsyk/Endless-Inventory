package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.network.IPacketDistributor;
import com.kwwsyk.endinv.common.options.IServerConfig;
import com.kwwsyk.endinv.common.util.SortType;

import java.util.UUID;

public final class ModInfo {

    public static final String MOD_ID = "endless_inventory";

    public static final UUID DEFAULT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    static boolean clientLoaded = false;

    private static IServerConfig serverConfig;

    public static SortType.ISortHelper sortHelper;

    private static IPacketDistributor packetDistributor;

    public static IPlatform platformContext;

    public static IServerConfig getServerConfig() {
        return serverConfig;
    }

    public static void setServerConfig(IServerConfig serverConfig) {
        if(ModInfo.serverConfig!=null) throw new IllegalStateException("Try to set config when config has been initialized.");
        ModInfo.serverConfig = serverConfig;
    }

    public static IPacketDistributor getPacketDistributor(){
        return packetDistributor;
    }

    public static void setPacketDistributor(IPacketDistributor packetDistributor){
        ModInfo.packetDistributor = packetDistributor;
    }

    public static boolean isClientLoaded(){
        return clientLoaded;
    }
}
