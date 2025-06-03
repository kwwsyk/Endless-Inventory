package com.kwwsyk.endinv.common.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public interface IInputHandler {

    boolean isActiveAndMatches(KeyMapping keyMapping, InputConstants.Key key);

    boolean isActiveAndMatches(KeyMappings.EndInvKey endInvKey, InputConstants.Key key);
}
