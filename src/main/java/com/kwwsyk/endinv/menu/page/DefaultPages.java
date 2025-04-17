package com.kwwsyk.endinv.menu.page;

import com.kwwsyk.endinv.options.ItemClassify;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import static com.kwwsyk.endinv.options.ItemClassify.*;

public final class DefaultPages {

    public static final Object2ObjectArrayMap<Holder<ItemClassify>, DisplayPage.PageConstructor> CLASSIFY2PAGE = new Object2ObjectArrayMap<>();
    public static final Object2ObjectArrayMap<Holder<ItemClassify>, ResourceLocation> CLASSIFY2RSRC = new Object2ObjectArrayMap<>();
    private static void linkVanillaItem(Holder<ItemClassify> classify, String s){
        CLASSIFY2RSRC.put(classify,ResourceLocation.withDefaultNamespace(s));
    }

    static {
        CLASSIFY2PAGE.defaultReturnValue(ItemDisplay::new);
        CLASSIFY2PAGE.put(ALL,ItemDisplay::new);
        CLASSIFY2PAGE.put(BLOCKS,ItemDisplay::new);
        CLASSIFY2PAGE.put(WEAPONS,ItemDisplay::new);
        CLASSIFY2PAGE.put(ENCHANTMENT_BOOKS,ItemEntryDisplay::new);

        CLASSIFY2RSRC.defaultReturnValue(null);
        linkVanillaItem(ALL,"chest");
        linkVanillaItem(BLOCKS,"stone");
        linkVanillaItem(WEAPONS,"iron_sword");
        linkVanillaItem(TOOLS,"iron_pickaxe");
        linkVanillaItem(EQUIPMENTS,"iron_chestplate");
        linkVanillaItem(FOOD_POTION,"bread");
        linkVanillaItem(ENCHANTMENT_BOOKS,"enchanted_book");
        linkVanillaItem(VANISHING_ENCHANTABLE,"diamond_helmet");

    }

}
