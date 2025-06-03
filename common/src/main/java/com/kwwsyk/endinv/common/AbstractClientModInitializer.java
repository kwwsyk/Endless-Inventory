package com.kwwsyk.endinv.common;

import com.kwwsyk.endinv.common.client.ClientModInfo;
import com.kwwsyk.endinv.common.client.IContainerScreenHelper;
import com.kwwsyk.endinv.common.client.IInputHandler;
import com.kwwsyk.endinv.common.client.option.IClientConfig;

public abstract class AbstractClientModInitializer {

    protected AbstractClientModInitializer(){
        ClientModInfo.setClientConfig(loadClientConfig());
        ClientModInfo.inputHandler = getInputHandler();
        ClientModInfo.containerScreenHelper = getScreenHelper();
    }

    protected abstract IClientConfig loadClientConfig();

    protected abstract IInputHandler getInputHandler();

    protected abstract IContainerScreenHelper getScreenHelper();

}
