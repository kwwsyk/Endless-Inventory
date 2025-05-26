package com.kwwsyk.endinv.common.options;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static net.minecraft.tags.ItemTags.*;

public class ItemClassify{
    private final Predicate<ItemStack> rule;
    public static final List<TagKey<Item>> WEAPON_TAGS = new ArrayList<>();
    public static final List<TagKey<Item>> TOOL_TAGS = new ArrayList<>();
    public static final List<TagKey<Item>> EQUIPPABLE_TAGS = new ArrayList<>();
    public static final List<Holder<ItemClassify>> DEFAULT_CLASSIFIES = new ArrayList<>();
    public static final Holder<ItemClassify> ALL = register("all",(stack)->true);
    public static final Holder<ItemClassify> BLOCKS = register("blocks",(stack)->stack.getItem() instanceof BlockItem);
    public static final Holder<ItemClassify> WEAPONS = register("weapons", ItemClassify::isWeapon);
    public static final Holder<ItemClassify> TOOLS = register("tools",ItemClassify::isTool);
    public static final Holder<ItemClassify> EQUIPMENTS = register("defence_equipments",ItemClassify::isDefenceEquipment);
    public static final Holder<ItemClassify> FOOD_POTION = register("food_and_potion",ItemClassify::isFoodOrPotion);
    public static final Holder<ItemClassify> ENCHANTMENT_BOOKS = register("enchantment_books",stack->stack.getItem() instanceof EnchantedBookItem);
    public static final Holder<ItemClassify> VANISHING_ENCHANTABLE = register("vanishing_enchantable",stack->stack.is(ItemTags.VANISHING_ENCHANTABLE));
    public static final Int2IntArrayMap INDEX2HIDING = new Int2IntArrayMap();

    public ItemClassify(Predicate<ItemStack> rule){
        this.rule = rule;
    }

    public boolean matches(ItemStack stack) {
        return rule.test(stack);
    }

    private static Holder<ItemClassify> register(String s, Predicate<ItemStack> rule){
        Holder<ItemClassify> ret = ModInitializer.CLASSIFIES.register(s,()->new ItemClassify(rule));
        DEFAULT_CLASSIFIES.add(ret);
        return ret;
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
        INDEX2HIDING.defaultReturnValue(0);
        INDEX2HIDING.put(8,1);
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
