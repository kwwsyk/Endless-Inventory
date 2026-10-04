package com.kwwsyk.endinv.common.autopick.options;

import com.kwwsyk.endinv.common.options.config.ComplexConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;

public class ExpDropsConfig extends ComplexConfigEntryImpl<Void> {
    public final BooleanEntry PROTECT_DROPS = new BooleanEntry("protect_drops",
            new String[]{"Give experience orbs temporary protection from damage."}, true);
    public final IntEntry DIRECTED_DISTRIBUTE = new IntEntry("directed_distribute", new String[]{
            "Move experience orbs towards the nearest player within 16 blocks.",
            "0 disables it; a negative value teleports orbs; a positive value controls velocity."
    }, -1);
    public final BooleanEntry TOUCH_DIRECTLY = new BooleanEntry("touch_directly",
            new String[]{"Immediately let the nearest player within 16 blocks touch the experience orb."}, false);
    public final BooleanEntry GIVE_DIRECTLY = new BooleanEntry("give_directly", new String[]{
            "Give generated experience directly to the responsible player without spawning an orb.",
            "This may conflict with mods that depend on experience-orb lifecycle events."
    }, false);

    public ExpDropsConfig(String key) {
        super(key);
    }

    @Override
    public ConfigEntryImpl<?>[] fields() {
        return new ConfigEntryImpl[]{PROTECT_DROPS, DIRECTED_DISTRIBUTE, TOUCH_DIRECTLY, GIVE_DIRECTLY};
    }
}
