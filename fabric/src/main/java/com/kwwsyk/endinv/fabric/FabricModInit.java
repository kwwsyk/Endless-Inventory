package com.kwwsyk.endinv.fabric;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import net.fabricmc.api.ModInitializer;

import java.util.function.Function;
import java.util.function.Supplier;

public class FabricModInit extends AbstractModInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
    }

    @Override
    protected <T> Supplier<T> register(String id, Supplier<T> supplier) {
        return null;
    }

    @Override
    protected <I extends net.minecraft.world.item.Item> Supplier<I> registerItem(String id, Function<net.minecraft.world.item.Item.Properties, net.minecraft.world.item.Item> constructor) {
        return null;
    }
}
