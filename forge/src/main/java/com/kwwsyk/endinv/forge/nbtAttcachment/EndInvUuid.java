package com.kwwsyk.endinv.forge.nbtAttcachment;

import com.kwwsyk.endinv.common.ModInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public class EndInvUuid implements IEndInvUuid, INBTSerializable<CompoundTag> {
    private UUID uuid = ModInfo.DEFAULT_UUID;

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("EndInvUuid", uuid);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.hasUUID("EndInvUuid")) {
            uuid = tag.getUUID("EndInvUuid");
        }
    }
}
