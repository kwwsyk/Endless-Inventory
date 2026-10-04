package com.kwwsyk.endinv.common.autopick.options;


import com.kwwsyk.endinv.common.options.config.ComplexConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;

/**Configuration on behaviors of experience orbs.<p>
 * {@link #PROTECT_DROPS} give invulnerability for 5 min for every orb.<br>
 * The 3 remain configurations can effect individually and some overrides other logically.<br>
 * <l>
 *     {@link #DIRECTED_DISTRIBUTE} gives orbs a velocity towards player.<br>
 *     {@link #TOUCH_DIRECTLY} let orbs be touched directly by player. And then the orb shall discard, prevent {@code distribute}<br>.
 *     {@link #GIVE_DIRECTLY} let player directly increase exp on killing mobs or mining, jumping the summoning of orbs.
 *          And all living orbs from other sources discard with increasing player's exp value. This entry can cause conflictions show as bugs
 *          with some mods whose features rely on exp orbs' creating or {@code playerTouch} process.
 * </l>
 * <l>
 *     For exp from loot:<br>
 *      {@code give} > {@code touch} > {@code distribute}<p>
 *     For living exp:<br>
 *      {@code touch} > {@code give} > {@code distribute}</p>
 * </l>
 *
 * @since 26/3/2 - in dev
 * @version dev
 */
public class ExpDropsConfig extends ComplexConfigEntryImpl<ExpDropsConfig.Param> {

    public ExpDropsConfig(String key) {
        super(key);
    }

    public record Param(
            boolean protectDrops,
            int directedDistribute,
            boolean touchDirectly, boolean giveDirectly
    ){
        public static final Param DEFAULT = new Param(true,-1, false, false);
    }

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

    @Override
    public ConfigEntryImpl<?>[] fields() {
        return new ConfigEntryImpl[]{PROTECT_DROPS, DIRECTED_DISTRIBUTE, TOUCH_DIRECTLY, GIVE_DIRECTLY};
    }
}
