package com.kwwsyk.endinv.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class SearchUtil {
    public static boolean matchesSearch(ItemStack stack, String search) {
        if (search == null || search.isEmpty()) return true;

        String[] terms = search.toLowerCase(Locale.ROOT).trim().split("\\s+");

        for (String term : terms) {
            if (!matchesSingleTerm(stack, term)) {
                return false; // AND 逻辑：任意一个不匹配即失败
            }
        }

        return true;
    }

    private static boolean matchesSingleTerm(ItemStack stack, String term) {
        if (term.startsWith("#")) {
            return matchesTag(stack, term.substring(1));
        } else if (term.startsWith("*")) {
            return matchesId(stack, term.substring(1));
        } else if (term.startsWith("^")) {
            return matchesTooltip(stack, term.substring(1));
        } else if (term.startsWith("@")) {
            return matchesNamespace(stack, term.substring(1));
        } else {
            return matchesName(stack, term); // 无前缀：只匹配 HoverName 和 ID，不匹配 Tooltip
        }
    }

    private static boolean matchesTag(ItemStack stack, String tagId) {
        return stack.getTags().anyMatch(tag ->
                tag.location().toString().toLowerCase(Locale.ROOT).contains(tagId)
        );
    }

    private static boolean matchesId(ItemStack stack, String idSearch) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString().toLowerCase(Locale.ROOT).contains(idSearch);
    }

    private static boolean matchesTooltip(ItemStack stack, String tooltipSearch) {
        List<Component> tooltip = stack.getTooltipLines(
                Item.TooltipContext.of(Minecraft.getInstance().level),
                null,
                TooltipFlag.Default.NORMAL
        );
        for (Component line : tooltip) {
            if (line.getString().toLowerCase(Locale.ROOT).contains(tooltipSearch)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesName(ItemStack stack, String nameSearch) {
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(nameSearch)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().toLowerCase(Locale.ROOT).contains(nameSearch);
    }

    private static boolean matchesNamespace(ItemStack stack, String nsSearch) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().toLowerCase(Locale.ROOT).contains(nsSearch);
    }
}
