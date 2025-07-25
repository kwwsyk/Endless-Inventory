package com.kwwsyk.endinv.forge.nbtAttcachment;

import com.kwwsyk.endinv.common.AbstractModInitializer;
import com.kwwsyk.endinv.common.ModInfo;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttachingCapabilities {

    public static Capability<EndInvUuid> END_INV_UUID = null;

    @SubscribeEvent
    public static void reg(RegisterCapabilitiesEvent event){
        event.register(IEndInvUuid.class);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event){
        if (!(event.getObject() instanceof Player)) return;

        EndInvUuid backend = new EndInvUuid();
        LazyOptional<IEndInvUuid> optional = LazyOptional.of(() -> backend);

        event.addCapability(AbstractModInitializer.withModLocation("uuid"), new ICapabilitySerializable<CompoundTag>() {
            @Override
            public CompoundTag serializeNBT() {
                return backend.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag compoundTag) {
                backend.deserializeNBT(compoundTag);
            }

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction direction) {
                return capability==END_INV_UUID ? optional.cast() : LazyOptional.empty();
            }
        });
    }


}
