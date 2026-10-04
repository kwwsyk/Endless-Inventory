package com.kwwsyk.endinv.common.autopick.options;

import com.kwwsyk.endinv.common.options.config.ComplexConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;

public class PickupHelperOptions extends ComplexConfigEntryImpl<Void> {
    public final DropsConfig ITEM_DROPS = new DropsConfig("item_drops_config");
    public final ExpDropsConfig EXP_DROPS = new ExpDropsConfig("exp_drops_config");

    public PickupHelperOptions() {
        super("pickup_helper", new String[]{"Options controlling dropped items and experience."}, null);
    }

    @Override
    public ConfigEntryImpl<?>[] fields() {
        return new ConfigEntryImpl[]{ITEM_DROPS, EXP_DROPS};
    }
}
