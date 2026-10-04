package com.kwwsyk.endinv.common.autopick.options;

import com.kwwsyk.endinv.common.options.config.ComplexConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;

public class DropsConfig extends ComplexConfigEntryImpl<Void> {
    public final BooleanEntry PROTECT_DROPS = new BooleanEntry("protect_drops",
            new String[]{"Give item drops temporary protection from fire, explosions, and other damage."}, true);
    public final IntEntry DIRECTED_DISTRIBUTE = new IntEntry("directed_distribute", new String[]{
            "Move drops towards the player instead of using their random initial velocity.",
            "0 disables it; a negative value teleports drops; a positive value controls velocity."
    }, 0);
    public final BooleanEntry SEND_TO_INVENTORY = new BooleanEntry("send_to_inventory", new String[]{
            "Immediately simulate player contact so drops first enter the normal inventory."
    }, false);
    public final BooleanEntry DIRECTLY_SEND_TO_ENDINV = new BooleanEntry("directly_send_to_endinv", new String[]{
            "Send generated drops directly to Endless Inventory. This is the legacy auto-pick mode."
    }, false);
    public final BooleanEntry ENDINV_AFTER_INVENTORY = new BooleanEntry("send_to_endinv_after_touch", new String[]{
            "For captured drops, send the remainder to Endless Inventory after normal inventory insertion."
    }, true);
    public final BooleanEntry PICK_TO_ENDINV = new BooleanEntry("pick_to_endinv", new String[]{
            "Send items encountered during normal ground-item pickup to Endless Inventory."
    }, false);

    public DropsConfig(String key) {
        super(key);
    }

    @Override
    public ConfigEntryImpl<?>[] fields() {
        return new ConfigEntryImpl[]{PROTECT_DROPS, DIRECTED_DISTRIBUTE, SEND_TO_INVENTORY,
                DIRECTLY_SEND_TO_ENDINV, ENDINV_AFTER_INVENTORY, PICK_TO_ENDINV};
    }
}
