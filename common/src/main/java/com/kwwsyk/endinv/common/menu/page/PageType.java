package com.kwwsyk.endinv.common.menu.page;

import com.kwwsyk.endinv.common.client.gui.ScreenFramework;
import com.kwwsyk.endinv.common.client.gui.page.*;
import com.kwwsyk.endinv.common.util.ItemKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static net.minecraft.tags.ItemTags.*;

public class PageType {

    public static final String DEFAULT_KEY = "all_items";

    private static final List<Predicate<ItemKey>> equipmentSubclassifications = List.of(
            ofEquipmentSlotType(EquipmentSlot.HEAD),
            ofEquipmentSlotType(EquipmentSlot.CHEST),
            ofEquipmentSlotType(EquipmentSlot.LEGS),
            ofEquipmentSlotType(EquipmentSlot.FEET),
            ofEquipmentSlotType(EquipmentSlot.MAINHAND),
            ofEquipmentSlotType(EquipmentSlot.OFFHAND)
    );

    private static Predicate<ItemKey> ofEquipmentSlotType(EquipmentSlot slot){
        return it-> {
            var comp = it.toStack(1).getComponents().get(DataComponents.EQUIPPABLE);
            return comp!=null && comp.slot() == slot;
        };
    }

    public static final List<TagKey<Item>> WEAPON_TAGS = new ArrayList<>();
    public static final List<TagKey<Item>> TOOL_TAGS = new ArrayList<>();
    public static final List<TagKey<Item>> EQUIPPABLE_TAGS = new ArrayList<>();

    public static final PageType ALL_ITEMS = createClassifiedPage(DEFAULT_KEY,null,"chest");
    public static final PageType BLOCK_ITEMS = createClassifiedPage("block_items",(stack)->stack.getItem() instanceof BlockItem,"stone");
    public static final PageType WEAPONS = createClassifiedPage("weapons",PageType::isWeapon,"iron_sword");
    public static final PageType TOOLS = createClassifiedPage("tools",PageType::isTool,"iron_pickaxe");
    public static final PageType EQUIPMENTS = new PageType(
            (type,manager)->new SegClassifyItemDisplay(type, manager,equipmentSubclassifications,false,true),
            "equipments",PageType::isDefenceEquipment,Identifier.withDefaultNamespace("iron_chestplate")
    );
    public static final PageType CONSUMABLE = createClassifiedPage("consumable",PageType::isFoodOrPotion,"bread");
    //updatable entry: Let items who have::: .components().has(DataComponents.STORED_ENCHANTMENTS
    public static final PageType ENCHANTED_BOOKS = new PageType(
            (t, f) -> new ItemEntryDisplay(
                    t, f, ItemEntryDisplay.DescriptionProvider::fromEnch
            ),
            "enchanted_books",
            stack -> stack.getItem() == Items.ENCHANTED_BOOK,
            Identifier.withDefaultNamespace("enchanted_book")
    );
    public static final PageType BOOKMARK = new PageType(StarredItemPage::new,"bookmark",null,Identifier.withDefaultNamespace("book"));

    private final PageConstructor constructor;
    @Nullable
    public final Predicate<ItemStack> itemClassify;
    @Nullable
    public Identifier icon = null;
    public final String registerName;

    @FunctionalInterface
    public interface PageConstructor {
        /**
         * DisplayPage's constructor or it's variation.
         * Called by {@link ScreenFramework}'s constructor
         *
         * @since 1.1.0
         *
         * @param pageType registered page type
         * @param manager since 1.1.0, it's always ScreenFramework/
         */
        DisplayPage create(PageType pageType, ScreenFramework manager);
    }

    public PageType(PageConstructor constructor, String registerName){
        this.constructor = constructor;
        this.registerName = registerName;
        this.itemClassify = null;
    }

    public PageType(PageConstructor constructor, String registerName,@Nullable Predicate<ItemStack> itemClassify){
        this.constructor = constructor;
        this.itemClassify = itemClassify;
        this.registerName = registerName;
    }

    public PageType(PageConstructor constructor, String registerName,@Nullable Predicate<ItemStack> itemClassify,@Nullable Identifier icon){
        this.constructor = constructor;
        this.itemClassify = itemClassify;
        this.icon = icon;
        this.registerName = registerName;
    }

    public static PageType createClassifiedPage(String registerName,@Nullable Predicate<ItemStack> itemClassify, String icon){
        return new PageType(ItemDisplay::new,registerName,itemClassify,Identifier.withDefaultNamespace(icon));
    }

    /**
     * Build new DisplayPage.<p>
     * Called by ScreenFramework's constructor on client.
     */
    public DisplayPage buildPage(ScreenFramework meta){
        var page =  constructor.create(this, meta);
        if(icon!=null) page.icon = icon;
        return page;
    }

    public String toString(){
        return this.registerName;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PageType pageType
                && Objects.equals(pageType.constructor,constructor)
                && Objects.equals(pageType.itemClassify, itemClassify)
                && Objects.equals(pageType.registerName, registerName);
    }

    private static boolean isWeapon(ItemStack itemStack){
        Item item = itemStack.getItem();
        return
                itemStack.is(AXES) ||
                item instanceof  TridentItem ||
                item instanceof ProjectileWeaponItem ||
                WEAPON_TAGS.stream().anyMatch(itemStack::is);
    }

    private static boolean isTool(ItemStack itemStack){
        Item item = itemStack.getItem();
        return
                itemStack.is(AXES) ||
                item instanceof ShearsItem ||
                itemStack.is(SHOVELS) ||
                item instanceof FlintAndSteelItem ||
                item instanceof FishingRodItem ||
                TOOL_TAGS.stream().anyMatch(itemStack::is);
    }

    private static boolean isDefenceEquipment(ItemStack itemStack){
        Item item = itemStack.getItem();
        return
                itemStack.has(DataComponents.EQUIPPABLE) ||item instanceof ShieldItem ||
                item == Items.ELYTRA ||
                EQUIPPABLE_TAGS.stream().anyMatch(itemStack::is);
    }

    private static boolean isFoodOrPotion(ItemStack itemStack){
        Item item = itemStack.getItem();
        return item instanceof PotionItem || itemStack.has(DataComponents.FOOD);
    }

    static {
        WEAPON_TAGS.add(SWORDS);
        WEAPON_TAGS.add(AXES);
        TOOL_TAGS.add(AXES);
        TOOL_TAGS.add(PICKAXES);
        TOOL_TAGS.add(HOES);
        TOOL_TAGS.add(SHOVELS);
    }
}
