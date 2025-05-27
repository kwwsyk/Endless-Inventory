package com.kwwsyk.endinv.common.util;

import com.kwwsyk.endinv.common.EndlessInventory;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public enum SortType {
    DEFAULT("sorttype.endinv.default"),
    COUNT("sorttype.endinv.count"),
    SPACE_AND_NAME("sorttype.endinv.name"),
    ID("sorttype.endinv.id"),
    LAST_MODIFIED("sorttype.endinv.last_modified");

    public final String translationKey;

    SortType(String translationKey){
        this.translationKey = translationKey;
    }

    public static final StreamCodec<ByteBuf,SortType> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SortType decode(ByteBuf byteBuf) {
            byte b = byteBuf.readByte();
            SortType[] types = SortType.values();
            if (b < 0 || b >= types.length) b = 0;
            return types[b];
        }

        @Override
        public void encode(ByteBuf o, SortType sortType) {
            o.writeByte(sortType.ordinal());
        }
    };
    public static final Codec<SortType> CODEC = Codec.STRING.xmap(
            name -> {
                try {
                    return SortType.valueOf(name.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return SortType.DEFAULT;
                }
            },
            SortType::name
    );

    public interface ISortHelper{

        default Comparator<ItemStack> getComparator(SortType sortType, EndlessInventory endInv){
            return switch (sortType){
                case DEFAULT -> null;
                case COUNT -> Comparator.comparingInt(ItemStack::getCount);
                case SPACE_AND_NAME -> sortById();
                case ID -> REGISTRY_ORDER_COMPARATOR;
                case LAST_MODIFIED -> Comparator.comparingLong(s -> endInv.getItemMap().get(ItemKey.asKey(s)).lastModTime());
            };
        }

        Comparator<ItemStack> sortById();

        Comparator<ItemStack> REGISTRY_ORDER_COMPARATOR = Comparator.comparingInt(
                stack -> BuiltInRegistries.ITEM.getId(stack.getItem())
        );
    }
}
