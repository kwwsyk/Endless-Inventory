package com.kwwsyk.endinv.common.menu.page;

import com.kwwsyk.endinv.common.menu.page.pageManager.PageMetaDataManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static net.minecraft.tags.ItemTags.*;

public class PageType {

    public static final String DEFAULT_KEY = "all_items";

    public static final List<TagKey<Item>> WEAPON_TAGS = new ArrayList<>();
    public static final List<TagKey<Item>> TOOL_TAGS = new ArrayList<>();
    public static final List<TagKey<Item>> EQUIPPABLE_TAGS = new ArrayList<>();

    public static final PageType ALL_ITEMS = createClassifiedPage(DEFAULT_KEY,null,"chest");
    public static final PageType BLOCK_ITEMS = createClassifiedPage("block_items",(stack)->stack.getItem() instanceof BlockItem,"stone");
    public static final PageType WEAPONS = createClassifiedPage("weapons",PageType::isWeapon,"iron_sword");
    public static final PageType TOOLS = createClassifiedPage("tools",PageType::isTool,"iron_pickaxe");
    public static final PageType EQUIPMENTS = createClassifiedPage("equipments",PageType::isDefenceEquipment,"iron_chestplate");
    public static final PageType CONSUMABLE = createClassifiedPage("consumable",PageType::isFoodOrPotion,"bread");
    public static final PageType ENCHANTED_BOOKS = createItemEntry("enchanted_books",stack->stack.getItem() instanceof EnchantedBookItem,"enchanted_book");
    public static final PageType VANISHING = createClassifiedPage("vanishing_enchantable",stack->stack.is(VANISHING_ENCHANTABLE),"diamond_helmet");
    public static final PageType BOOKMARK = new PageType(StarredItemPage::new,"bookmark",null,ResourceLocation.withDefaultNamespace("book"));

    private final PageConstructor constructor;
    @Nullable
    public final Predicate<ItemStack> itemClassify;
    public ResourceLocation icon = null;
    public final String registerName;

    @FunctionalInterface
    public interface PageConstructor {
        DisplayPage create(PageType pageType, PageMetaDataManager manager);
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

    public PageType(PageConstructor constructor, String registerName,@Nullable Predicate<ItemStack> itemClassify,@Nullable ResourceLocation icon){
        this.constructor = constructor;
        this.itemClassify = itemClassify;
        this.icon = icon;
        this.registerName = registerName;
    }

    public static PageType createClassifiedPage(String registerName, Predicate<ItemStack> itemClassify, String icon){
        return new PageType(ItemDisplay::new,registerName,itemClassify,ResourceLocation.withDefaultNamespace(icon));
    }

    public static PageType createItemEntry(String registerName, Predicate<ItemStack> itemClassify, String icon){
        return new PageType(ItemEntryDisplay::new,registerName,itemClassify, ResourceLocation.withDefaultNamespace(icon));
    }

    public DisplayPage buildPage(PageMetaDataManager meta){
        var page =  constructor.create(this, meta);
        if(icon!=null) page.icon = icon;
        return page;
    }

    public String toString(){
        try {
            return Holder.direct(this).getRegisteredName();
        } catch (Exception e) {
            return super.toString();
        }
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
        return item instanceof SwordItem ||
                item instanceof  AxeItem ||
                item instanceof  TridentItem ||
                item instanceof  MaceItem ||
                item instanceof ProjectileWeaponItem ||
                WEAPON_TAGS.stream().anyMatch(itemStack::is);
    }

    private static boolean isTool(ItemStack itemStack){
        Item item = itemStack.getItem();
        return item instanceof PickaxeItem ||
                item instanceof AxeItem ||
                item instanceof ShearsItem ||
                item instanceof ShovelItem ||
                item instanceof FlintAndSteelItem ||
                item instanceof FishingRodItem ||
                TOOL_TAGS.stream().anyMatch(itemStack::is);
    }

    private static boolean isDefenceEquipment(ItemStack itemStack){
        Item item = itemStack.getItem();
        return item instanceof ArmorItem ||
                item instanceof ShieldItem ||
                item instanceof ElytraItem ||
                EQUIPPABLE_TAGS.stream().anyMatch(itemStack::is);
    }

    private static boolean isFoodOrPotion(ItemStack itemStack){
        Item item = itemStack.getItem();
        return item instanceof PotionItem ||
                itemStack.get(DataComponents.FOOD) != null;
    }

    static {
        WEAPON_TAGS.add(SWORDS);
        WEAPON_TAGS.add(AXES);
        WEAPON_TAGS.add(BOW_ENCHANTABLE);
        WEAPON_TAGS.add(CROSSBOW_ENCHANTABLE);
        TOOL_TAGS.add(AXES);
        TOOL_TAGS.add(PICKAXES);
        TOOL_TAGS.add(HOES);
        TOOL_TAGS.add(SHOVELS);
        TOOL_TAGS.add(MINING_ENCHANTABLE);
        EQUIPPABLE_TAGS.add(EQUIPPABLE_ENCHANTABLE);
    }
}
