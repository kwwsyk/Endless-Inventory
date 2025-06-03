package com.kwwsyk.endinv.common.options;

import com.kwwsyk.endinv.common.util.Accessibility;

public interface IServerConfig {

    IConfigValue<Integer> getMaxAllowedStackSize();

    IConfigValue<Boolean> allowInfinityMode();

    IConfigValue<Boolean> enableAutoPick();

    IConfigValue<ContentTransferMode> transferMode();

    IConfigValue<Accessibility> defaultAccessibility();

    IConfigValue<MissingEndInvPolicy> policyHandlingMissing();
}
