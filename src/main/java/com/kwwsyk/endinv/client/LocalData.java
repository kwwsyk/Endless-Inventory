package com.kwwsyk.endinv.client;

import com.kwwsyk.endinv.network.payloads.EndInvSettings;

public class LocalData {
    //todo: 暂缓之计，未来使用toml与本地config确定此值；Server值被此值同步
    public static EndInvSettings settings = EndInvSettings.DEFAULT;
}
