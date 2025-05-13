package com.kwwsyk.endinv.client.gui.bg;

import com.kwwsyk.endinv.client.gui.EndlessInventoryScreen;
import com.kwwsyk.endinv.client.gui.ScreenFrameWork;

public abstract class Transparent extends ScreenBgRendererImpl {

    public Transparent(ScreenFrameWork frameWork) {
        super(frameWork);
    }

    public Transparent(EndlessInventoryScreen screen) {
        super(screen);
    }
}
