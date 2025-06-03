package com.kwwsyk.endinv.common.client;

import com.kwwsyk.endinv.common.client.option.IClientConfig;

public class ClientModInfo {

    private static IClientConfig clientConfig;

    public static IInputHandler inputHandler;

    public static IContainerScreenHelper containerScreenHelper;//todo

    public static IClientConfig getClientConfig() {
        return clientConfig;
    }

    public static void setClientConfig(IClientConfig clientConfig) {
        if(ClientModInfo.clientConfig!=null) throw new IllegalStateException("Try to set config when config has been initialized.");
        ClientModInfo.clientConfig = clientConfig;
    }
}
