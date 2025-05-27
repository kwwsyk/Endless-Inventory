package com.kwwsyk.endinv.neoforge.events;

import com.kwwsyk.endinv.common.ModInfo;
import com.kwwsyk.endinv.common.data.EndlessInventoryData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = ModInfo.MOD_ID)
public class LevelEvents {

    @SubscribeEvent
    public static void load(LevelEvent.Load event){
        PlayerEvents.tickRefresh = true;
        if(event.getLevel() instanceof ServerLevel serverLevel){
            EndlessInventoryData.init(serverLevel);
        }

    }
}
