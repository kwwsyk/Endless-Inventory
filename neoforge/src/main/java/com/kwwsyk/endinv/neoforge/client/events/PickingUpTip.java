package com.kwwsyk.endinv.neoforge.client.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.client.event.AutoPickTipper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT,modid = ModInfo.MOD_ID)
public class PickingUpTip {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        AutoPickTipper.onRenderGui(event.getGuiGraphics());
    }
}
